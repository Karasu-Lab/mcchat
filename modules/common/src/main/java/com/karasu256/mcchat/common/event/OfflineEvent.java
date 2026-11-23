package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.MinecraftScreen;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;

@EventType("offline")
public class OfflineEvent extends StatusEvent {
  public OfflineEvent(
      EventContext context, Origin origin, PlayerRef player, MinecraftScreen screen) {
    super(context, origin, player, screen);
  }
}
