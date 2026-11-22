package com.karasu256.client.capture;

import com.karasu256.ChatNative;
import com.karasu256.capture.CapturePipeline;
import com.karasu256.capture.CapturedData;
import com.karasu256.capture.CapturedData.Kind;
import com.karasu256.capture.Origin;
import com.karasu256.capture.PlayerRef;
import com.karasu256.capture.PlayerRefs;
import com.mojang.authlib.GameProfile;
import java.util.List;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class ClientCapture {
  private ClientCapture() {}

  public static void register() {
    ClientReceiveMessageEvents.CHAT.register(
        (message, signed, sender, params, receptionTime) ->
            submit(
                Kind.CHAT_RECEIVED,
                sender == null ? null : PlayerRefs.of(sender),
                signed != null ? signed.signedContent() : message.getString()));

    ClientReceiveMessageEvents.GAME.register(
        (message, overlay) -> {
          if (!overlay) {
            submit(Kind.CHAT_RECEIVED, null, message.getString());
          }
        });

    ClientSendMessageEvents.CHAT.register(
        message -> submit(Kind.CHAT_SENT, localPlayer(), message));

    ClientSendMessageEvents.COMMAND.register(
        command -> submit(Kind.COMMAND_SENT, localPlayer(), command));
  }

  public static void playerJoined(GameProfile profile) {
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
    List<PlayerRef> players =
        client.getConnection().getOnlinePlayers().stream()
            .map(info -> PlayerRefs.of(info.getProfile()))
            .toList();
    ChatNative.pipeline().submit(CapturedData.playerList(Origin.CLIENT, players));
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
