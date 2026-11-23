package com.karasu256.mcchat.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class WorldContext {
  public static final WorldContext EMPTY =
      new WorldContext(Collections.emptyList(), Collections.emptyList());

  private final List<PlayerRef> players;
  private final List<CapturedData> history;

  public WorldContext(List<PlayerRef> players, List<CapturedData> history) {
    this.players = players;
    this.history = history;
  }

  public List<PlayerRef> players() {
    return players;
  }

  public List<CapturedData> history() {
    return history;
  }

  public WorldContext without(CapturedData.Kind kind) {
    return new WorldContext(
        players,
        Collections.unmodifiableList(
            history.stream().filter(d -> d.kind() != kind).collect(Collectors.toList())));
  }

  public WorldContext apply(CapturedData data, int historySize) {
    switch (data.kind()) {
      case PLAYER_LIST:
        return new WorldContext(data.players(), history);
      case PLAYER_JOINED:
        {
          if (!data.player().isPresent()) {
            return this;
          }
          PlayerRef joined = data.player().get();
          List<PlayerRef> next = new ArrayList<>(players);
          next.removeIf(p -> p.uuid().equals(joined.uuid()));
          next.add(joined);
          return new WorldContext(Collections.unmodifiableList(next), history);
        }
      case PLAYER_LEFT:
        {
          if (!data.player().isPresent()) {
            return this;
          }
          PlayerRef left = data.player().get();
          List<PlayerRef> next = new ArrayList<>(players);
          next.removeIf(p -> p.uuid().equals(left.uuid()));
          return new WorldContext(Collections.unmodifiableList(next), history);
        }
      case ONLINE:
      case OFFLINE:
      case TYPING_STARTED:
      case TYPING_STOPPED:
      case WORLD_CHANGED:
        return this;
      default:
        {
          List<CapturedData> next = new ArrayList<>(history);
          next.add(data);
          if (next.size() > historySize) {
            next = new ArrayList<>(next.subList(next.size() - historySize, next.size()));
          }
          return new WorldContext(players, Collections.unmodifiableList(next));
        }
    }
  }

  JsonObject toJson() {
    JsonObject json = new JsonObject();
    if (!players.isEmpty()) {
      JsonArray array = new JsonArray();
      players.forEach(p -> array.add(p.toJson()));
      json.add("players", array);
    }
    if (!history.isEmpty()) {
      JsonArray array = new JsonArray();
      history.forEach(d -> array.add(d.toJson()));
      json.add("history", array);
    }
    return json;
  }

  static WorldContext fromJson(JsonObject json) {
    List<PlayerRef> players = new ArrayList<>();
    if (json.has("players")) {
      for (JsonElement element : json.getAsJsonArray("players")) {
        players.add(PlayerRef.fromJson(element.getAsJsonObject()));
      }
    }
    List<CapturedData> history = new ArrayList<>();
    if (json.has("history")) {
      for (JsonElement element : json.getAsJsonArray("history")) {
        history.add(CapturedData.fromJson(element.getAsJsonObject()));
      }
    }
    return new WorldContext(
        Collections.unmodifiableList(players), Collections.unmodifiableList(history));
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof WorldContext
        && players.equals(((WorldContext) o).players)
        && history.equals(((WorldContext) o).history);
  }

  @Override
  public int hashCode() {
    return Objects.hash(players, history);
  }
}
