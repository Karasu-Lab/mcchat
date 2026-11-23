package com.karasu256.mcchat.common;

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
  private final List<CapturedData> history;
  private final WorldIcon icon;

  public WorldContext(List<PlayerRef> players, List<CapturedData> history) {
    this(players, history, null);
  }

  public WorldContext(List<PlayerRef> players, List<CapturedData> history, WorldIcon icon) {
    this.players = players.isEmpty() ? null : players;
    this.history = history.isEmpty() ? null : history;
    this.icon = icon;
  }

  public List<PlayerRef> players() {
    return players == null ? Collections.emptyList() : players;
  }

  public List<CapturedData> history() {
    return history == null ? Collections.emptyList() : history;
  }

  public Optional<WorldIcon> icon() {
    return Optional.ofNullable(icon);
  }

  public WorldContext withIcon(WorldIcon next) {
    return new WorldContext(players(), history(), next);
  }

  public WorldContext without(CapturedData.Kind kind) {
    return new WorldContext(
        players(),
        Collections.unmodifiableList(
            history().stream().filter(d -> d.kind() != kind).collect(Collectors.toList())),
        icon);
  }

  public WorldContext apply(CapturedData data, int historySize) {
    switch (data.kind()) {
      case PLAYER_LIST:
        return new WorldContext(data.players(), history(), icon);
      case PLAYER_JOINED:
        {
          if (!data.player().isPresent()) {
            return this;
          }
          PlayerRef joined = data.player().get();
          List<PlayerRef> next = new ArrayList<>(players());
          next.removeIf(p -> p.uuid().equals(joined.uuid()));
          next.add(joined);
          return new WorldContext(Collections.unmodifiableList(next), history(), icon);
        }
      case PLAYER_LEFT:
        {
          if (!data.player().isPresent()) {
            return this;
          }
          PlayerRef left = data.player().get();
          List<PlayerRef> next = new ArrayList<>(players());
          next.removeIf(p -> p.uuid().equals(left.uuid()));
          return new WorldContext(Collections.unmodifiableList(next), history(), icon);
        }
      case WORLD_ICON:
        return withIcon(data.icon().orElse(null));
      case ONLINE:
      case OFFLINE:
      case TYPING_STARTED:
      case TYPING_STOPPED:
      case WORLD_CHANGED:
        return this;
      default:
        {
          List<CapturedData> next = new ArrayList<>(history());
          next.add(data);
          if (next.size() > historySize) {
            next = new ArrayList<>(next.subList(next.size() - historySize, next.size()));
          }
          return new WorldContext(players(), Collections.unmodifiableList(next), icon);
        }
    }
  }

  boolean valid() {
    return players().stream().allMatch(p -> p != null && p.valid())
        && history().stream().allMatch(d -> d != null && d.valid())
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
