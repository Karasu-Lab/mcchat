package com.karasu256.client.mixin;

import com.karasu256.client.capture.ClientCapture;
import com.karasu256.client.screen.ScreenTracked;
import com.karasu256.mcchat.common.MinecraftChatScreen;
import com.karasu256.mcchat.common.MinecraftScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin implements ScreenTracked {
  @Override
  public MinecraftScreen chatnative$screen() {
    return new MinecraftChatScreen();
  }

  @Inject(method = "onEdited", at = @At("HEAD"))
  private void chatnative$onEdited(String text, CallbackInfo ci) {
    ClientCapture.typing(true);
  }

  @Inject(method = "removed", at = @At("HEAD"))
  private void chatnative$onRemoved(CallbackInfo ci) {
    ClientCapture.typing(false);
  }
}
