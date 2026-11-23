package com.karasu256.client.event;

import com.karasu256.client.chat.ClientChatTextProcessor;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.event.ChatSentEvent;
import com.karasu256.mcchat.common.event.EventContext;

public final class ClientChatSentEvent extends ChatSentEvent {
  public ClientChatSentEvent(
      EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }

  @Override
  public void handle() {
    if (origin() == Origin.CLIENT) {
      ClientChatTextProcessor.send(text());
    } else {
      super.handle();
    }
  }
}
