package com.karasu256.mcchat.common;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    TYPING_STOPPED,
    WORLD_CHANGED,
    WORLD_ICON
  }

  private final UUID id;
  @SerializedName("timestamp")
  private final long timestampMillis;

  private final Origin origin;
  private final Kind kind;
  private final PlayerRef player;
  private final String content;
  private final List<PlayerRef> players;
  private final MinecraftScreen screen;
  private final WorldIcon icon;

  public CapturedData(
      UUID id,
      long timestampMillis,
      Origin origin,
      Kind kind,
      Optional<PlayerRef> player,
      Optional<String> content,
      List<PlayerRef> players) {
    this(id, timestampMillis, origin, kind, player, content, players, null, null);
  }

  private CapturedData(
      UUID id,
      long timestampMillis,
      Origin origin,
      Kind kind,
      Optional<PlayerRef> player,
      Optional<String> content,
      List<PlayerRef> players,
      MinecraftScreen screen,
      WorldIcon icon) {
    this.screen = screen;
    this.icon = icon;
    this.id = id;
    this.timestampMillis = timestampMillis;
    this.origin = origin;
    this.kind = kind;
    this.player = player.orElse(null);
    this.content = content.orElse(null);
    this.players = players.isEmpty() ? null : players;
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
    return Optional.ofNullable(player);
  }

  public Optional<String> content() {
    return Optional.ofNullable(content);
  }

  public List<PlayerRef> players() {
    return players == null ? Collections.emptyList() : players;
  }

  public Optional<MinecraftScreen> screen() {
    return Optional.ofNullable(screen);
  }

  public Optional<WorldIcon> icon() {
    return Optional.ofNullable(icon);
  }

  public static CapturedData status(
      Origin origin, Kind kind, PlayerRef player, MinecraftScreen screen) {
    return new CapturedData(
        UUID.randomUUID(),
        System.currentTimeMillis(),
        origin,
        kind,
        Optional.ofNullable(player),
        Optional.empty(),
        Collections.emptyList(),
        screen,
        null);
  }

  public static CapturedData worldIcon(Origin origin, WorldIcon icon) {
    return new CapturedData(
        UUID.randomUUID(),
        System.currentTimeMillis(),
        origin,
        Kind.WORLD_ICON,
        Optional.empty(),
        Optional.empty(),
        Collections.emptyList(),
        null,
        icon);
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

  boolean valid() {
    return id != null
        && origin != null
        && kind != null
        && (player == null || player.valid())
        && (screen == null || screen.name() != null)
        && (icon == null || icon.png() != null)        && players().stream().allMatch(p -> p != null && p.valid());
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
        && player().equals(other.player())
        && content().equals(other.content())
        && players().equals(other.players())
        && screen().equals(other.screen())
        && icon().equals(other.icon());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, timestampMillis, origin, kind, player(), content(), players(), screen(), icon());
  }
}
