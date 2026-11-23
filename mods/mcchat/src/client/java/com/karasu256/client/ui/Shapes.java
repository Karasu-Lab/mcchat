package com.karasu256.client.ui;

import com.karasu256.ChatNative;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class Shapes {
  private static ShaderInstance shader;

  private Shapes() {}

  public static void register() {
    CoreShaderRegistrationCallback.EVENT.register(context ->
        context.register(
            new ResourceLocation(ChatNative.MOD_ID, "rounded"),
            DefaultVertexFormat.POSITION_TEX_COLOR,
            loaded -> shader = loaded));
  }

  static void roundRect(GuiGraphics g, int x0, int y0, int x1, int y1, int radius, int argb) {
    int r = Math.max(0, Math.min(radius, Math.min((x1 - x0) / 2, (y1 - y0) / 2)));
    draw(g, 0f, x0, y0, x1, y1, r, argb, null, 0f, 0f, 0f, 0f);
  }

  static void face(
      GuiGraphics g,
      ResourceLocation texture,
      int x,
      int y,
      int size,
      float[] skin) {
    draw(
        g,
        1f,
        x,
        y,
        x + size,
        y + size,
        size / 2f,
        0xFFFFFFFF,
        texture,
        skin[0],
        skin[1],
        skin[2],
        skin[3]);
  }

  static void replyIcon(GuiGraphics g, int x, int y, int size, int argb) {
    draw(g, 2f, x, y, x + size, y + size, 0f, argb, null, 0f, 0f, 0f, 0f);
  }

  private static void draw(
      GuiGraphics g,
      float mode,
      float x0,
      float y0,
      float x1,
      float y1,
      float radius,
      int argb,
      ResourceLocation texture,
      float skin0,
      float skin1,
      float skin2,
      float skin3) {
    if (shader == null) {
      return;
    }
    float edge = 1f;
    g.flush();
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShader(() -> shader);
    RenderSystem.setShaderTexture(
        0, texture != null ? texture : new ResourceLocation("textures/misc/white.png"));
    shader.safeGetUniform("Size").set(x1 - x0 + 2 * edge, y1 - y0 + 2 * edge);
    shader.safeGetUniform("Pad").set(edge);
    shader.safeGetUniform("Radius").set(radius);
    shader.safeGetUniform("Mode").set(mode);
    shader.safeGetUniform("Skin").set(skin0, skin1, skin2, skin3);

    Matrix4f pose = g.pose().last().pose();
    BufferBuilder buffer = Tesselator.getInstance().getBuilder();
    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    buffer.vertex(pose, x0 - edge, y1 + edge, 0f).uv(0f, 1f).color(argb).endVertex();
    buffer.vertex(pose, x1 + edge, y1 + edge, 0f).uv(1f, 1f).color(argb).endVertex();
    buffer.vertex(pose, x1 + edge, y0 - edge, 0f).uv(1f, 0f).color(argb).endVertex();
    buffer.vertex(pose, x0 - edge, y0 - edge, 0f).uv(0f, 0f).color(argb).endVertex();
    BufferUploader.drawWithShader(buffer.end());
  }
}
