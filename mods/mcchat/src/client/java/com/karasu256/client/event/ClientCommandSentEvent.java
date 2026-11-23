package com.karasu256.client.event;

import com.karasu256.client.capture.ClientCapture;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.event.CommandSentEvent;
import com.karasu256.mcchat.common.event.EventContext;

public final class ClientCommandSentEvent extends CommandSentEvent {
  public ClientCommandSentEvent(
      EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }

  @Override
  public void handle() {
    if (origin() == Origin.CLIENT) {
      ClientCapture.sendCommand(content().orElse(""));
    } else {
      super.handle();
    }
  }
}
