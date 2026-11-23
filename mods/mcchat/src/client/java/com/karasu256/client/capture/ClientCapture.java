package com.karasu256.client.capture;

import com.karasu256.ChatNative;
import com.karasu256.capture.PlayerRefs;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.MinecraftScreen;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldData;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.ChatReceivedEvent;
import com.karasu256.mcchat.common.event.ChatSentEvent;
import com.karasu256.mcchat.common.event.CommandSentEvent;
import com.karasu256.mcchat.common.event.PlayerJoinedEvent;
import com.karasu256.mcchat.common.event.PlayerLeftEvent;
import com.karasu256.mcchat.common.event.PlayerListEvent;
import com.karasu256.mcchat.common.event.TypingStartedEvent;
import com.karasu256.mcchat.common.event.TypingStoppedEvent;
import com.karasu256.net.ChatTextPayload;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class ClientCapture {
  private static volatile Consumer<CapturedPacket> upstream;
  private static boolean typing;
  private static volatile ChatText outgoing;

  private ClientCapture() {}

  public static void register() {
    ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTime) ->
        new ChatReceivedEvent(
                ChatNative.pipeline(),
                Origin.CLIENT,
                sender == null ? null : PlayerRefs.of(sender),
                ChatText.parse(
                    signed != null ? signed.signedContent() : message.getString(),
                    onlinePlayers()))
            .handle());

    ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
      if (!overlay) {
        new ChatReceivedEvent(
                ChatNative.pipeline(),
                Origin.CLIENT,
                null,
                ChatText.parse(message.getString(), onlinePlayers()))
            .handle();
      }
    });

    ClientSendMessageEvents.CHAT.register(message -> {
      ChatText text = outgoingText(message);
      if (ClientPlayNetworking.canSend(ChatTextPayload.TYPE) && hasData(text)) {
        ClientPlayNetworking.send(new ChatTextPayload(text));
      }
      new ChatSentEvent(ChatNative.pipeline(), Origin.CLIENT, localPlayer(), text).handle();
    });

    ClientSendMessageEvents.COMMAND.register(command ->
        new CommandSentEvent(ChatNative.pipeline(), Origin.CLIENT, localPlayer(), command)
            .handle());
  }

  public static void screenChanged(MinecraftScreen screen) {
    ChatNative.pipeline().setScreen(screen);
  }

  public static void setUpstream(Consumer<CapturedPacket> sender) {
    upstream = sender;
  }

  public static synchronized void typing(boolean started) {
    if (!started && !typing) {
      return;
    }
    typing = started;
    ChatNative.pipeline().publish(typingEvent(started));
    relayTyping(started);
  }

  public static void relayTyping(boolean started) {
    Consumer<CapturedPacket> sender = upstream;
    if (sender != null) {
      sender.accept(
          new CapturedPacket(
              CapturedPacket.Type.EVENT,
              Origin.CLIENT,
              WorldData.empty("", ""),
              Optional.of(typingEvent(started))));
    }
  }

  public static void sendChat(ChatText text) {
    outgoing = text;
    String wire = ClientPlayNetworking.canSend(ChatTextPayload.TYPE) ? text.plain() : text.stripped().plain();
    sendToServer(wire, connection -> connection.sendChat(wire));
  }

  private static ChatText outgoingText(String message) {
    ChatText text = outgoing;
    outgoing = null;
    return text != null && text.plain().equals(message)
        ? text
        : ChatText.parse(message, onlinePlayers());
  }

  private static boolean hasData(ChatText text) {
    return !text.mentioned().isEmpty() || text.replyTarget().isPresent();
  }

  public static void sendCommand(String text) {
    sendToServer(
        text,
        connection -> connection.sendCommand(text.startsWith("/") ? text.substring(1) : text));
  }

  private static void sendToServer(String text, Consumer<ClientPacketListener> action) {
    Minecraft client = Minecraft.getInstance();
    if (text.isBlank() || client.player == null || client.getConnection() == null) {
      return;
    }
    client.execute(() -> {
      ClientPacketListener connection = client.getConnection();
      if (connection != null) {
        action.accept(connection);
      }
    });
  }

  private static CapturedEvent typingEvent(boolean started) {
    return started
        ? new TypingStartedEvent(ChatNative.pipeline(), Origin.CLIENT, localPlayer())
        : new TypingStoppedEvent(ChatNative.pipeline(), Origin.CLIENT, localPlayer());
  }

  public static void playerJoined(GameProfile profile) {
    ClientSkins.request(profile);
    new PlayerJoinedEvent(ChatNative.pipeline(), Origin.CLIENT, PlayerRefs.of(profile)).handle();
  }

  public static void playerLeft(PlayerInfo info) {
    if (info != null) {
      new PlayerLeftEvent(ChatNative.pipeline(), Origin.CLIENT, PlayerRefs.of(info.getProfile()))
          .handle();
    }
  }

  public static void submitPlayerList(Minecraft client) {
    if (client.getConnection() == null) {
      return;
    }
    client
        .getConnection()
        .getOnlinePlayers()
        .forEach(info -> ClientSkins.request(info.getProfile()));
    new PlayerListEvent(ChatNative.pipeline(), Origin.CLIENT, onlinePlayers()).handle();
  }

  public static List<PlayerRef> onlinePlayers() {
    ClientPacketListener connection = Minecraft.getInstance().getConnection();
    return connection == null
        ? List.of()
        : connection.getOnlinePlayers().stream()
            .map(info -> PlayerRefs.of(info.getProfile()))
            .toList();
  }

  public static PlayerRef sessionPlayer() {
    User user = Minecraft.getInstance().getUser();
    return new PlayerRef(user.getProfileId(), user.getName(), Optional.empty());
  }

  public static PlayerRef localPlayer() {
    Minecraft client = Minecraft.getInstance();
    return client.player == null ? null : PlayerRefs.of(client.player.getGameProfile());
  }
}
