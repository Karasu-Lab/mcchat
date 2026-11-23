package com.karasu256.client.ui;

final class Fade {
  private static float alpha = 1f;

  private Fade() {}

  static float get() {
    return alpha;
  }

  static void set(float value) {
    alpha = value;
  }

  static int tint(int argb) {
    if (alpha >= 1f) {
      return argb;
    }
    int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(alpha, 0f));
    return (a << 24) | (argb & 0xFFFFFF);
  }
}
