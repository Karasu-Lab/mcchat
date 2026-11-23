package com.karasu256.client.mixin;

import com.karasu256.client.ChatNativeClient;
import com.karasu256.client.chat.ClientChatTextProcessor;
import com.karasu256.client.ui.ChatLog;
import java.util.List;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
  @Unique private GuiMessage.Line chatnative$line;

  @ModifyVariable(
      method =
          "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
      at = @At("HEAD"),
      argsOnly = true)
  private GuiMessageTag chatnative$markMention(GuiMessageTag tag) {
    return ClientChatTextProcessor.markMention(tag);
  }

  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void chatnative$hideVanillaList(
      GuiGraphics graphics, int tickCount, int mouseX, int mouseY, CallbackInfo ci) {
    if (ChatNativeClient.chatUi().isActive()) {
      ci.cancel();
      if (Minecraft.getInstance().screen == null) {
        ChatNativeClient.chatUi().renderHud(graphics, tickCount);
      }
    }
  }

  @Inject(method = "clearMessages", at = @At("HEAD"))
  private void chatnative$clearLog(boolean clearHistory, CallbackInfo ci) {
    ChatLog.INSTANCE.clear();
  }

  @Inject(method = "getMessageTagAt", at = @At("HEAD"), cancellable = true)
  private void chatnative$noTag(double x, double y, CallbackInfoReturnable<GuiMessageTag> cir) {
    if (ChatNativeClient.chatUi().isActive()) {
      cir.setReturnValue(null);
    }
  }

  @Inject(method = "getClickedComponentStyleAt", at = @At("HEAD"), cancellable = true)
  private void chatnative$noStyle(double x, double y, CallbackInfoReturnable<Style> cir) {
    if (ChatNativeClient.chatUi().isActive()) {
      cir.setReturnValue(null);
    }
  }

  @Inject(method = "handleChatQueueClicked", at = @At("HEAD"), cancellable = true)
  private void chatnative$noQueueClick(double x, double y, CallbackInfoReturnable<Boolean> cir) {
    if (ChatNativeClient.chatUi().isActive()) {
      cir.setReturnValue(false);
    }
  }

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
    if (line == null || !ClientChatTextProcessor.isMention(line.tag())) {
      return color;
    }
    return (color & 0xFF000000) | 0x00FFF3A0;
  }
}
