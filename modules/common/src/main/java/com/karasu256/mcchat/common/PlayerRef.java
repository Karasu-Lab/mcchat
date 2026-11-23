package com.karasu256.mcchat.common;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerRef {
  private final UUID uuid;
  private final String name;
  private final Optional<Skin> skin;

  public PlayerRef(UUID uuid, String name, Optional<Skin> skin) {
    this.uuid = uuid;
    this.name = name;
    this.skin = skin;
  }

  public UUID uuid() {
    return uuid;
  }

  public String name() {
    return name;
  }

  public Optional<Skin> skin() {
    return skin;
  }

  public PlayerRef withoutSkin() {
    return new PlayerRef(uuid, name, Optional.empty());
  }

  JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("uuid", uuid.toString());
    json.addProperty("name", name);
    skin.ifPresent(s -> json.add("skin", s.toJson()));
    return json;
  }

  static PlayerRef fromJson(JsonObject json) {
    Optional<Skin> skin =
        json.has("skin")
            ? Optional.of(Skin.fromJson(json.getAsJsonObject("skin")))
            : Optional.empty();
    return new PlayerRef(
        UUID.fromString(json.get("uuid").getAsString()), json.get("name").getAsString(), skin);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PlayerRef
        && uuid.equals(((PlayerRef) o).uuid)
        && name.equals(((PlayerRef) o).name)
        && skin.equals(((PlayerRef) o).skin);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, name, skin);
  }

  public static final class Skin {
    private final byte[] png;

    public Skin(byte[] png) {
      this.png = png;
    }

    public byte[] png() {
      return png;
    }

    JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.addProperty("png", Hex.encode(png));
      return json;
    }

    static Skin fromJson(JsonObject json) {
      return new Skin(Hex.decode(json.get("png").getAsString()));
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
