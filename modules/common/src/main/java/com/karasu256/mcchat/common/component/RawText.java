package com.karasu256.mcchat.common.component;

import java.util.Objects;

public final class RawText extends ChatTextComponent {
  private final String text;

  public RawText(String text) {
    this.text = text;
  }

  public String text() {
    return text == null ? "" : text;
  }

  public boolean isEmpty() {
    return text().isEmpty();
  }

  public RawText plus(RawText other) {
    return new RawText(text() + other.text());
  }

  public RawText slice(int start, int end) {
    return new RawText(text().substring(start, end));
  }

  @Override
  public String type() {
    return "raw";
  }

  @Override
  public String plain() {
    return text();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof RawText && text().equals(((RawText) o).text());
  }

  @Override
  public int hashCode() {
    return Objects.hash(text());
  }
}
