package com.karasu256.client.capture;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.WorldIcon;
import com.karasu256.mcchat.common.WorldSource;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.multiplayer.ServerData;

public final class RemoteWorldSource extends WorldSource {
  private final ServerData server;

  public RemoteWorldSource(ServerData server) {
    super(
        server == null
            ? ""
            : UUID.nameUUIDFromBytes(server.ip.getBytes(StandardCharsets.UTF_8)).toString(),
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
