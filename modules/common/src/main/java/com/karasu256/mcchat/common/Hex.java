package com.karasu256.mcchat.common;

final class Hex {
  private Hex() {}

  static String encode(byte[] bytes) {
    StringBuilder out = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      out.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
    }
    return out.toString();
  }

  static byte[] decode(String text) {
    if (text.length() % 2 != 0) {
      throw new IllegalArgumentException("Odd hex length");
    }
    byte[] out = new byte[text.length() / 2];
    for (int i = 0; i < out.length; i++) {
      int high = Character.digit(text.charAt(i * 2), 16);
      int low = Character.digit(text.charAt(i * 2 + 1), 16);
      if (high < 0 || low < 0) {
        throw new IllegalArgumentException("Invalid hex digit");
      }
      out[i] = (byte) (high << 4 | low);
    }
    return out;
  }
}
