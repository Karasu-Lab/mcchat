package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;

@EventType("chat_sent")
public class ChatSentEvent extends MessageEvent {
  public ChatSentEvent(EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }
}
