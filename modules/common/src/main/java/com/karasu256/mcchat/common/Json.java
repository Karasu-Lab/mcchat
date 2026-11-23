package com.karasu256.mcchat.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.EventRegistry;
import java.io.IOException;
import java.util.Locale;

public final class Json {
  private Json() {}

  public static Gson create(EventRegistry registry) {
    return new GsonBuilder()
        .registerTypeAdapterFactory(new LowercaseEnumFactory())
        .registerTypeHierarchyAdapter(MinecraftScreen.class, new ScreenAdapter())
        .registerTypeAdapterFactory(new EventAdapterFactory(registry))
        .create();
  }

  private static final class EventAdapterFactory implements TypeAdapterFactory {
    private final EventRegistry registry;

    EventAdapterFactory(EventRegistry registry) {
      this.registry = registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (!CapturedEvent.class.isAssignableFrom(type.getRawType())) {
        return null;
      }
      TypeAdapterFactory self = this;
      TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
      return new TypeAdapter<T>() {
        @Override
        public void write(JsonWriter out, T value) throws IOException {
          if (value == null) {
            out.nullValue();
            return;
          }
          TypeAdapter<T> delegate =
              gson.getDelegateAdapter(self, TypeToken.get((Class<T>) value.getClass()));
          JsonObject tree = delegate.toJsonTree(value).getAsJsonObject();
          tree.addProperty("kind", ((CapturedEvent) value).type());
          elements.write(out, tree);
        }

        @Override
        public T read(JsonReader in) throws IOException {
          if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
          }
          JsonObject tree = elements.read(in).getAsJsonObject();
          Class<? extends CapturedEvent> eventClass =
              registry.classOf(tree.get("kind").getAsString());
          if (eventClass == null) {
            throw new JsonParseException("Unknown event kind");
          }
          return (T) gson.getDelegateAdapter(self, TypeToken.get(eventClass)).fromJsonTree(tree);
        }
      };
    }
  }

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
