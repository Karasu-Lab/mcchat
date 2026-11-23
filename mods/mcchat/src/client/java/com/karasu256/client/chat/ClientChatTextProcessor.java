package com.karasu256.client.chat;

import com.karasu256.capture.PlayerRefs;
import com.karasu256.client.capture.ClientCapture;
import com.karasu256.client.ui.ChatLog;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.net.ChatTextPayload;
import com.karasu256.net.ReceivedChatTextPayload;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

public final class ClientChatTextProcessor {
  private static final List<Consumer<ReceivedChat>> listeners = new CopyOnWriteArrayList<>();
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

  public static void register() {
    ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTime) ->
        dispatch(
            received(
                sender == null ? null : PlayerRefs.of(sender),
                signed != null ? signed.signedContent() : message.getString(),
                signed != null ? signed.decoratedContent() : message)));

    ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
      if (!overlay) {
        dispatch(received(null, message.getString(), message));
      }
    });
  }

  public static void listen(Consumer<ReceivedChat> listener) {
    listeners.add(listener);
  }

  private static void dispatch(ReceivedChat chat) {
    listeners.forEach(listener -> listener.accept(chat));
  }

  private static ReceivedChat received(PlayerRef sender, String rawText, Component display) {
    ReceivedChatTextPayload payload = structuredPayload;
    structuredPayload = null;
    UUID senderId = sender == null ? null : sender.uuid();
    long now = System.currentTimeMillis();
    if (payload == null
        || !payload.sender().equals(senderId)
        || !payload.text().plain().equals(rawText)) {
      return new ReceivedChat(
          UUID.randomUUID(), sender, ChatText.raw(rawText), display, false, now);
    }
    boolean mention = payload.text().mentioned().contains(ClientCapture.sessionPlayer().uuid());
    highlightNextMessage = mention;
    return new ReceivedChat(payload.messageId(), sender, payload.text(), display, mention, now);
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
    chatText = ChatLog.INSTANCE.attachReply(chatText);
    boolean hasStructuredData =
        !chatText.mentioned().isEmpty() || chatText.replyTarget().isPresent();
    if (hasStructuredData && ClientPlayNetworking.canSend(ChatTextPayload.TYPE)) {
      ClientPlayNetworking.send(new ChatTextPayload(chatText));
    }
    return chatText;
  }
}
