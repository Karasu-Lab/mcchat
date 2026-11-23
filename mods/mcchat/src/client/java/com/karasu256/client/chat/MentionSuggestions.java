package com.karasu256.client.chat;

import com.karasu256.client.mixin.EditBoxAccessor;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.component.ChatTextComponent;
import com.karasu256.mcchat.common.component.MentionData;
import com.karasu256.mcchat.common.component.RawText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class MentionSuggestions {
  private final EditBox input;
  private Optional<MentionData.Completion> completion = Optional.empty();
  private int selected;
  private boolean applying;

  public MentionSuggestions(EditBox input) {
    this.input = input;
    List<EditBox.TextFormatter> formatters = ((EditBoxAccessor) input).chatnative$formatters();
    List<EditBox.TextFormatter> vanilla = List.copyOf(formatters);
    formatters.clear();
    input.addFormatter((text, offset) -> {
      FormattedCharSequence base = null;
      for (EditBox.TextFormatter formatter : vanilla) {
        base = formatter.format(text, offset);
        if (base != null) {
          break;
        }
      }
      return styled(base != null ? base : FormattedCharSequence.forward(text, Style.EMPTY), offset);
    });
  }

  private FormattedCharSequence styled(FormattedCharSequence base, int offset) {
    List<Style> styles = new ArrayList<>();
    ChatTextRenderer.render(
            ChatText.parse(input.getValue(), ClientChatTextProcessor.onlinePlayers()))
        .visit(
            (style, content) -> {
              content.chars().forEach(character -> styles.add(style));
              return Optional.empty();
            },
            Style.EMPTY);
    return sink ->
        base.accept((index, style, codePoint) -> {
          int absolute = offset + index;
          Style rendered = absolute < styles.size() ? styles.get(absolute) : Style.EMPTY;
          return sink.accept(index, rendered.applyTo(style), codePoint);
        });
  }

  public void update() {
    if (applying) {
      return;
    }
    List<PlayerRef> players = ClientChatTextProcessor.onlinePlayers();
    completion =
        MentionData.complete(
            ChatText.parse(input.getValue(), players), input.getCursorPosition(), players);
    selected = 0;
  }

  public boolean keyPressed(KeyEvent event) {
    if (completion.isEmpty()) {
      return false;
    }
    List<PlayerRef> candidates = completion.get().candidates();
    if (event.isUp()) {
      selected = (selected + candidates.size() - 1) % candidates.size();
      return true;
    }
    if (event.isDown()) {
      selected = (selected + 1) % candidates.size();
      return true;
    }
    if (event.isCycleFocus() || event.isConfirmation()) {
      insert(candidates.get(selected));
      return true;
    }
    if (event.isEscape()) {
      completion = Optional.empty();
      return true;
    }
    return false;
  }

  public void render(GuiGraphicsExtractor graphics) {
    if (completion.isEmpty()) {
      return;
    }
    Font font = Minecraft.getInstance().font;
    List<PlayerRef> candidates = completion.get().candidates();
    int rows = Math.min(candidates.size(), 5);
    int width = 0;
    for (int i = 0; i < rows; i++) {
      width = Math.max(width, font.width(candidates.get(i).name()));
    }
    int left = input.getX();
    int top = input.getY() - 4 - rows * 12;
    graphics.fill(left, top - 2, left + width + 8, top + rows * 12 + 2, 0xD0000000);
    for (int i = 0; i < rows; i++) {
      int color = i == selected ? 0xFFFFFF55 : 0xFFAAAAAA;
      graphics.text(font, candidates.get(i).name(), left + 4, top + i * 12 + 2, color);
    }
  }

  public void insertMention(PlayerRef player) {
    ChatText text = ChatText.parse(input.getValue(), ClientChatTextProcessor.onlinePlayers());
    int cursor = input.getCursorPosition();
    int offset = 0;
    for (ChatTextComponent component : text.components()) {
      int length = component.plain().length();
      if (!(component instanceof RawText) && cursor > offset && cursor < offset + length) {
        cursor = offset + length;
      }
      offset += length;
    }
    MentionData mention = new MentionData(player.uuid(), player.name());
    int next = cursor + mention.plain().length();
    applying = true;
    input.setValue(text.replace(cursor, cursor, List.of(mention)).plain());
    input.setCursorPosition(next);
    input.setHighlightPos(next);
    applying = false;
    completion = Optional.empty();
  }

  private void insert(PlayerRef player) {
    MentionData.Inserted inserted = completion.get().insert(player);
    applying = true;
    input.setValue(inserted.text().plain());
    input.setCursorPosition(inserted.cursor());
    input.setHighlightPos(inserted.cursor());
    applying = false;
    completion = Optional.empty();
  }
}
