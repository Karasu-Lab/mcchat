package com.karasu256.client.event;

import com.karasu256.client.capture.ClientCapture;
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
      ClientCapture.sendChat(text());
    } else {
      super.handle();
    }
  }
}
