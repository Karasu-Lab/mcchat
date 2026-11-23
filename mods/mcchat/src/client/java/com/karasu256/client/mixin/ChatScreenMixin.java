package com.karasu256.client.mixin;

import com.karasu256.client.capture.ClientCapture;
import com.karasu256.client.chat.MentionSuggestions;
import com.karasu256.client.screen.ScreenTracked;
import com.karasu256.mcchat.common.MinecraftChatScreen;
import com.karasu256.mcchat.common.MinecraftScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin implements ScreenTracked {
  @Shadow protected EditBox input;

  @Unique private MentionSuggestions chatnative$mentions;

  @Override
  public MinecraftScreen chatnative$screen() {
    return new MinecraftChatScreen();
  }

  @Inject(method = "init", at = @At("TAIL"))
  private void chatnative$onInit(CallbackInfo ci) {
    chatnative$mentions = new MentionSuggestions(input);
  }

  @Inject(method = "onEdited", at = @At("HEAD"))
  private void chatnative$onEdited(String text, CallbackInfo ci) {
    ClientCapture.typing(true);
    if (chatnative$mentions != null) {
      chatnative$mentions.update();
    }
  }

  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void chatnative$onKeyPressed(
      int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
    if (chatnative$mentions != null && chatnative$mentions.keyPressed(keyCode)) {
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "render", at = @At("TAIL"))
  private void chatnative$onRender(
      GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    if (chatnative$mentions != null) {
      chatnative$mentions.render(graphics);
    }
  }

  @Inject(method = "removed", at = @At("HEAD"))
  private void chatnative$onRemoved(CallbackInfo ci) {
    ClientCapture.typing(false);
  }
}
