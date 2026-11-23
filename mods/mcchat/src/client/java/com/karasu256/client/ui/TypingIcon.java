package com.karasu256.client.ui;

import com.karasu256.ChatNative;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class TypingIcon implements SimpleSynchronousResourceReloadListener {
  private final ResourceLocation id = new ResourceLocation(ChatNative.MOD_ID, "typing_icon");
  private final ResourceLocation svg = new ResourceLocation(ChatNative.MOD_ID, "icons/typing.svg");
  private final int textureSize;
  private boolean loaded;

  public TypingIcon(int textureSize) {
    this.textureSize = textureSize;
  }

  @Override
  public ResourceLocation getFabricId() {
    return id;
  }

  @Override
  public void onResourceManagerReload(ResourceManager manager) {
    loaded = SvgTextures.register(manager, svg, id, textureSize);
  }

  void draw(GuiGraphics graphics, int x, int y, int size, int argb) {
    if (loaded) {
      SvgTextures.draw(graphics, id, textureSize, x, y, size, argb);
    }
  }
}
