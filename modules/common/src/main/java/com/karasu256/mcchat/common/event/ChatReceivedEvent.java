package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;

@EventType("chat_received")
public class ChatReceivedEvent extends MessageEvent {
  public ChatReceivedEvent(EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }
}
