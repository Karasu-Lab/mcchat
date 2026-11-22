package com.karasu256.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record CapturedData(
    UUID id,
    long timestampMillis,
    Origin origin,
    Kind kind,
    Optional<PlayerRef> player,
    Optional<String> content,
    List<PlayerRef> players) {

  public enum Kind {
    CHAT_RECEIVED,
    CHAT_SENT,
    COMMAND_SENT,
    COMMAND_EXECUTED,
    PLAYER_LIST,
    PLAYER_JOINED,
    PLAYER_LEFT;

    public static final Codec<Kind> CODEC = Origin.enumCodec(Kind.class);
  }

  public static final Codec<CapturedData> CODEC =
      RecordCodecBuilder.create(
          i ->
              i.group(
                      PlayerRef.UUID_CODEC.fieldOf("id").forGetter(CapturedData::id),
                      Codec.LONG.fieldOf("timestamp").forGetter(CapturedData::timestampMillis),
                      Origin.CODEC.fieldOf("origin").forGetter(CapturedData::origin),
                      Kind.CODEC.fieldOf("kind").forGetter(CapturedData::kind),
                      PlayerRef.CODEC.optionalFieldOf("player").forGetter(CapturedData::player),
                      Codec.STRING.optionalFieldOf("content").forGetter(CapturedData::content),
                      PlayerRef.CODEC
                          .listOf()
                          .optionalFieldOf("players", List.of())
                          .forGetter(CapturedData::players))
                  .apply(i, CapturedData::new));

  public static CapturedData of(Origin origin, Kind kind, PlayerRef player, String content) {
    return new CapturedData(
        UUID.randomUUID(),
        System.currentTimeMillis(),
        origin,
        kind,
        Optional.ofNullable(player),
        Optional.ofNullable(content),
        List.of());
  }

  public static CapturedData playerList(Origin origin, List<PlayerRef> players) {
    return new CapturedData(
        UUID.randomUUID(),
        System.currentTimeMillis(),
        origin,
        Kind.PLAYER_LIST,
        Optional.empty(),
        Optional.empty(),
        List.copyOf(players));
  }
}
