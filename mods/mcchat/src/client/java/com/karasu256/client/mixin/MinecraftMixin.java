package com.karasu256.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @Shadow public Screen screen;

  @Inject(method = "setScreen", at = @At("HEAD"))
  private void chatnative$onScreenClosing(Screen next, CallbackInfo ci) {
    if (next == null && screen != null) {
      screen.chatnative$closed();
    }
  }

  @Inject(method = "setScreen", at = @At("TAIL"))
  private void chatnative$onScreenOpened(Screen next, CallbackInfo ci) {
    if (screen != null) {
      screen.chatnative$opened();
    }
  }
}
