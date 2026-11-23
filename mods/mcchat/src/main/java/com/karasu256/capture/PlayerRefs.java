package com.karasu256.capture;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.util.Optional;

public final class PlayerRefs {
  private PlayerRefs() {}

  public static PlayerRef of(GameProfile profile) {
    Optional<PlayerRef.Skin> skin =
        profile.properties().get("textures").stream()
            .findFirst()
            .map(p -> new PlayerRef.Skin(p.value(), Optional.ofNullable(signatureOf(p))));
    return new PlayerRef(profile.id(), profile.name(), skin);
  }

  private static String signatureOf(Property property) {
    return property.hasSignature() ? property.signature() : null;
  }
}
