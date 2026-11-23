package com.karasu256.client.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.event.CommandExecutedEvent;
import com.karasu256.mcchat.common.event.EventContext;

public final class ClientCommandExecutedEvent extends CommandExecutedEvent {
  public ClientCommandExecutedEvent(
      EventContext context, Origin origin, PlayerRef player, String content) {
    super(context, origin, player, content);
  }

  @Override
  public void handle() {}
}
