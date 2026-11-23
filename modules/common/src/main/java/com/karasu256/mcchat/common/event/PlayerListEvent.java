package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@EventType("player_list")
public class PlayerListEvent extends CapturedEvent {
  private final List<PlayerRef> players;

  public PlayerListEvent(EventContext context, Origin origin, List<PlayerRef> players) {
    super(context, origin, null);
    this.players =
        players.isEmpty() ? null : Collections.unmodifiableList(new ArrayList<>(players));
  }

  private PlayerListEvent(
      EventContext context, UUID id, long timestampMillis, Origin origin, List<PlayerRef> players) {
    super(context, id, timestampMillis, origin, null);
    this.players = players.isEmpty() ? null : players;
  }

  public List<PlayerRef> players() {
    return players == null ? Collections.emptyList() : players;
  }

  @Override
  public List<PlayerRef> referencedPlayers() {
    return players();
  }

  @Override
  public CapturedEvent mapPlayers(UnaryOperator<PlayerRef> mapper) {
    return new PlayerListEvent(
        context(),
        id(),
        timestampMillis(),
        origin(),
        players().stream().map(mapper).collect(Collectors.toList()));
  }

  @Override
  public WorldContext applyTo(WorldContext world, int historySize) {
    return world.withPlayers(players());
  }

  @Override
  public boolean valid() {
    return super.valid() && players().stream().allMatch(p -> p != null && p.valid());
  }
}
