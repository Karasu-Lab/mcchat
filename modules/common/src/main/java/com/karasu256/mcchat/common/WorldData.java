package com.karasu256.mcchat.common;

import com.google.gson.JsonObject;
import java.util.Objects;

public final class WorldData {
  private final String host;
  private final String worldName;
  private final WorldContext context;

  public WorldData(String host, String worldName, WorldContext context) {
    this.host = host;
    this.worldName = worldName;
    this.context = context;
  }

  public String host() {
    return host;
  }

  public String worldName() {
    return worldName;
  }

  public WorldContext context() {
    return context;
  }

  public static WorldData empty(String host, String worldName) {
    return new WorldData(host, worldName, WorldContext.EMPTY);
  }

  public WorldData withContext(WorldContext next) {
    return new WorldData(host, worldName, next);
  }

  public WorldData withoutContext() {
    return withContext(WorldContext.EMPTY);
  }

  JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("host", host);
    json.addProperty("worldName", worldName);
    if (!context.equals(WorldContext.EMPTY)) {
      json.add("context", context.toJson());
    }
    return json;
  }

  static WorldData fromJson(JsonObject json) {
    return new WorldData(
        json.get("host").getAsString(),
        json.get("worldName").getAsString(),
        json.has("context")
            ? WorldContext.fromJson(json.getAsJsonObject("context"))
            : WorldContext.EMPTY);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorldData
        && host.equals(((WorldData) o).host)
        && worldName.equals(((WorldData) o).worldName)
        && context.equals(((WorldData) o).context);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, worldName, context);
  }
}
