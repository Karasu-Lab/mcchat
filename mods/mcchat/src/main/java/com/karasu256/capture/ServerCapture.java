package com.karasu256.capture;

import com.karasu256.ChatNative;
import com.karasu256.capture.CapturedData.Kind;
import java.util.List;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerCapture {
  private ServerCapture() {}

  public static void register() {
    ServerMessageEvents.CHAT_MESSAGE.register(
        (message, sender, params) ->
            ChatNative.pipelineFor(sender.level().getServer())
                .submit(
                    CapturedData.of(
                        Origin.SERVER,
                        Kind.CHAT_RECEIVED,
                        PlayerRefs.of(sender.getGameProfile()),
                        message.signedContent())));

    ServerMessageEvents.COMMAND_MESSAGE.register(
        (message, source, params) ->
            ChatNative.pipelineFor(source.getServer())
                .submit(
                    CapturedData.of(
                        Origin.SERVER,
                        Kind.CHAT_RECEIVED,
                        source.getPlayer() == null
                            ? null
                            : PlayerRefs.of(source.getPlayer().getGameProfile()),
                        message.signedContent())));

    ServerMessageEvents.GAME_MESSAGE.register(
        (server, message, overlay) -> {
          if (!overlay) {
            ChatNative.pipelineFor(server)
                .submit(CapturedData.of(Origin.SERVER, Kind.CHAT_SENT, null, message.getString()));
          }
        });

    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          CapturePipeline pipeline = ChatNative.pipelineFor(server);
          pipeline.submit(CapturedData.playerList(Origin.SERVER, onlinePlayers(server)));
          pipeline.submit(
              CapturedData.of(
                  Origin.SERVER,
                  Kind.PLAYER_JOINED,
                  PlayerRefs.of(handler.getPlayer().getGameProfile()),
                  null));
        });

    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) ->
            ChatNative.pipelineFor(server)
                .submit(
                    CapturedData.of(
                        Origin.SERVER,
                        Kind.PLAYER_LEFT,
                        PlayerRefs.of(handler.getPlayer().getGameProfile()),
                        null)));
  }

  public static void commandExecuted(ServerPlayer player, String command) {
    MinecraftServer server = player.level().getServer();
    if (!server.isDedicatedServer()) {
      return;
    }
    ChatNative.pipelineFor(server)
        .submit(
            CapturedData.of(
                Origin.SERVER,
                Kind.COMMAND_EXECUTED,
                PlayerRefs.of(player.getGameProfile()),
                command));
  }

  public static List<PlayerRef> onlinePlayers(MinecraftServer server) {
    return server.getPlayerList().getPlayers().stream()
        .map(p -> PlayerRefs.of(p.getGameProfile()))
        .toList();
  }
}
