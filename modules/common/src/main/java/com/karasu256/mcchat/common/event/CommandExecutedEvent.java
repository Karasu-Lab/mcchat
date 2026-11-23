package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;

@EventType("command_executed")
public class CommandExecutedEvent extends MessageEvent {
  public CommandExecutedEvent(
      EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }
}
