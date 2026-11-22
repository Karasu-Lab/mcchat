package com.karasu256.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum Origin {
  SERVER,
  CLIENT;

  public static final Codec<Origin> CODEC = enumCodec(Origin.class);

  static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
    return Codec.STRING.comapFlatMap(
        name -> {
          try {
            return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
          } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
          }
        },
        value -> value.name().toLowerCase(Locale.ROOT));
  }
}
