package com.karasu256.capture;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public record CapturedPacket(
    Type type, Origin authority, WorldData world, Optional<CapturedData> data) {

  public enum Type {
    SNAPSHOT,
    RESET,
    EVENT;

    public static final Codec<Type> CODEC = Origin.enumCodec(Type.class);
  }

  public static final Codec<CapturedPacket> CODEC =
      RecordCodecBuilder.create(
          i ->
              i.group(
                      Type.CODEC.fieldOf("type").forGetter(CapturedPacket::type),
                      Origin.CODEC.fieldOf("authority").forGetter(CapturedPacket::authority),
                      WorldData.CODEC.fieldOf("world").forGetter(CapturedPacket::world),
                      CapturedData.CODEC.optionalFieldOf("data").forGetter(CapturedPacket::data))
                  .apply(i, CapturedPacket::new));

  public static CapturedPacket event(Origin authority, WorldData world, CapturedData data) {
    return new CapturedPacket(Type.EVENT, authority, world.withoutContext(), Optional.of(data));
  }

  public String encode() {
    return CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow().toString();
  }

  public static Optional<CapturedPacket> decode(String line) {
    try {
      JsonElement json = JsonParser.parseString(line);
      return CODEC.parse(JsonOps.INSTANCE, json).result();
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }
}
