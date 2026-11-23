package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;

@EventType("chat_received")
public class ChatReceivedEvent extends MessageEvent {
  public ChatReceivedEvent(EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }

  public ChatReceivedEvent(EventContext context, Origin origin, PlayerRef player, ChatText text) {
    super(context, origin, player, text);
  }
}
