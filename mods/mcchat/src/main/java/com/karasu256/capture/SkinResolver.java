package com.karasu256.capture;

import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.event.CapturedEvent;
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

  public static CapturedEvent attach(CapturedEvent event) {
    return event.mapPlayers(SkinResolver::attachSkin);
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
