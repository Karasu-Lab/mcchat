package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.WorldContext;

@EventType("world_changed")
public class WorldChangedEvent extends MessageEvent {
  public WorldChangedEvent(EventContext context, Origin origin, String worldName) {
    super(context, origin, null, worldName);
  }

  @Override
  public WorldContext applyTo(WorldContext world, int historySize) {
    return world;
  }
}
