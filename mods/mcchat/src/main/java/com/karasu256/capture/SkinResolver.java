package com.karasu256.capture;

import com.karasu256.mcchat.common.CapturedData;
import com.karasu256.mcchat.common.PlayerRef;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinResolver {
  private static final Map<UUID, byte[]> PNGS = new ConcurrentHashMap<>();

  private SkinResolver() {}

  public static void put(UUID uuid, byte[] png) {
    PNGS.put(uuid, png);
  }

  public static CapturedData attach(CapturedData data) {
    switch (data.kind()) {
      case PLAYER_LIST:
      case PLAYER_JOINED:
      case PLAYER_LEFT:
        return new CapturedData(
            data.id(),
            data.timestampMillis(),
            data.origin(),
            data.kind(),
            data.player().map(SkinResolver::attachSkin),
            data.content(),
            attachAll(data.players()));
      default:
        return data;
    }
  }

  public static List<PlayerRef> attachAll(List<PlayerRef> refs) {
    return refs.stream().map(SkinResolver::attachSkin).toList();
  }

  private static PlayerRef attachSkin(PlayerRef ref) {
    byte[] png = PNGS.get(ref.uuid());
    if (ref.skin().isPresent() || png == null) {
      return ref;
    }
    return new PlayerRef(ref.uuid(), ref.name(), Optional.of(new PlayerRef.Skin(png)));
  }
}
