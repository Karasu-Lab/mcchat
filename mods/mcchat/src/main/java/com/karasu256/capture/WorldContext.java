package com.karasu256.capture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;

public record WorldContext(List<PlayerRef> players, List<CapturedData> history) {
  public static final WorldContext EMPTY = new WorldContext(List.of(), List.of());

  public static final Codec<WorldContext> CODEC =
      RecordCodecBuilder.create(
          i ->
              i.group(
                      PlayerRef.CODEC
                          .listOf()
                          .optionalFieldOf("players", List.of())
                          .forGetter(WorldContext::players),
                      CapturedData.CODEC
                          .listOf()
                          .optionalFieldOf("history", List.of())
                          .forGetter(WorldContext::history))
                  .apply(i, WorldContext::new));

  public WorldContext without(CapturedData.Kind kind) {
    return new WorldContext(players, history.stream().filter(d -> d.kind() != kind).toList());
  }

  public WorldContext apply(CapturedData data, int historySize) {
    switch (data.kind()) {
      case PLAYER_LIST:
        return new WorldContext(List.copyOf(data.players()), history);
      case PLAYER_JOINED:
        {
          if (data.player().isEmpty()) {
            return this;
          }
          PlayerRef joined = data.player().get();
          List<PlayerRef> next = new ArrayList<>(players);
          next.removeIf(p -> p.uuid().equals(joined.uuid()));
          next.add(joined);
          return new WorldContext(List.copyOf(next), history);
        }
      case PLAYER_LEFT:
        {
          if (data.player().isEmpty()) {
            return this;
          }
          PlayerRef left = data.player().get();
          List<PlayerRef> next = new ArrayList<>(players);
          next.removeIf(p -> p.uuid().equals(left.uuid()));
          return new WorldContext(List.copyOf(next), history);
        }
      default:
        {
          List<CapturedData> next = new ArrayList<>(history);
          next.add(data);
          if (next.size() > historySize) {
            next = new ArrayList<>(next.subList(next.size() - historySize, next.size()));
          }
          return new WorldContext(players, List.copyOf(next));
        }
    }
  }
}
