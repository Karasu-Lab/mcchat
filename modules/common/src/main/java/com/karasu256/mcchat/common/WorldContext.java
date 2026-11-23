package com.karasu256.mcchat.common;

import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.EventContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class WorldContext {
  public static final WorldContext EMPTY =
      new WorldContext(Collections.emptyList(), Collections.emptyList());

  private final List<PlayerRef> players;
  private final List<CapturedEvent> history;
  private final WorldIcon icon;

  public WorldContext(List<PlayerRef> players, List<CapturedEvent> history) {
    this(players, history, null);
  }

  public WorldContext(List<PlayerRef> players, List<CapturedEvent> history, WorldIcon icon) {
    this.players = players.isEmpty() ? null : players;
    this.history = history.isEmpty() ? null : history;
    this.icon = icon;
  }

  public List<PlayerRef> players() {
    return players == null ? Collections.emptyList() : players;
  }

  public List<CapturedEvent> history() {
    return history == null ? Collections.emptyList() : history;
  }

  public Optional<WorldIcon> icon() {
    return Optional.ofNullable(icon);
  }

  public WorldContext withIcon(WorldIcon next) {
    return new WorldContext(players(), history(), next);
  }

  public WorldContext withPlayers(List<PlayerRef> next) {
    return new WorldContext(next, history(), icon);
  }

  public WorldContext without(Class<? extends CapturedEvent> type) {
    return new WorldContext(
        players(),
        Collections.unmodifiableList(
            history().stream().filter(e -> !type.isInstance(e)).collect(Collectors.toList())),
        icon);
  }

  public WorldContext record(CapturedEvent event, int historySize) {
    List<CapturedEvent> next = new ArrayList<>(history());
    next.add(event);
    if (next.size() > historySize) {
      next = new ArrayList<>(next.subList(next.size() - historySize, next.size()));
    }
    return new WorldContext(players(), Collections.unmodifiableList(next), icon);
  }

  public WorldContext apply(CapturedEvent event, int historySize) {
    return event.applyTo(this, historySize);
  }

  void bind(EventContext context) {
    history().forEach(event -> event.bind(context));
  }

  boolean valid() {
    return players().stream().allMatch(p -> p != null && p.valid())
        && history().stream().allMatch(e -> e != null && e.valid())
        && (icon == null || icon.png() != null);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorldContext
        && players().equals(((WorldContext) o).players())
        && history().equals(((WorldContext) o).history())
        && icon().equals(((WorldContext) o).icon());
  }

  @Override
  public int hashCode() {
    return Objects.hash(players(), history(), icon());
  }
}
