package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;

@EventType("typing_started")
public class TypingStartedEvent extends TypingEvent {
  public TypingStartedEvent(EventContext context, Origin origin, PlayerRef player) {
    super(context, origin, player);
  }

  @Override
  public TypingEvent relayedAs(EventContext context, Origin origin) {
    return new TypingStartedEvent(context, origin, player().orElse(null));
  }
}
