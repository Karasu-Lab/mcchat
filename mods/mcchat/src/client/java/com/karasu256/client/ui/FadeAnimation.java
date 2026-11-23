package com.karasu256.client.ui;

final class FadeAnimation implements Animation {
  private final long durationMillis;
  private float from;
  private float to;
  private long startedAt;
  private float outer = 1f;

  FadeAnimation(long durationMillis, float initial) {
    this.durationMillis = durationMillis;
    this.from = initial;
    this.to = initial;
  }

  void target(float value) {
    if (value == to) {
      return;
    }
    from = alpha();
    to = value;
    startedAt = System.currentTimeMillis();
  }

  float alpha() {
    long elapsed = System.currentTimeMillis() - startedAt;
    if (elapsed >= durationMillis) {
      return to;
    }
    return from + (to - from) * elapsed / durationMillis;
  }

  @Override
  public void render() {
    outer = Fade.get();
    Fade.set(outer * alpha());
  }

  @Override
  public void postRender() {
    Fade.set(outer);
  }
}
