package com.karasu256.client.ui;

import com.karasu256.client.chat.ChatTextRenderer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

final class ChatLayout {
  private final ChatTheme theme;
  private final DateTimeFormatter timeFormat =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

  ChatLayout(ChatTheme theme) {
    this.theme = theme;
  }

  interface Row {
    int height();
  }

  record MessageRow(
      ChatLog.Message message,
      ChatLog.Message quoted,
      FormattedCharSequence quote,
      int quoteX,
      int quoteWidth,
      int quoteHeight,
      List<FormattedCharSequence> lines,
      String name,
      int bubbleX,
      int bubbleY,
      int bubbleWidth,
      int bubbleHeight,
      int avatarY,
      String time,
      int height)
      implements Row {}

  record DividerRow(String label, int labelWidth, int height) implements Row {}

  record TypingRow(List<UUID> typers, int height) implements Row {}

  record Layout(List<Row> rows, int[] tops, int height) {}

  int scaledWidth(Font font, FormattedCharSequence text) {
    return (int) Math.ceil(font.width(text) * theme.metrics.small);
  }

  int scaledHeight(Font font) {
    return (int) Math.ceil(font.lineHeight * theme.metrics.small);
  }

  String senderName(ChatLog.Message message) {
    return message.sender() == null
        ? Component.translatable("chatnative.chat.system").getString()
        : message.sender().name();
  }

  FormattedCharSequence quoteText(Font font, ChatLog.Message target, int maxWidth, float scale) {
    String plain = target.text().plain().replace('\n', ' ');
    int limit = theme.metrics.previewLimit;
    boolean truncated = plain.codePointCount(0, plain.length()) > limit;
    String preview =
        truncated ? plain.substring(0, plain.offsetByCodePoints(0, limit)) + "…" : plain;
    Component name =
        Component.literal(senderName(target))
            .withStyle(Style.EMPTY.withColor(theme.colors.primary & 0xFFFFFF));
    Component line = Component.empty().append(name).append(" " + preview);
    int budget = Math.max(0, Math.round(maxWidth / scale));
    if (font.width(line) <= budget) {
      return line.getVisualOrderText();
    }
    FormattedText head = font.substrByWidth(line, Math.max(0, budget - font.width("…")));
    return Language.getInstance()
        .getVisualOrder(FormattedText.composite(head, FormattedText.of("…")));
  }

  Layout build(ChatLog log, List<UUID> typers, Font font, int width, boolean showOwnName) {
    ChatTheme.Metrics m = theme.metrics;
    List<Row> rows = new ArrayList<>();
    for (ChatLog.Item item : log.items()) {
      if (item instanceof ChatLog.Message message) {
        rows.add(message(log, font, message, width, showOwnName));
      } else if (item instanceof ChatLog.Divider divider) {
        String label = divider.worldName().isEmpty() ? divider.worldId() : divider.worldName();
        rows.add(
            new DividerRow(
                label,
                scaledWidth(font, Component.literal(label).getVisualOrderText()),
                2 * m.dividerPadV + scaledHeight(font)));
      }
    }
    if (!typers.isEmpty()) {
      rows.add(new TypingRow(typers, Math.max(m.avatar, 2 * m.typingPad + m.dot)));
    }
    int[] tops = new int[rows.size()];
    int y = 0;
    for (int i = 0; i < rows.size(); i++) {
      tops[i] = y;
      y += rows.get(i).height() + (i + 1 < rows.size() ? m.itemGap : 0);
    }
    return new Layout(rows, tops, y);
  }

  private MessageRow message(
      ChatLog log, Font font, ChatLog.Message message, int width, boolean showOwnName) {
    ChatTheme.Metrics m = theme.metrics;
    String time = timeFormat.format(Instant.ofEpochMilli(message.timestampMillis()));
    int timeWidth = scaledWidth(font, Component.literal(time).getVisualOrderText());
    int bubbleMax = width - 2 * m.listPadH - m.avatar - m.avatarGap - m.timeGap - timeWidth;
    List<FormattedCharSequence> lines =
        font.split(
            ChatTextRenderer.render(message.text(), message.component()),
            bubbleMax - 2 * m.bubblePadH);
    int contentWidth = 0;
    for (FormattedCharSequence line : lines) {
      contentWidth = Math.max(contentWidth, font.width(line));
    }
    String name = message.own() && !showOwnName ? null : senderName(message);
    int nameHeight = 0;
    if (name != null) {
      contentWidth =
          Math.max(contentWidth, scaledWidth(font, Component.literal(name).getVisualOrderText()));
      nameHeight = scaledHeight(font);
    }
    int bubbleWidth = contentWidth + 2 * m.bubblePadH;
    int bubbleHeight = 2 * m.bubblePadV + nameHeight + Math.max(lines.size(), 1) * font.lineHeight;

    ChatLog.Message quoted = message.text().replyTarget().flatMap(log::find).orElse(null);
    FormattedCharSequence quote = null;
    int quoteWidth = 0;
    int quoteBlock = 0;
    int quoteHeight = 0;
    if (quoted != null) {
      quoteHeight = font.lineHeight + 2 * m.quotePadV;
      int fixedWidth = 2 * m.quotePadH + font.lineHeight + 2 * m.quoteGap + font.lineHeight;
      quote = quoteText(font, quoted, bubbleMax - fixedWidth, 1f);
      quoteWidth = fixedWidth + font.width(quote);
      quoteBlock = quoteHeight + m.quoteColumnGap;
    }
    int height = quoteBlock + Math.max(bubbleHeight, m.avatar);
    return new MessageRow(
        message,
        quoted,
        quote,
        m.listPadH + m.inset,
        quoteWidth,
        quoteHeight,
        lines,
        name,
        m.listPadH + m.avatar + m.avatarGap,
        height - bubbleHeight,
        bubbleWidth,
        bubbleHeight,
        height - m.avatar,
        time,
        height);
  }
}
