package com.karasu256.client.event;

import com.karasu256.client.capture.ClientCapture;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.event.EventContext;
import com.karasu256.mcchat.common.event.TypingStartedEvent;
import net.minecraft.client.Minecraft;

public final class ClientTypingStartedEvent extends TypingStartedEvent {
  public ClientTypingStartedEvent(EventContext context, Origin origin, PlayerRef player) {
    super(context, origin, player);
  }

  @Override
  public void handle() {
    Minecraft client = Minecraft.getInstance();
    if (origin() == Origin.CLIENT) {
      if (client.player != null && client.getConnection() != null) {
        client.execute(() -> ClientCapture.relayTyping(true));
      }
    } else if (!isLocalPlayer(client)) {
      super.handle();
    }
  }

  private boolean isLocalPlayer(Minecraft client) {
    return client.player != null
        && player().filter(p -> p.uuid().equals(client.player.getUUID())).isPresent();
  }
}
