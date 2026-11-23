package com.karasu256.client.ui;

import net.minecraft.client.gui.GuiGraphics;

interface Animation {
  void render(GuiGraphics graphics);

  void postRender(GuiGraphics graphics);
}
