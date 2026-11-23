package com.karasu256.mcchat.common;

import com.google.gson.annotations.JsonAdapter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerRef {
  private final UUID uuid;
  private final String name;
  private final Skin skin;

  public PlayerRef(UUID uuid, String name, Optional<Skin> skin) {
    this.uuid = uuid;
    this.name = name;
    this.skin = skin.orElse(null);
  }

  public UUID uuid() {
    return uuid;
  }

  public String name() {
    return name;
  }

  public Optional<Skin> skin() {
    return Optional.ofNullable(skin);
  }

  public PlayerRef withoutSkin() {
    return new PlayerRef(uuid, name, Optional.empty());
  }

  public boolean valid() {
    return uuid != null && name != null && (skin == null || skin.png != null);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PlayerRef
        && uuid.equals(((PlayerRef) o).uuid)
        && name.equals(((PlayerRef) o).name)
        && skin().equals(((PlayerRef) o).skin());
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, name, skin());
  }

  public static final class Skin {
    @JsonAdapter(Json.HexAdapter.class)
    private final byte[] png;

    public Skin(byte[] png) {
      this.png = png;
    }

    public byte[] png() {
      return png;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Skin && Arrays.equals(png, ((Skin) o).png);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(png);
    }
  }
}
