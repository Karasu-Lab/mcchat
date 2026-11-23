package com.karasu256.capture;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.event.ChatReceivedEvent;
import com.karasu256.mcchat.common.event.ChatSentEvent;
import com.karasu256.mcchat.common.event.CommandExecutedEvent;
import com.karasu256.mcchat.common.event.PlayerJoinedEvent;
import com.karasu256.mcchat.common.event.PlayerLeftEvent;
import com.karasu256.mcchat.common.event.PlayerListEvent;
import java.util.List;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerCapture {
  private ServerCapture() {}

  public static void register() {
    ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
      MinecraftServer server = sender.level().getServer();
      new ChatReceivedEvent(
              ChatNative.pipelineFor(server),
              Origin.SERVER,
              PlayerRefs.of(sender.getGameProfile()),
              ServerChatTextProcessor.received(sender, message.signedContent(), server))
          .handle();
    });

    ServerMessageEvents.COMMAND_MESSAGE.register((message, source, params) -> {
      CapturePipeline pipeline = ChatNative.pipelineFor(source.getServer());
      ServerPlayer player = source.getPlayer();
      new ChatReceivedEvent(
              pipeline,
              Origin.SERVER,
              player == null ? null : PlayerRefs.of(player.getGameProfile()),
              ServerChatTextProcessor.parse(message.signedContent(), source.getServer()))
          .handle();
    });

    ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
      if (!overlay) {
        new ChatSentEvent(ChatNative.pipelineFor(server), Origin.SERVER, null, message.getString())
            .handle();
      }
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      CapturePipeline pipeline = ChatNative.pipelineFor(server);
      new PlayerListEvent(pipeline, Origin.SERVER, onlinePlayers(server)).handle();
      new PlayerJoinedEvent(
              pipeline, Origin.SERVER, PlayerRefs.of(handler.getPlayer().getGameProfile()))
          .handle();
    });

    ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
        new PlayerLeftEvent(
                ChatNative.pipelineFor(server),
                Origin.SERVER,
                PlayerRefs.of(handler.getPlayer().getGameProfile()))
            .handle());
  }

  public static void commandExecuted(ServerPlayer player, String command) {
    MinecraftServer server = player.level().getServer();
    new CommandExecutedEvent(
            ChatNative.pipelineFor(server),
            Origin.SERVER,
            PlayerRefs.of(player.getGameProfile()),
            command)
        .handle();
  }

  public static List<PlayerRef> onlinePlayers(MinecraftServer server) {
    return server.getPlayerList().getPlayers().stream()
        .map(p -> PlayerRefs.of(p.getGameProfile()))
        .toList();
  }
}
