package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;

@EventType("command_sent")
public class CommandSentEvent extends MessageEvent {
  public CommandSentEvent(EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }
}
