package com.karasu256.capture;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.component.ChatText;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerChatTextProcessor {
  private static final Map<UUID, ChatText> HELD_CHAT_TEXTS = new ConcurrentHashMap<>();

  private ServerChatTextProcessor() {}

  public static void hold(ServerPlayer player, ChatText chatText) {
    if (chatText != null && chatText.valid()) {
      HELD_CHAT_TEXTS.put(player.getUUID(), chatText);
    }
  }

  public static ChatText received(ServerPlayer sender, String rawText, MinecraftServer server) {
    ChatText held = HELD_CHAT_TEXTS.remove(sender.getUUID());
    ChatText chatText = held != null && held.plain().equals(rawText) ? held : parse(rawText, server);
    return chatText.withoutReplyUnless(ChatNative.pipelineFor(server)::hasMessage);
  }

  public static ChatText parse(String rawText, MinecraftServer server) {
    return ChatText.parse(rawText, ServerCapture.onlinePlayers(server));
  }
}
