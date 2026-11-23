package com.karasu256.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatComponent.class)
public interface ChatComponentInvoker {
  @Invoker("getTimeFactor")
  static double getTimeFactor(int age) {
    throw new AssertionError();
  }
}
