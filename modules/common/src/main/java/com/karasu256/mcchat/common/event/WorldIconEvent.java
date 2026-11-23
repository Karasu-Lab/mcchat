package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.WorldContext;
import com.karasu256.mcchat.common.WorldIcon;
import java.util.Optional;

@EventType("world_icon")
public class WorldIconEvent extends CapturedEvent {
  private final WorldIcon icon;

  public WorldIconEvent(EventContext context, Origin origin, WorldIcon icon) {
    super(context, origin, null);
    this.icon = icon;
  }

  public Optional<WorldIcon> icon() {
    return Optional.ofNullable(icon);
  }

  @Override
  public WorldContext applyTo(WorldContext world, int historySize) {
    return world.withIcon(icon);
  }

  @Override
  public boolean valid() {
    return super.valid() && (icon == null || icon.png() != null);
  }
}
