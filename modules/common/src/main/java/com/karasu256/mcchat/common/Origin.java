package com.karasu256.mcchat.common;

import java.util.Locale;

public enum Origin {
  SERVER,
  CLIENT;

  public String wireName() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static Origin fromWireName(String value) {
    return valueOf(value.toUpperCase(Locale.ROOT));
  }
}
