package com.karasu256.mcchat.common;

import com.google.gson.annotations.JsonAdapter;
import java.util.Arrays;

public final class WorldIcon {
  @JsonAdapter(Json.HexAdapter.class)
  private final byte[] png;

  public WorldIcon(byte[] png) {
    this.png = png;
  }

  public byte[] png() {
    return png;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorldIcon && Arrays.equals(png, ((WorldIcon) o).png);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(png);
  }
}
