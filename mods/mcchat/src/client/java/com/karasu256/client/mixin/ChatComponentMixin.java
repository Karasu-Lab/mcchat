package com.karasu256.client.mixin;

import com.karasu256.client.capture.ClientCapture;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
  @Unique private GuiMessage.Line chatnative$line;

  @Redirect(
      method = "render",
      at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", ordinal = 0))
  private Object chatnative$captureLine(List<?> lines, int index) {
    Object line = lines.get(index);
    chatnative$line = (GuiMessage.Line) line;
    return line;
  }

  @ModifyArg(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
              ordinal = 0),
      index = 4)
  private int chatnative$highlight(int color) {
    GuiMessage.Line line = chatnative$line;
    if (line == null || !chatnative$mentionsMe(line)) {
      return color;
    }
    return (color & 0xFF000000) | 0x00FFF3A0;
  }

  @Unique
  private static boolean chatnative$mentionsMe(GuiMessage.Line line) {
    StringBuilder text = new StringBuilder();
    line.content()
        .accept((index, style, codePoint) -> {
          text.appendCodePoint(codePoint);
          return true;
        });
    PlayerRef me = ClientCapture.sessionPlayer();
    return ChatText.parse(text.toString(), List.of(me)).mentioned().contains(me.uuid());
  }
}
