package com.karasu256.mcchat.common;

import com.google.gson.JsonObject;
import java.util.Objects;

public final class WorldData {
  private final String id;
  private final String worldName;
  private final WorldContext context;

  public WorldData(String id, String worldName, WorldContext context) {
    this.id = id;
    this.worldName = worldName;
    this.context = context;
  }

  public String id() {
    return id;
  }

  public String worldName() {
    return worldName;
  }

  public WorldContext context() {
    return context;
  }

  public static WorldData empty(String id, String worldName) {
    return new WorldData(id, worldName, WorldContext.EMPTY);
  }

  public WorldData withContext(WorldContext next) {
    return new WorldData(id, worldName, next);
  }

  public WorldData withoutContext() {
    return withContext(WorldContext.EMPTY);
  }

  JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("id", id);
    json.addProperty("worldName", worldName);
    if (!context.equals(WorldContext.EMPTY)) {
      json.add("context", context.toJson());
    }
    return json;
  }

  static WorldData fromJson(JsonObject json) {
    return new WorldData(
        json.get("id").getAsString(),
        json.get("worldName").getAsString(),
        json.has("context")
            ? WorldContext.fromJson(json.getAsJsonObject("context"))
            : WorldContext.EMPTY);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorldData
        && id.equals(((WorldData) o).id)
        && worldName.equals(((WorldData) o).worldName)
        && context.equals(((WorldData) o).context);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, worldName, context);
  }
}
