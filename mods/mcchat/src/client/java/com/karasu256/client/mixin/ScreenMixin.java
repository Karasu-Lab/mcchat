package com.karasu256.client.mixin;

import com.karasu256.client.screen.ScreenTracked;
import com.karasu256.mcchat.common.MinecraftScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Screen.class)
public abstract class ScreenMixin implements ScreenTracked {
  @Override
  public MinecraftScreen chatnative$screen() {
    return new MinecraftScreen(getClass().getName());
  }
}
