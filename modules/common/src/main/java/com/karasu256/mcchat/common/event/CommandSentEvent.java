package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;

@EventType("command_sent")
public class CommandSentEvent extends MessageEvent {
  public CommandSentEvent(EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }

  public CommandSentEvent(EventContext context, Origin origin, PlayerRef player, ChatText text) {
    super(context, origin, player, text);
  }
}
