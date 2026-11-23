package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.MinecraftScreen;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;
import java.util.Optional;

public abstract class StatusEvent extends CapturedEvent {
  private final MinecraftScreen screen;

  protected StatusEvent(
      EventContext context, Origin origin, PlayerRef player, MinecraftScreen screen) {
    super(context, origin, player);
    this.screen = screen;
  }

  public Optional<MinecraftScreen> screen() {
    return Optional.ofNullable(screen);
  }

  @Override
  public WorldContext applyTo(WorldContext world, int historySize) {
    return world;
  }

  @Override
  public boolean valid() {
    return super.valid() && (screen == null || screen.name() != null);
  }
}
