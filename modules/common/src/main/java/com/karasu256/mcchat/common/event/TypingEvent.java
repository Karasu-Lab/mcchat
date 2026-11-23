package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;

public abstract class TypingEvent extends CapturedEvent {
  protected TypingEvent(EventContext context, Origin origin, PlayerRef player) {
    super(context, origin, player);
  }

  public abstract TypingEvent relayedAs(EventContext context, Origin origin);

  @Override
  public WorldContext applyTo(WorldContext world, int historySize) {
    return world;
  }
}
