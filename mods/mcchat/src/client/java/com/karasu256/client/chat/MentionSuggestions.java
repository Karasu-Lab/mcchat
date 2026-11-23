package com.karasu256.client.chat;

import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.component.MentionData;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;

public final class MentionSuggestions {
  private final EditBox input;
  private Optional<MentionData.Completion> completion = Optional.empty();
  private int selected;
  private boolean applying;

  public MentionSuggestions(EditBox input) {
    this.input = input;
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

  public boolean keyPressed(int keyCode) {
    if (completion.isEmpty()) {
      return false;
    }
    List<PlayerRef> candidates = completion.get().candidates();
    switch (keyCode) {
      case GLFW.GLFW_KEY_UP:
        selected = (selected + candidates.size() - 1) % candidates.size();
        return true;
      case GLFW.GLFW_KEY_DOWN:
        selected = (selected + 1) % candidates.size();
        return true;
      case GLFW.GLFW_KEY_TAB:
      case GLFW.GLFW_KEY_ENTER:
      case GLFW.GLFW_KEY_KP_ENTER:
        insert(candidates.get(selected));
        return true;
      case GLFW.GLFW_KEY_ESCAPE:
        completion = Optional.empty();
        return true;
      default:
        return false;
    }
  }

  public void render(GuiGraphics graphics) {
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
      graphics.drawString(font, candidates.get(i).name(), left + 4, top + i * 12 + 2, color);
    }
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
