package com.karasu256.mcchat.common;

import java.util.Objects;

public final class WorldData {
  private final String id;
  private final String worldName;
  private final WorldContext context;

  public WorldData(String id, String worldName, WorldContext context) {
    this.id = id;
    this.worldName = worldName;
    this.context = context.equals(WorldContext.EMPTY) ? null : context;
  }

  public String id() {
    return id;
  }

  public String worldName() {
    return worldName;
  }

  public WorldContext context() {
    return context == null ? WorldContext.EMPTY : context;
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

  boolean valid() {
    return id != null && worldName != null && context().valid();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorldData
        && id.equals(((WorldData) o).id)
        && worldName.equals(((WorldData) o).worldName)
        && context().equals(((WorldData) o).context());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, worldName, context());
  }
}
