package com.karasu256.client.mixin;

import com.karasu256.client.ChatNativeClient;
import com.karasu256.client.chat.ClientChatTextProcessor;
import com.karasu256.client.ui.ChatLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
  @Unique private static GuiMessage.Line chatnative$line;

  @ModifyVariable(
      method =
          "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
      at = @At("HEAD"),
      argsOnly = true)
  private GuiMessageTag chatnative$markMention(GuiMessageTag tag) {
    return ClientChatTextProcessor.markMention(tag);
  }

  @Inject(
      method =
          "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
      at = @At("HEAD"),
      cancellable = true)
  private void chatnative$hideVanillaList(
      GuiGraphicsExtractor graphics,
      Font font,
      int tickCount,
      int mouseX,
      int mouseY,
      ChatComponent.DisplayMode mode,
      boolean focused,
      CallbackInfo ci) {
    if (ChatNativeClient.chatUi().isActive()) {
      ci.cancel();
      if (Minecraft.getInstance().gui.screen() == null) {
        ChatNativeClient.chatUi().renderHud(graphics, tickCount);
      }
    }
  }

  @Inject(method = "clearMessages", at = @At("HEAD"))
  private void chatnative$clearLog(boolean clearHistory, CallbackInfo ci) {
    ChatLog.INSTANCE.clear();
  }

  @Inject(method = "captureClickableText", at = @At("HEAD"), cancellable = true)
  private void chatnative$noClickableText(
      net.minecraft.client.gui.ActiveTextCollector collector,
      int mouseX,
      int mouseY,
      ChatComponent.DisplayMode mode,
      CallbackInfo ci) {
    if (ChatNativeClient.chatUi().isActive()) {
      ci.cancel();
    }
  }

  @Inject(method = "lambda$extractRenderState$1", at = @At("HEAD"))
  private static void chatnative$captureLine(
      int left,
      int bottom,
      ChatComponent.ChatGraphicsAccess access,
      int padding,
      float opacity,
      GuiMessage.Line line,
      int index,
      float alpha,
      CallbackInfo ci) {
    chatnative$line = line;
  }

  @ModifyArg(
      method = "lambda$extractRenderState$1",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V"),
      index = 4)
  private static int chatnative$highlight(int color) {
    GuiMessage.Line line = chatnative$line;
    if (line == null || !ClientChatTextProcessor.isMention(line.tag())) {
      return color;
    }
    return (color & 0xFF000000) | 0x00FFF3A0;
  }
}
