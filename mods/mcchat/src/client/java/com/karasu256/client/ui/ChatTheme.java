package com.karasu256.client.ui;

public final class ChatTheme {
  public final Colors colors;
  public final Metrics metrics;

  public ChatTheme(Colors colors, Metrics metrics) {
    this.colors = colors;
    this.metrics = metrics;
  }

  public static ChatTheme dark() {
    return new ChatTheme(new Colors(), new Metrics(0.5f, 0.75f));
  }

  public static final class Colors {
    public final int surface = 0xFF141315;
    public final int surfaceContainer = 0xFF201F21;
    public final int surfaceContainerHigh = 0xFF2B292B;
    public final int surfaceContainerHighest = 0xFF363437;
    public final int primary = 0xFFFFAFD3;
    public final int primaryContainer = 0xFF7F2652;
    public final int onPrimaryContainer = 0xFFFFD8E9;
    public final int secondaryContainer = 0xFF004C6A;
    public final int onSecondaryContainer = 0xFFC5E8FF;
    public final int onSurface = 0xFFE6E3E6;
    public final int onSurfaceVariant = 0xFFC9C5CA;
    public final int outlineVariant = 0xFF484649;
  }

  public static final class Metrics {
    public final float small;
    public final int listPadH;
    public final int itemGap;
    public final int avatar;
    public final int avatarGap;
    public final int bubblePadH;
    public final int bubblePadV;
    public final int corner;
    public final int timeGap;
    public final int inset;
    public final int quotePadH;
    public final int quotePadV;
    public final int quoteGap;
    public final int quoteColumnGap;
    public final int previewLimit;
    public final int dividerPadV;
    public final int dividerGap;
    public final int dot;
    public final int dotGap;
    public final int typingPad;
    public final int stackStep;
    public final int replyButton;
    public final int replyRowHeight;
    public final int replyGap;
    public final int fadeMillis;
    public final int vanillaInputBar;
    public final int vanillaInputSide;
    public final int vanillaChatBottom;
    public final int vanillaChatLeft;
    public final int scrollStep;
    public final float maxWidthRatio;
    public final int iconTextureSize;
    public final int typingCycleMillis;
    public final float typingStagger;
    public final int skinSize;
    public final int faceTexels;
    public final int faceU;
    public final int faceV;
    public final int hatU;

    public Metrics(float unitsPerDp, float small) {
      this.small = small;
      vanillaInputBar = 14;
      vanillaInputSide = 2;
      vanillaChatBottom = 40;
      vanillaChatLeft = 4;
      scrollStep = 18;
      maxWidthRatio = 0.5f;
      iconTextureSize = 64;
      typingCycleMillis = 1200;
      typingStagger = 0.15f;
      skinSize = 64;
      faceTexels = 8;
      faceU = 8;
      faceV = 8;
      hatU = 40;
      listPadH = dp(unitsPerDp, 16);
      itemGap = dp(unitsPerDp, 8);
      avatar = dp(unitsPerDp, 32);
      avatarGap = dp(unitsPerDp, 8);
      bubblePadH = dp(unitsPerDp, 16);
      bubblePadV = dp(unitsPerDp, 8);
      corner = dp(unitsPerDp, 16);
      timeGap = dp(unitsPerDp, 8);
      inset = dp(unitsPerDp, 40);
      quotePadH = dp(unitsPerDp, 16);
      quotePadV = dp(unitsPerDp, 8);
      quoteGap = dp(unitsPerDp, 8);
      quoteColumnGap = dp(unitsPerDp, 4);
      previewLimit = 20;
      dividerPadV = dp(unitsPerDp, 8);
      dividerGap = dp(unitsPerDp, 12);
      dot = dp(unitsPerDp, 6);
      dotGap = dp(unitsPerDp, 4);
      typingPad = dp(unitsPerDp, 16);
      stackStep = dp(unitsPerDp, 16);
      replyButton = dp(unitsPerDp, 40);
      replyRowHeight = replyButton;
      replyGap = dp(unitsPerDp, 4);
      fadeMillis = 150;
    }

    private static int dp(float unitsPerDp, int value) {
      return Math.round(value * unitsPerDp);
    }
  }
}
