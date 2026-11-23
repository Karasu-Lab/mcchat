package com.karasu256.client.ui;

import com.karasu256.ChatNative;
import com.karasu256.client.chat.ChatTextRenderer;
import com.karasu256.client.chat.MentionTarget;
import com.karasu256.client.ui.ChatLayout.MessageRow;
import com.karasu256.mcchat.common.PlayerRef;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;

public final class ChatUi {
  private final ChatTheme theme;
  private final ChatLayout layout;
  private final ChatRenderer renderer;
  private final ChatLog log = ChatLog.INSTANCE;

  private ChatLayout.Layout cached;
  private int cachedVersion = -1;
  private int cachedWidth = -1;
  private boolean cachedOwnTyping;
  private boolean cachedOwnName;
  private int scroll;

  public ChatUi(ChatTheme theme) {
    this.theme = theme;
    this.layout = new ChatLayout(theme);
    this.renderer = new ChatRenderer(theme, layout);
  }

  public boolean isActive() {
    Screen screen = Minecraft.getInstance().gui.screen();
    return (screen == null || screen instanceof ChatScreen)
        && !ChatNative.clientConfig().useVanillaChat;
  }

  private record Region(float scale, int left, int width, int bottom, int height, int top) {
    boolean contains(double screenX, double screenY) {
      double x = localX(screenX);
      double y = localY(screenY);
      return x >= 0 && x < width && y >= top && y < bottom;
    }

    double localX(double screenX) {
      return (screenX - left) / scale;
    }

    double localY(double screenY) {
      return screenY / scale;
    }
  }

  private Region region(int screenWidth, int screenHeight) {
    Options options = Minecraft.getInstance().options;
    float scale = options.chatScale().get().floatValue();
    int bottom = Mth.floor((screenHeight - theme.metrics.vanillaChatBottom) / scale);
    double heightOption =
        Minecraft.getInstance().gui.screen() instanceof ChatScreen
            ? options.chatHeightFocused().get()
            : options.chatHeightUnfocused().get();
    int height = Mth.ceil(ChatComponent.getHeight(heightOption) / scale);
    int left = theme.metrics.vanillaChatLeft;
    int width =
        Mth.floor(
            Math.min(
                    ChatComponent.getWidth(options.chatWidth().get()),
                    screenWidth * theme.metrics.maxWidthRatio)
                / scale);
    return new Region(scale, left, width, bottom, height, bottom - height);
  }

  private int replyRowTop(Screen screen) {
    return screen.height
        - theme.metrics.vanillaInputBar
        - theme.metrics.replyGap
        - theme.metrics.replyRowHeight;
  }

  public void render(Screen screen, GuiGraphicsExtractor g) {
    Font font = Minecraft.getInstance().font;
    Region region = region(screen.width, screen.height);
    prepare(font, region);
    draw(g, font, region, origin(region), null);
    renderer.replyStatus(g, font, log, screen.width, replyRowTop(screen));
  }

  public void renderHover(Screen screen, GuiGraphicsExtractor g, int mouseX, int mouseY) {
    Region region = region(screen.width, screen.height);
    Font font = Minecraft.getInstance().font;
    ChatTheme.Metrics m = theme.metrics;
    ChatLog.Message reply = renderer.visibleReply();
    if (isActive()
        && reply != null
        && inside(mouseX, mouseY, 0, replyRowTop(screen), screen.width, m.replyRowHeight)) {
      if (inside(
          mouseX,
          mouseY,
          m.vanillaChatLeft,
          replyRowTop(screen) + (m.replyButton - m.avatar) / 2,
          m.avatar,
          m.avatar)) {
        g.setComponentTooltipForNextFrame(font, senderTooltip(reply.sender()), mouseX, mouseY);
      }
      return;
    }
    if (!isActive() || cached == null || !region.contains(mouseX, mouseY)) {
      return;
    }
    double x = region.localX(mouseX);
    double y = region.localY(mouseY);
    int origin = origin(region);
    for (int i = 0; i < cached.rows().size(); i++) {
      if (!(cached.rows().get(i) instanceof MessageRow row)) {
        continue;
      }
      if (inside(x, y, m.listPadH, origin + cached.tops()[i] + row.avatarY(), m.avatar, m.avatar)) {
        g.setComponentTooltipForNextFrame(
            font, senderTooltip(row.message().sender()), mouseX, mouseY);
        return;
      }
      int textX = row.bubbleX() + m.bubblePadH;
      int textY =
          origin
              + cached.tops()[i]
              + row.bubbleY()
              + m.bubblePadV
              + (row.name() != null ? layout.scaledHeight(font) : 0);
      for (int line = 0; line < row.lines().size(); line++) {
        FormattedCharSequence text = row.lines().get(line);
        if (inside(
            x, y, textX, textY + line * font.lineHeight, font.width(text), font.lineHeight)) {
          Style style = styleAt(font, text, (int) (x - textX));
          if (style != null && style.getHoverEvent() instanceof HoverEvent.ShowEntity show) {
            g.setComponentTooltipForNextFrame(
                font, show.entity().getTooltipLines(), mouseX, mouseY);
            return;
          }
        }
      }
    }
  }

  private static Style styleAt(Font font, FormattedCharSequence text, int x) {
    int[] width = {0};
    Style[] found = {null};
    text.accept((index, style, codepoint) -> {
      width[0] += font.width(FormattedCharSequence.codepoint(codepoint, style));
      if (x < width[0]) {
        found[0] = style;
        return false;
      }
      return true;
    });
    return found[0];
  }

  private List<Component> senderTooltip(PlayerRef sender) {
    if (sender != null) {
      return ChatTextRenderer.playerInfo(sender.uuid(), sender.name()).getTooltipLines();
    }
    GuiMessageTag tag =
        Minecraft.getInstance().getSingleplayerServer() != null
            ? GuiMessageTag.systemSinglePlayer()
            : GuiMessageTag.system();
    return List.of(tag.text());
  }

  public void renderHud(GuiGraphicsExtractor g, int tickCount) {
    Minecraft client = Minecraft.getInstance();
    if (!isActive()
        || client.gui.screen() != null
        || client.gui.hud.isHidden()
        || client.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
      return;
    }
    Region region =
        region(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    build(client.font, region);
    ChatLayout.Row last =
        cached.rows().isEmpty() ? null : cached.rows().get(cached.rows().size() - 1);
    int typingExtra =
        last instanceof ChatLayout.TypingRow ? last.height() + theme.metrics.itemGap : 0;
    draw(
        g,
        client.font,
        region,
        region.bottom() - theme.metrics.itemGap - (cached.height() - typingExtra),
        tickCount);
  }

  private void draw(GuiGraphicsExtractor g, Font font, Region region, int origin, Integer hudTick) {
    double opacity = Minecraft.getInstance().options.chatOpacity().get() * 0.9 + 0.1;
    g.pose().pushMatrix();
    g.pose().translate(region.left(), 0);
    g.pose().scale(region.scale(), region.scale());
    for (int i = 0; i < cached.rows().size(); i++) {
      ChatLayout.Row row = cached.rows().get(i);
      int top = origin + cached.tops()[i];
      float alpha = 1f;
      if (hudTick != null) {
        if (!(row instanceof MessageRow message)) {
          continue;
        }
        int age = hudTick - message.message().addedTick();
        alpha = (float) (timeFactor(age) * opacity);
        if ((int) (255 * alpha) <= 3) {
          continue;
        }
      }
      if (top + row.height() >= region.top() && top <= region.bottom()) {
        Fade.set(alpha);
        renderer.row(g, font, row, top, region.width());
      }
    }
    Fade.set(1f);
    g.pose().popMatrix();
  }

  public boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
    if (!isActive()) {
      return false;
    }
    int replyTop = replyRowTop(screen);
    if (renderer.visibleReply() != null
        && mouseY >= replyTop
        && mouseY < replyTop + theme.metrics.replyRowHeight) {
      replyStatusClicked(screen, mouseX, mouseY, replyTop);
      return true;
    }
    Region region = region(screen.width, screen.height);
    if (!region.contains(mouseX, mouseY)) {
      return false;
    }
    prepare(Minecraft.getInstance().font, region);
    int origin = origin(region);
    double x = region.localX(mouseX);
    double y = region.localY(mouseY);
    for (int i = 0; i < cached.rows().size(); i++) {
      if (!(cached.rows().get(i) instanceof MessageRow row)) {
        continue;
      }
      int top = origin + cached.tops()[i];
      boolean onBubble =
          inside(x, y, row.bubbleX(), top + row.bubbleY(), row.bubbleWidth(), row.bubbleHeight());
      boolean onQuote =
          row.quoted() != null
              && inside(x, y, row.quoteX(), top, row.quoteWidth(), row.quoteHeight());
      boolean onAvatar =
          inside(
              x,
              y,
              theme.metrics.listPadH,
              top + row.avatarY(),
              theme.metrics.avatar,
              theme.metrics.avatar);
      if (button == 0
          && onAvatar
          && row.message().sender() != null
          && screen instanceof MentionTarget target) {
        target.chatnative$insertMention(row.message().sender());
        return true;
      }
      if (button == 1 && onBubble) {
        log.toggleReply(row.message());
        return true;
      }
      if (button == 0 && onQuote) {
        jumpTo(row.quoted(), region);
        return true;
      }
    }
    return false;
  }

  public boolean mouseScrolled(Screen screen, double mouseX, double mouseY, double amount) {
    if (!isActive() || cached == null || cached.rows().isEmpty()) {
      return false;
    }
    scroll += amount > 0 ? theme.metrics.scrollStep : -theme.metrics.scrollStep;
    return true;
  }

  private void replyStatusClicked(Screen screen, double mouseX, double mouseY, int replyTop) {
    ChatTheme.Metrics m = theme.metrics;
    int buttonTop = replyTop;
    if (inside(
        mouseX,
        mouseY,
        renderer.mentionToggleX(screen.width),
        buttonTop,
        m.replyButton,
        m.replyButton)) {
      log.toggleMention();
    } else if (inside(
        mouseX, mouseY, renderer.cancelX(screen.width), buttonTop, m.replyButton, m.replyButton)) {
      log.cancelReply();
    }
  }

  private void jumpTo(ChatLog.Message target, Region region) {
    for (int i = 0; i < cached.rows().size(); i++) {
      if (cached.rows().get(i) instanceof MessageRow row
          && row.message().id().equals(target.id())) {
        int base = region.bottom() - theme.metrics.itemGap - cached.height();
        scroll = region.top() + (region.height() - row.height()) / 2 - cached.tops()[i] - base;
        return;
      }
    }
  }

  private void prepare(Font font, Region region) {
    build(font, region);
    int max = Math.max(0, cached.height() + 2 * theme.metrics.itemGap - region.height());
    scroll = Math.max(0, Math.min(scroll, max));
  }

  private void build(Font font, Region region) {
    boolean ownTyping = ChatNative.clientConfig().showOwnTyping;
    boolean ownName = ChatNative.clientConfig().showOwnName;
    if (cached == null
        || cachedOwnName != ownName
        || cachedVersion != log.version()
        || cachedWidth != region.width()
        || cachedOwnTyping != ownTyping) {
      int previousHeight = cached == null ? 0 : cached.height();
      UUID self = Minecraft.getInstance().getUser().getProfileId();
      List<UUID> typers = log.typing();
      if (!ownTyping) {
        typers.remove(self);
      }
      cached = layout.build(log, typers, font, region.width(), ownName);
      cachedOwnName = ownName;
      cachedVersion = log.version();
      cachedWidth = region.width();
      cachedOwnTyping = ownTyping;
      if (scroll > 0) {
        scroll += cached.height() - previousHeight;
      }
    }
  }

  private int origin(Region region) {
    return region.bottom() - theme.metrics.itemGap - cached.height() + scroll;
  }

  private static double timeFactor(int age) {
    double fade = Mth.clamp((1.0 - age / 200.0) * 10.0, 0.0, 1.0);
    return fade * fade;
  }

  private static boolean inside(double x, double y, int left, int top, int width, int height) {
    return x >= left && x < left + width && y >= top && y < top + height;
  }
}
