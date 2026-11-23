package com.karasu256.client.chat;

import com.karasu256.capture.PlayerRefs;
import com.karasu256.client.capture.ClientCapture;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.net.ChatTextPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class ClientChatTextProcessor {
  private static volatile ChatText outgoingChatText;

  private ClientChatTextProcessor() {}

  public static List<PlayerRef> onlinePlayers() {
    ClientPacketListener connection = Minecraft.getInstance().getConnection();
    return connection == null
        ? List.of()
        : connection.getOnlinePlayers().stream()
            .map(info -> PlayerRefs.of(info.getProfile()))
            .toList();
  }

  public static ChatText received(String rawText) {
    return ChatText.parse(rawText, onlinePlayers());
  }

  public static void send(ChatText chatText) {
    outgoingChatText = chatText;
    ChatText wireText =
        ClientPlayNetworking.canSend(ChatTextPayload.TYPE) ? chatText : chatText.stripped();
    ClientCapture.sendChat(wireText.plain());
  }

  public static ChatText sent(String message) {
    ChatText pendingChatText = outgoingChatText;
    outgoingChatText = null;
    ChatText chatText =
        pendingChatText != null && pendingChatText.plain().equals(message)
            ? pendingChatText
            : ChatText.parse(message, onlinePlayers());
    boolean hasStructuredData =
        !chatText.mentioned().isEmpty() || chatText.replyTarget().isPresent();
    if (hasStructuredData && ClientPlayNetworking.canSend(ChatTextPayload.TYPE)) {
      ClientPlayNetworking.send(new ChatTextPayload(chatText));
    }
    return chatText;
  }
}
