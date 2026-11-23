package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import java.util.Optional;

public abstract class MessageEvent extends CapturedEvent {
  private final String content;

  protected MessageEvent(EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player);
    this.content = content;
  }

  public Optional<String> content() {
    return Optional.ofNullable(content);
  }
}
