package com.karasu256.capture;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.WorldIcon;
import com.karasu256.mcchat.common.WorldSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public final class ServerWorldSource extends WorldSource {
  private final Path iconFile;

  public ServerWorldSource(MinecraftServer server) {
    super(
        ChatNative.levelId(server),
        server.isDedicatedServer() ? server.getMotd() : server.getWorldData().getLevelName(),
        Origin.SERVER);
    this.iconFile = server.getWorldPath(LevelResource.ICON_FILE);
  }

  @Override
  protected Optional<WorldIcon> icon() {
    if (!Files.isRegularFile(iconFile)) {
      return Optional.empty();
    }
    try {
      return Optional.of(new WorldIcon(Files.readAllBytes(iconFile)));
    } catch (IOException e) {
      ChatNative.LOGGER.warn("Cannot read world icon {}", iconFile, e);
      return Optional.empty();
    }
  }
}
