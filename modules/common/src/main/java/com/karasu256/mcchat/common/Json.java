package com.karasu256.mcchat.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Locale;

final class Json {
  static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapterFactory(new LowercaseEnumFactory())
          .registerTypeHierarchyAdapter(MinecraftScreen.class, new ScreenAdapter())
          .create();

  private Json() {}

  private static final class ScreenAdapter extends TypeAdapter<MinecraftScreen> {
    @Override
    public void write(JsonWriter out, MinecraftScreen value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else {
        out.value(value.name());
      }
    }

    @Override
    public MinecraftScreen read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return MinecraftScreen.of(in.nextString());
    }
  }

  private static final class LowercaseEnumFactory implements TypeAdapterFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      Class<? super T> raw = type.getRawType();
      if (!raw.isEnum()) {
        return null;
      }
      Object[] constants = raw.getEnumConstants();
      return (TypeAdapter<T>)
          new TypeAdapter<Object>() {
            @Override
            public void write(JsonWriter out, Object value) throws IOException {
              if (value == null) {
                out.nullValue();
              } else {
                out.value(((Enum<?>) value).name().toLowerCase(Locale.ROOT));
              }
            }

            @Override
            public Object read(JsonReader in) throws IOException {
              if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
              }
              String name = in.nextString();
              for (Object constant : constants) {
                if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
                  return constant;
                }
              }
              return null;
            }
          };
    }
  }

  static final class HexAdapter extends TypeAdapter<byte[]> {
    @Override
    public void write(JsonWriter out, byte[] value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else {
        out.value(Hex.encode(value));
      }
    }

    @Override
    public byte[] read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return Hex.decode(in.nextString());
    }
  }
}
