package com.karasu256.mcchat.common;

import java.util.Objects;

public class MinecraftScreen {
  private final String name;

  public MinecraftScreen(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  public static MinecraftScreen of(String name) {
    MinecraftScreen chat = new MinecraftChatScreen();
    return chat.name().equals(name) ? chat : new MinecraftScreen(name);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof MinecraftScreen && name.equals(((MinecraftScreen) o).name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
