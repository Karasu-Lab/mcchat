package com.karasu256.capture;

import com.karasu256.mcchat.common.PlayerRef;
import com.mojang.authlib.GameProfile;
import java.util.Optional;

public final class PlayerRefs {
  private PlayerRefs() {}

  public static PlayerRef of(GameProfile profile) {
    return new PlayerRef(profile.id(), profile.name(), Optional.empty());
  }
}
