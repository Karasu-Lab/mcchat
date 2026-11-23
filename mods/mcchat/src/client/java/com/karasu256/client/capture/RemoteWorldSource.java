package com.karasu256.client.capture;

import com.google.common.net.HostAndPort;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.WorldIcon;
import com.karasu256.mcchat.common.WorldSource;
import java.util.Optional;
import net.minecraft.client.multiplayer.ServerData;

public final class RemoteWorldSource extends WorldSource {
  private final ServerData server;

  public RemoteWorldSource(ServerData server) {
    super(
        server == null ? "" : HostAndPort.fromString(server.ip).getHost(),
        server == null ? "" : server.name,
        Origin.CLIENT);
    this.server = server;
  }

  @Override
  protected Optional<WorldIcon> icon() {
    return server == null
        ? Optional.empty()
        : Optional.ofNullable(server.getIconBytes()).map(WorldIcon::new);
  }
}
