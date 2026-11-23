package com.karasu256.client.capture;

import com.karasu256.ChatNative;
import com.karasu256.capture.CapturePipeline;
import com.karasu256.mcchat.common.CapturedData;
import com.karasu256.mcchat.common.CapturedData.Kind;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldData;
import com.karasu256.capture.PlayerRefs;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class ClientCapture {
  private static volatile Consumer<CapturedPacket> upstream;
  private static boolean typing;

  private ClientCapture() {}

  public static void register() {
    ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTime) ->
        submit(
            Kind.CHAT_RECEIVED,
            sender == null ? null : PlayerRefs.of(sender),
            signed != null ? signed.signedContent() : message.getString()));

    ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
      if (!overlay) {
        submit(Kind.CHAT_RECEIVED, null, message.getString());
      }
    });

    ClientSendMessageEvents.CHAT.register(
        message -> submit(Kind.CHAT_SENT, localPlayer(), message));

    ClientSendMessageEvents.COMMAND.register(
        command -> submit(Kind.COMMAND_SENT, localPlayer(), command));
  }

  public static void setUpstream(Consumer<CapturedPacket> sender) {
    upstream = sender;
  }

  public static synchronized void typing(boolean started) {
    if (!started && !typing) {
      return;
    }
    typing = started;
    ChatNative.pipeline().publish(typingData(started));
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
              Optional.of(typingData(started))));
    }
  }

  private static CapturedData typingData(boolean started) {
    return CapturedData.of(
        Origin.CLIENT, started ? Kind.TYPING_STARTED : Kind.TYPING_STOPPED, localPlayer(), null);
  }

  public static void playerJoined(GameProfile profile) {
    ClientSkins.request(profile);
    submit(Kind.PLAYER_JOINED, PlayerRefs.of(profile), null);
  }

  public static void playerLeft(PlayerInfo info) {
    if (info != null) {
      submit(Kind.PLAYER_LEFT, PlayerRefs.of(info.getProfile()), null);
    }
  }

  public static void submitPlayerList(Minecraft client) {
    if (client.getConnection() == null) {
      return;
    }
    client.getConnection().getOnlinePlayers().forEach(info -> ClientSkins.request(info.getProfile()));
    List<PlayerRef> players =
        client.getConnection().getOnlinePlayers().stream()
            .map(info -> PlayerRefs.of(info.getProfile()))
            .toList();
    ChatNative.pipeline().submit(CapturedData.playerList(Origin.CLIENT, players));
  }

  public static PlayerRef sessionPlayer() {
    User user = Minecraft.getInstance().getUser();
    return new PlayerRef(user.getProfileId(), user.getName(), Optional.empty());
  }

  private static PlayerRef localPlayer() {
    Minecraft client = Minecraft.getInstance();
    return client.player == null ? null : PlayerRefs.of(client.player.getGameProfile());
  }

  private static void submit(Kind kind, PlayerRef player, String content) {
    CapturePipeline pipeline = ChatNative.pipeline();
    pipeline.submit(CapturedData.of(Origin.CLIENT, kind, player, content));
  }
}
