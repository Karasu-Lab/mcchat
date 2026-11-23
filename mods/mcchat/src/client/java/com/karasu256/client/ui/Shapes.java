package com.karasu256.client.ui;

import com.karasu256.ChatNative;
import com.karasu256.client.mixin.GuiGraphicsExtractorAccessor;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class Shapes {
  private static final float EDGE = 1f;

  private static RenderPipeline rect;
  private static RenderPipeline face;
  private static RenderPipeline icon;

  private Shapes() {}

  public static void register(ChatTheme.Metrics metrics) {
    VertexFormat format =
        VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("UV0", GpuFormat.RG32_FLOAT)
            .addAttribute("UV3", GpuFormat.RG32_FLOAT)
            .addAttribute("LineWidth", GpuFormat.R32_FLOAT)
            .build();
    float hatOffset = (metrics.hatU - metrics.faceU) / (float) metrics.skinSize;
    rect = pipeline("rounded_rect", format, null, null, 0f);
    face = pipeline("rounded_face", format, "FACE", BindGroupLayouts.SAMPLER0, hatOffset);
    icon = pipeline("rounded_icon", format, "ICON", null, 0f);
  }

  private static RenderPipeline pipeline(
      String name,
      VertexFormat format,
      String define,
      com.mojang.renderpearl.api.pipeline.BindGroupLayout samplers,
      float hatOffset) {
    RenderPipeline.Builder builder =
        RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(ChatNative.MOD_ID, "pipeline/" + name))
            .withVertexShader(Identifier.fromNamespaceAndPath(ChatNative.MOD_ID, "core/rounded"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(ChatNative.MOD_ID, "core/rounded"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, format)
            .withPrimitiveTopology(PrimitiveTopology.QUADS);
    if (samplers != null) {
      builder.withBindGroupLayout(samplers);
    }
    if (define != null) {
      builder.withShaderDefine(define);
    }
    builder.withShaderDefine("HAT_DX", hatOffset);
    return builder.build();
  }

  static void roundRect(
      GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int radius, int argb) {
    if (rect == null) {
      return;
    }
    int r = Math.max(0, Math.min(radius, Math.min((x1 - x0) / 2, (y1 - y0) / 2)));
    submit(g, rect, TextureSetup.noTexture(), x0, y0, x1, y1, r, argb, 0f, 0f, 0f);
  }

  static void face(
      GuiGraphicsExtractor g, Identifier texture, int x, int y, int size, float[] skin) {
    if (face == null) {
      return;
    }
    AbstractTexture loaded = Minecraft.getInstance().getTextureManager().getTexture(texture);
    TextureSetup setup = TextureSetup.singleTexture(loaded.getTextureView(), loaded.getSampler());
    submit(
        g, face, setup, x, y, x + size, y + size, size / 2f, 0xFFFFFFFF, skin[0], skin[2], skin[3]);
  }

  static void replyIcon(GuiGraphicsExtractor g, int x, int y, int size, int argb) {
    if (icon == null) {
      return;
    }
    submit(g, icon, TextureSetup.noTexture(), x, y, x + size, y + size, 0f, argb, 0f, 0f, 0f);
  }

  private static void submit(
      GuiGraphicsExtractor g,
      RenderPipeline pipeline,
      TextureSetup textures,
      float x0,
      float y0,
      float x1,
      float y1,
      float radius,
      int argb,
      float faceU,
      float faceV,
      float faceSize) {
    Matrix3x2f pose = new Matrix3x2f(g.pose());
    float scale = (float) Math.sqrt(Math.abs(pose.m00() * pose.m11() - pose.m01() * pose.m10()));
    ScreenRectangle bounds =
        new ScreenRectangle(
                Math.round(x0 - EDGE), Math.round(y0 - EDGE),
                Math.round(x1 - x0 + 2 * EDGE), Math.round(y1 - y0 + 2 * EDGE))
            .transformMaxBounds(pose);
    ((GuiGraphicsExtractorAccessor) g)
        .chatnative$guiRenderState()
        .addGuiElement(
            new Rounded(
                pipeline,
                textures,
                pose,
                x0,
                y0,
                x1,
                y1,
                radius * scale,
                Fade.tint(argb),
                faceU,
                faceV,
                faceSize,
                bounds));
  }

  private record Rounded(
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      Matrix3x2fc pose,
      float x0,
      float y0,
      float x1,
      float y1,
      float radius,
      int argb,
      float faceU,
      float faceV,
      float faceSize,
      ScreenRectangle bounds)
      implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer consumer) {
      corner(consumer, x0 - EDGE, y1 + EDGE, 0f, 1f);
      corner(consumer, x1 + EDGE, y1 + EDGE, 1f, 1f);
      corner(consumer, x1 + EDGE, y0 - EDGE, 1f, 0f);
      corner(consumer, x0 - EDGE, y0 - EDGE, 0f, 0f);
    }

    private void corner(VertexConsumer consumer, float x, float y, float cornerX, float cornerY) {
      float unitX = (x - x0) / (x1 - x0);
      float unitY = (y - y0) / (y1 - y0);
      consumer
          .addVertexWith2DPose(pose, x, y)
          .setColor(argb)
          .setUv(unitX, unitY)
          .setUv3(faceU + unitX * faceSize, faceV + unitY * faceSize)
          .setLineWidth(radius);
    }

    @Override
    public ScreenRectangle scissorArea() {
      return null;
    }
  }
}
