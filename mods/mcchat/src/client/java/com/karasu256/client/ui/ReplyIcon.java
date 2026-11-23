package com.karasu256.client.ui;

import com.karasu256.ChatNative;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ReplyIcon implements SimpleSynchronousResourceReloadListener {
  private final ResourceLocation id = new ResourceLocation(ChatNative.MOD_ID, "reply_icon");
  private final ResourceLocation svg = new ResourceLocation(ChatNative.MOD_ID, "icons/reply.svg");
  private final int textureSize;
  private boolean loaded;

  public ReplyIcon(int textureSize) {
    this.textureSize = textureSize;
  }

  @Override
  public ResourceLocation getFabricId() {
    return id;
  }

  @Override
  public void onResourceManagerReload(ResourceManager manager) {
    loaded = false;
    try (InputStream stream = manager.getResourceOrThrow(svg).open()) {
      NativeImage image = SvgRasterizer.rasterize(stream, textureSize);
      DynamicTexture texture = new DynamicTexture(image);
      texture.setFilter(true, false);
      Minecraft.getInstance().getTextureManager().register(id, texture);
      loaded = true;
    } catch (IOException e) {
      ChatNative.LOGGER.error("Failed to load the reply icon {}", svg, e);
    }
  }

  void draw(GuiGraphics graphics, int x, int y, int size, int argb) {
    if (!loaded) {
      return;
    }
    float outer = RenderSystem.getShaderColor()[3];
    RenderSystem.enableBlend();
    RenderSystem.setShaderColor(
        ((argb >> 16) & 0xFF) / 255f,
        ((argb >> 8) & 0xFF) / 255f,
        (argb & 0xFF) / 255f,
        outer * ((argb >>> 24) / 255f));
    graphics.blit(id, x, y, size, size, 0f, 0f, textureSize, textureSize, textureSize, textureSize);
    RenderSystem.setShaderColor(1f, 1f, 1f, outer);
  }
}
