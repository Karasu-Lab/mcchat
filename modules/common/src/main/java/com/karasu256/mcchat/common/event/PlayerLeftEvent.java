package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

@EventType("player_left")
public class PlayerLeftEvent extends CapturedEvent {
  public PlayerLeftEvent(EventContext context, Origin origin, PlayerRef player) {
    super(context, origin, player);
  }

  private PlayerLeftEvent(
      EventContext context, UUID id, long timestampMillis, Origin origin, PlayerRef player) {
    super(context, id, timestampMillis, origin, player);
  }

  @Override
  public List<PlayerRef> referencedPlayers() {
    return player().map(Collections::singletonList).orElse(Collections.emptyList());
  }

  @Override
  public CapturedEvent mapPlayers(UnaryOperator<PlayerRef> mapper) {
    return new PlayerLeftEvent(
        context(), id(), timestampMillis(), origin(), player().map(mapper).orElse(null));
  }

  @Override
  public WorldContext applyTo(WorldContext world, int historySize) {
    if (!player().isPresent()) {
      return world;
    }
    PlayerRef left = player().get();
    List<PlayerRef> next = new ArrayList<>(world.players());
    next.removeIf(p -> p.uuid().equals(left.uuid()));
    return world.withPlayers(Collections.unmodifiableList(next));
  }
}
