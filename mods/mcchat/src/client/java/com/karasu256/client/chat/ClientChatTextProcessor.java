package com.karasu256.client.chat;

import com.karasu256.capture.PlayerRefs;
import com.karasu256.client.capture.ClientCapture;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.net.ChatTextPayload;
import com.karasu256.net.ReceivedChatTextPayload;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class ClientChatTextProcessor {
  private static volatile ReceivedChatTextPayload structuredPayload;
  private static volatile ChatText outgoingChatText;
  private static volatile boolean highlightNextMessage;

  private ClientChatTextProcessor() {}

  public static void structured(ReceivedChatTextPayload payload) {
    if (payload.text() != null && payload.text().valid()) {
      structuredPayload = payload;
    }
  }

  public static GuiMessageTag markMention(GuiMessageTag tag) {
    if (!highlightNextMessage) {
      return tag;
    }
    highlightNextMessage = false;
    return new GuiMessageTag(
        tag == null ? 0 : tag.indicatorColor(),
        tag == null ? null : tag.icon(),
        tag == null ? null : tag.text(),
        "chatnative:mention");
  }

  public static boolean isMention(GuiMessageTag tag) {
    return tag != null && "chatnative:mention".equals(tag.logTag());
  }

  public static void finishChatMessage() {
    highlightNextMessage = false;
  }

  public static List<PlayerRef> onlinePlayers() {
    ClientPacketListener connection = Minecraft.getInstance().getConnection();
    return connection == null
        ? List.of()
        : connection.getOnlinePlayers().stream()
            .map(info -> PlayerRefs.of(info.getProfile()))
            .toList();
  }

  public static ChatText received(UUID sender, String rawText) {
    ReceivedChatTextPayload payload = structuredPayload;
    structuredPayload = null;
    if (payload == null
        || !payload.sender().equals(sender)
        || !payload.text().plain().equals(rawText)) {
      return ChatText.parse(rawText, onlinePlayers());
    }
    highlightNextMessage =
        payload.text().mentioned().contains(ClientCapture.sessionPlayer().uuid());
    return payload.text();
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
