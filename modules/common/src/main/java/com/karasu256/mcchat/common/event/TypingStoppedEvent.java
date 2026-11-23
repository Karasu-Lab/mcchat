package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;

@EventType("typing_stopped")
public class TypingStoppedEvent extends TypingEvent {
  public TypingStoppedEvent(EventContext context, Origin origin, PlayerRef player) {
    super(context, origin, player);
  }

  @Override
  public TypingEvent relayedAs(EventContext context, Origin origin) {
    return new TypingStoppedEvent(context, origin, player().orElse(null));
  }
}
