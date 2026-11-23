package com.karasu256.mcchat.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CapturedData {
  public enum Kind {
    CHAT_RECEIVED,
    CHAT_SENT,
    COMMAND_SENT,
    COMMAND_EXECUTED,
    PLAYER_LIST,
    PLAYER_JOINED,
    PLAYER_LEFT,
    ONLINE,
    OFFLINE,
    TYPING_STARTED,
    TYPING_STOPPED
  }

  private final UUID id;
  private final long timestampMillis;
  private final Origin origin;
  private final Kind kind;
  private final Optional<PlayerRef> player;
  private final Optional<String> content;
  private final List<PlayerRef> players;

  public CapturedData(
      UUID id,
      long timestampMillis,
      Origin origin,
      Kind kind,
      Optional<PlayerRef> player,
      Optional<String> content,
      List<PlayerRef> players) {
    this.id = id;
    this.timestampMillis = timestampMillis;
    this.origin = origin;
    this.kind = kind;
    this.player = player;
    this.content = content;
    this.players = players;
  }

  public UUID id() {
    return id;
  }

  public long timestampMillis() {
    return timestampMillis;
  }

  public Origin origin() {
    return origin;
  }

  public Kind kind() {
    return kind;
  }

  public Optional<PlayerRef> player() {
    return player;
  }

  public Optional<String> content() {
    return content;
  }

  public List<PlayerRef> players() {
    return players;
  }

  public static CapturedData of(Origin origin, Kind kind, PlayerRef player, String content) {
    return new CapturedData(
        UUID.randomUUID(),
        System.currentTimeMillis(),
        origin,
        kind,
        Optional.ofNullable(player),
        Optional.ofNullable(content),
        Collections.emptyList());
  }

  public static CapturedData playerList(Origin origin, List<PlayerRef> players) {
    return new CapturedData(
        UUID.randomUUID(),
        System.currentTimeMillis(),
        origin,
        Kind.PLAYER_LIST,
        Optional.empty(),
        Optional.empty(),
        Collections.unmodifiableList(new ArrayList<>(players)));
  }

  JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("id", id.toString());
    json.addProperty("timestamp", timestampMillis);
    json.addProperty("origin", origin.wireName());
    json.addProperty("kind", kind.name().toLowerCase(Locale.ROOT));
    player.ifPresent(p -> json.add("player", p.toJson()));
    content.ifPresent(c -> json.addProperty("content", c));
    if (!players.isEmpty()) {
      JsonArray array = new JsonArray();
      players.forEach(p -> array.add(p.toJson()));
      json.add("players", array);
    }
    return json;
  }

  static CapturedData fromJson(JsonObject json) {
    List<PlayerRef> players = new ArrayList<>();
    if (json.has("players")) {
      for (JsonElement element : json.getAsJsonArray("players")) {
        players.add(PlayerRef.fromJson(element.getAsJsonObject()));
      }
    }
    return new CapturedData(
        UUID.fromString(json.get("id").getAsString()),
        json.get("timestamp").getAsLong(),
        Origin.fromWireName(json.get("origin").getAsString()),
        Kind.valueOf(json.get("kind").getAsString().toUpperCase(Locale.ROOT)),
        json.has("player")
            ? Optional.of(PlayerRef.fromJson(json.getAsJsonObject("player")))
            : Optional.empty(),
        json.has("content") ? Optional.of(json.get("content").getAsString()) : Optional.empty(),
        Collections.unmodifiableList(players));
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CapturedData)) {
      return false;
    }
    CapturedData other = (CapturedData) o;
    return id.equals(other.id)
        && timestampMillis == other.timestampMillis
        && origin == other.origin
        && kind == other.kind
        && player.equals(other.player)
        && content.equals(other.content)
        && players.equals(other.players);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, timestampMillis, origin, kind, player, content, players);
  }
}
