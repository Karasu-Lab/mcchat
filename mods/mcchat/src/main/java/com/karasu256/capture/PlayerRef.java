package com.karasu256.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;

public record PlayerRef(UUID uuid, String name, Optional<Skin> skin) {
  public static final Codec<UUID> UUID_CODEC =
      Codec.STRING.xmap(UUID::fromString, UUID::toString);

  public static final Codec<PlayerRef> CODEC =
      RecordCodecBuilder.create(
          i ->
              i.group(
                      UUID_CODEC.fieldOf("uuid").forGetter(PlayerRef::uuid),
                      Codec.STRING.fieldOf("name").forGetter(PlayerRef::name),
                      Skin.CODEC.optionalFieldOf("skin").forGetter(PlayerRef::skin))
                  .apply(i, PlayerRef::new));

  public record Skin(String value, Optional<String> signature) {
    public static final Codec<Skin> CODEC =
        RecordCodecBuilder.create(
            i ->
                i.group(
                        Codec.STRING.fieldOf("value").forGetter(Skin::value),
                        Codec.STRING.optionalFieldOf("signature").forGetter(Skin::signature))
                    .apply(i, Skin::new));
  }
}
