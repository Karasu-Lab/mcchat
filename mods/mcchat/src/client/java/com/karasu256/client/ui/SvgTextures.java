package com.karasu256.client.ui;

import com.karasu256.ChatNative;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

final class SvgTextures {
  private SvgTextures() {}

  static boolean register(
      ResourceManager manager, ResourceLocation svg, ResourceLocation texture, int size) {
    try (InputStream stream = manager.getResourceOrThrow(svg).open()) {
      NativeImage image = SvgRasterizer.rasterize(stream, size);
      DynamicTexture dynamic = new DynamicTexture(image);
      dynamic.setFilter(true, false);
      Minecraft.getInstance().getTextureManager().register(texture, dynamic);
      return true;
    } catch (IOException e) {
      ChatNative.LOGGER.error("Failed to load the icon {}", svg, e);
      return false;
    }
  }

  static void draw(
      GuiGraphics graphics, ResourceLocation texture, int textureSize, int x, int y, int size, int argb) {
    float outer = RenderSystem.getShaderColor()[3];
    RenderSystem.enableBlend();
    RenderSystem.setShaderColor(
        ((argb >> 16) & 0xFF) / 255f,
        ((argb >> 8) & 0xFF) / 255f,
        (argb & 0xFF) / 255f,
        outer * ((argb >>> 24) / 255f));
    graphics.blit(
        texture, x, y, size, size, 0f, 0f, textureSize, textureSize, textureSize, textureSize);
    RenderSystem.setShaderColor(1f, 1f, 1f, outer);
  }
}
