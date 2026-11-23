package com.karasu256.client.ui;

import com.karasu256.client.ui.ChatLayout.DividerRow;
import com.karasu256.client.ui.ChatLayout.MessageRow;
import com.karasu256.client.ui.ChatLayout.TypingRow;
import com.karasu256.mcchat.common.component.MentionData;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

final class ChatRenderer {
  private final ChatTheme theme;
  private final ChatLayout layout;
  private final ReplyIcon replyIcon;
  private final FadeAnimation mentionFade;
  private final FadeAnimation replyFade;
  private ChatLog.Message shownReply;

  ChatRenderer(ChatTheme theme, ChatLayout layout, ReplyIcon replyIcon) {
    this.theme = theme;
    this.layout = layout;
    this.replyIcon = replyIcon;
    this.mentionFade = new FadeAnimation(theme.metrics.fadeMillis, 1f);
    this.replyFade = new FadeAnimation(theme.metrics.fadeMillis, 0f);
  }

  void row(GuiGraphics g, Font font, ChatLayout.Row row, int top, int width) {
    if (row instanceof MessageRow message) {
      message(g, font, message, top);
    } else if (row instanceof DividerRow divider) {
      divider(g, font, divider, top, width);
    } else if (row instanceof TypingRow typing) {
      typing(g, font, typing, top);
    }
  }

  void replyStatus(GuiGraphics g, Font font, ChatLog log, int width, int top) {
    if (log.replyTarget() != null) {
      shownReply = log.replyTarget();
    }
    replyFade.target(log.replyTarget() != null ? 1f : 0f);
    if (shownReply == null || log.replyTarget() == null && replyFade.alpha() <= 0f) {
      shownReply = null;
      return;
    }
    replyFade.render(g);
    replyRow(g, font, log, shownReply, width, top);
    replyFade.postRender(g);
  }

  private void replyRow(
      GuiGraphics g, Font font, ChatLog log, ChatLog.Message target, int width, int top) {
    ChatTheme.Metrics m = theme.metrics;
    g.fill(
        m.vanillaInputSide,
        top,
        width - m.vanillaInputSide,
        top + m.replyRowHeight,
        Minecraft.getInstance().options.getBackgroundColor(Integer.MIN_VALUE));
    int buttonTop = top;
    int avatarX = m.vanillaChatLeft;
    avatar(
        g,
        font,
        target.sender() == null ? null : target.sender().uuid(),
        layout.senderName(target),
        avatarX,
        buttonTop + (m.replyButton - m.avatar) / 2,
        m.avatar);
    int textX = avatarX + m.avatar + m.quoteGap;
    FormattedCharSequence text =
        layout.quoteText(font, target, Math.max(mentionToggleX(width) - m.quoteGap - textX, 0), 1f);
    g.drawString(
        font,
        text,
        textX,
        buttonTop + (m.replyButton - font.lineHeight + 1) / 2,
        theme.colors.onSurfaceVariant,
        false);
    String mention = String.valueOf(MentionData.PREFIX);
    glyph(g, font, mention, mentionToggleX(width), buttonTop, theme.colors.onSurfaceVariant);
    mentionFade.target(log.replyMention() ? 1f : 0f);
    mentionFade.render(g);
    glyph(g, font, mention, mentionToggleX(width), buttonTop, theme.colors.primary);
    mentionFade.postRender(g);
    glyph(g, font, "×", cancelX(width), buttonTop, theme.colors.onSurfaceVariant);
  }

  int cancelX(int width) {
    return width - theme.metrics.vanillaInputSide - theme.metrics.replyButton;
  }

  int mentionToggleX(int width) {
    return cancelX(width) - theme.metrics.replyButton;
  }

  private void message(GuiGraphics g, Font font, MessageRow row, int top) {
    ChatTheme.Metrics m = theme.metrics;
    ChatTheme.Colors c = theme.colors;
    ChatLog.Message message = row.message();
    avatar(
        g,
        font,
        message.sender() == null ? null : message.sender().uuid(),
        layout.senderName(message),
        m.listPadH,
        top + row.avatarY(),
        m.avatar);

    if (row.quoted() != null) {
      quote(g, font, row, top);
    }

    int container =
        message.mention()
            ? c.secondaryContainer
            : message.own() ? c.primaryContainer : c.surfaceContainerHigh;
    int content =
        message.mention()
            ? c.onSecondaryContainer
            : message.own() ? c.onPrimaryContainer : c.onSurface;
    int bubbleY = top + row.bubbleY();
    fillRound(
        g,
        row.bubbleX(),
        bubbleY,
        row.bubbleX() + row.bubbleWidth(),
        bubbleY + row.bubbleHeight(),
        m.corner,
        container);

    int textX = row.bubbleX() + m.bubblePadH;
    int textY = bubbleY + m.bubblePadV;
    if (row.name() != null) {
      scaled(g, font, Component.literal(row.name()).getVisualOrderText(), textX, textY, c.primary);
      textY += layout.scaledHeight(font);
    }
    for (FormattedCharSequence line : row.lines()) {
      g.drawString(font, line, textX, textY, content, false);
      textY += font.lineHeight;
    }

    scaled(
        g,
        font,
        Component.literal(row.time()).getVisualOrderText(),
        row.bubbleX() + row.bubbleWidth() + m.timeGap,
        top + row.height() - layout.scaledHeight(font),
        c.onSurfaceVariant);
  }

  private void quote(GuiGraphics g, Font font, MessageRow row, int top) {
    ChatTheme.Metrics m = theme.metrics;
    int x = row.quoteX();
    int height = row.quoteHeight();
    fillRound(
        g, x, top, x + row.quoteWidth(), top + height, height / 2, theme.colors.surfaceContainer);
    int avatarX = x + m.quotePadH;
    ChatLog.Message target = row.quoted();
    avatar(
        g,
        font,
        target.sender() == null ? null : target.sender().uuid(),
        layout.senderName(target),
        avatarX,
        top + m.quotePadV,
        font.lineHeight);
    int textX = avatarX + font.lineHeight + m.quoteGap;
    g.drawString(font, row.quote(), textX, top + m.quotePadV, theme.colors.onSurfaceVariant, false);
    replyIcon.draw(
        g,
        textX + font.width(row.quote()) + m.quoteGap,
        top + m.quotePadV,
        font.lineHeight,
        theme.colors.onSurfaceVariant);
  }

  private void divider(GuiGraphics g, Font font, DividerRow row, int top, int width) {
    ChatTheme.Metrics m = theme.metrics;
    int lineWidth = (width - 2 * m.listPadH - row.labelWidth() - 2 * m.dividerGap) / 2;
    int middle = top + row.height() / 2;
    g.fill(m.listPadH, middle, m.listPadH + lineWidth, middle + 1, theme.colors.outlineVariant);
    int labelX = m.listPadH + lineWidth + m.dividerGap;
    scaled(
        g,
        font,
        Component.literal(row.label()).getVisualOrderText(),
        labelX,
        top + m.dividerPadV,
        theme.colors.onSurfaceVariant);
    g.fill(
        labelX + row.labelWidth() + m.dividerGap,
        middle,
        width - m.listPadH,
        middle + 1,
        theme.colors.outlineVariant);
  }

  private void typing(GuiGraphics g, Font font, TypingRow row, int top) {
    ChatTheme.Metrics m = theme.metrics;
    List<UUID> typers = row.typers();
    int avatarY = top + row.height() - m.avatar;
    for (int i = 0; i < typers.size(); i++) {
      int avatarX = m.listPadH + i * m.stackStep;
      if (i > 0) {
        fillRound(
            g,
            avatarX - 1,
            avatarY - 1,
            avatarX + m.avatar + 1,
            avatarY + m.avatar + 1,
            (m.avatar + 2) / 2,
            theme.colors.surface);
      }
      PlayerInfo info = playerInfo(typers.get(i));
      avatar(
          g,
          font,
          typers.get(i),
          info == null ? "?" : info.getProfile().getName(),
          avatarX,
          avatarY,
          m.avatar);
    }
    int bubbleX = m.listPadH + (typers.size() - 1) * m.stackStep + m.avatar + m.avatarGap;
    int bubbleWidth = 3 * m.dot + 2 * m.dotGap + 2 * m.typingPad;
    int bubbleHeight = m.dot + 2 * m.typingPad;
    int bubbleY = top + row.height() - bubbleHeight;
    fillRound(
        g,
        bubbleX,
        bubbleY,
        bubbleX + bubbleWidth,
        bubbleY + bubbleHeight,
        m.corner,
        theme.colors.surfaceContainerHigh);
    float phase = (System.currentTimeMillis() % m.typingCycleMillis) / (float) m.typingCycleMillis;
    for (int i = 0; i < 3; i++) {
      float t = (phase - i * m.typingStagger + 1f) % 1f;
      float alpha = 0.3f + 0.7f * (t < 0.5f ? t * 2f : (1f - t) * 2f);
      int color = ((int) (alpha * 255) << 24) | (theme.colors.onSurfaceVariant & 0xFFFFFF);
      int dotX = bubbleX + m.typingPad + i * (m.dot + m.dotGap);
      int dotY = bubbleY + m.typingPad;
      g.fill(dotX + 1, dotY, dotX + m.dot - 1, dotY + m.dot, color);
      g.fill(dotX, dotY + 1, dotX + m.dot, dotY + m.dot - 1, color);
    }
  }

  private void glyph(GuiGraphics g, Font font, String text, int x, int y, int color) {
    int button = theme.metrics.replyButton;
    g.drawString(
        font,
        text,
        x + (button - font.width(text)) / 2,
        y + (button - font.lineHeight + 1) / 2,
        color,
        false);
  }

  private void scaled(
      GuiGraphics g, Font font, FormattedCharSequence text, int x, int y, int color) {
    g.pose().pushPose();
    g.pose().translate(x, y, 0);
    g.pose().scale(theme.metrics.small, theme.metrics.small, 1f);
    g.drawString(font, text, 0, 0, color, false);
    g.pose().popPose();
  }

  private PlayerInfo playerInfo(UUID id) {
    ClientPacketListener connection = Minecraft.getInstance().getConnection();
    return id == null || connection == null ? null : connection.getPlayerInfo(id);
  }

  private void avatar(GuiGraphics g, Font font, UUID id, String name, int x, int y, int size) {
    PlayerInfo info = playerInfo(id);
    if (info == null) {
      fillRound(g, x, y, x + size, y + size, size / 2, theme.colors.surfaceContainerHighest);
      String initial = name.isEmpty() ? "?" : new String(Character.toChars(name.codePointAt(0)));
      g.drawString(
          font,
          initial,
          x + (size - font.width(initial)) / 2,
          y + (size - font.lineHeight + 1) / 2,
          theme.colors.onSurfaceVariant,
          false);
      return;
    }
    face(g, info.getSkin().texture(), x, y, size);
  }

  private void face(GuiGraphics g, ResourceLocation texture, int x, int y, int size) {
    float radius = size / 2f;
    ChatTheme.Metrics m = theme.metrics;
    float texelsPerUnit = (float) m.faceTexels / size;
    RenderSystem.enableBlend();
    for (int row = 0; row < size; row++) {
      float dy = row + 0.5f - radius;
      float half = (float) Math.sqrt(Math.max(0f, radius * radius - dy * dy));
      int left = Math.round(radius - half);
      int right = Math.round(radius + half);
      if (right <= left) {
        continue;
      }
      int uWidth = Math.max(1, Math.round((right - left) * texelsPerUnit));
      for (int layerU : new int[] {m.faceU, m.hatU}) {
        g.blit(
            texture,
            x + left,
            y + row,
            right - left,
            1,
            layerU + left * texelsPerUnit,
            m.faceV + (int) (row * texelsPerUnit),
            uWidth,
            1,
            m.skinSize,
            m.skinSize);
      }
    }
  }

  static void fillRound(GuiGraphics g, int x0, int y0, int x1, int y1, int radius, int color) {
    int r = Math.max(0, Math.min(radius, Math.min((x1 - x0) / 2, (y1 - y0) / 2)));
    for (int i = 0; i < r; i++) {
      float dy = r - i - 0.5f;
      int inset = Math.round(r - (float) Math.sqrt(r * r - dy * dy));
      g.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, color);
      g.fill(x0 + inset, y1 - i - 1, x1 - inset, y1 - i, color);
    }
    g.fill(x0, y0 + r, x1, y1 - r, color);
  }
}
