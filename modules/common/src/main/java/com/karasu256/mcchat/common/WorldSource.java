package com.karasu256.mcchat.common;

import java.util.Collections;
import java.util.Optional;

public class WorldSource {
  private final String id;
  private final String worldName;
  private final Origin origin;

  public WorldSource(String id, String worldName, Origin origin) {
    this.id = id;
    this.worldName = worldName;
    this.origin = origin;
  }

  public Origin origin() {
    return origin;
  }

  protected Optional<WorldIcon> icon() {
    return Optional.empty();
  }

  public final WorldData world() {
    return new WorldData(
        id,
        worldName,
        new WorldContext(Collections.emptyList(), Collections.emptyList(), icon().orElse(null)));
  }
}
