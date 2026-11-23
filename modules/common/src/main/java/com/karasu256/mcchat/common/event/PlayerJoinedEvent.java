package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

@EventType("player_joined")
public class PlayerJoinedEvent extends CapturedEvent {
  public PlayerJoinedEvent(EventContext context, Origin origin, PlayerRef player) {
    super(context, origin, player);
  }

  private PlayerJoinedEvent(
      EventContext context, UUID id, long timestampMillis, Origin origin, PlayerRef player) {
    super(context, id, timestampMillis, origin, player);
  }

  @Override
  public List<PlayerRef> referencedPlayers() {
    return player().map(Collections::singletonList).orElse(Collections.emptyList());
  }

  @Override
  public CapturedEvent mapPlayers(UnaryOperator<PlayerRef> mapper) {
    return new PlayerJoinedEvent(
        context(), id(), timestampMillis(), origin(), player().map(mapper).orElse(null));
  }

  @Override
  public WorldContext applyTo(WorldContext world, int historySize) {
    if (!player().isPresent()) {
      return world;
    }
    PlayerRef joined = player().get();
    List<PlayerRef> next = new ArrayList<>(world.players());
    next.removeIf(p -> p.uuid().equals(joined.uuid()));
    next.add(joined);
    return world.withPlayers(Collections.unmodifiableList(next));
  }
}
