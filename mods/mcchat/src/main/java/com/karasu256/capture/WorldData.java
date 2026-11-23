package com.karasu256.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WorldData(String host, String worldName, WorldContext context) {
  public static final Codec<WorldData> CODEC =
      RecordCodecBuilder.create(i ->
          i.group(
                  Codec.STRING.fieldOf("host").forGetter(WorldData::host),
                  Codec.STRING.fieldOf("worldName").forGetter(WorldData::worldName),
                  WorldContext.CODEC
                      .optionalFieldOf("context", WorldContext.EMPTY)
                      .forGetter(WorldData::context))
              .apply(i, WorldData::new));

  public static WorldData empty(String host, String worldName) {
    return new WorldData(host, worldName, WorldContext.EMPTY);
  }

  public WorldData withContext(WorldContext next) {
    return new WorldData(host, worldName, next);
  }

  public WorldData withoutContext() {
    return withContext(WorldContext.EMPTY);
  }
}
