package com.karasu256.client.capture;

import com.karasu256.ChatNative;
import com.karasu256.capture.SkinResolver;
import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.PlayerSkin;

public final class ClientSkins {
  private static final Set<UUID> requested = ConcurrentHashMap.newKeySet();

  private ClientSkins() {}

  public static void request(GameProfile profile) {
    if (!requested.add(profile.id())) {
      return;
    }
    Minecraft client = Minecraft.getInstance();
    client
        .getSkinManager()
        .get(profile)
        .handleAsync(
            (skin, error) -> {
              store(client, profile.id(), skin == null ? Optional.<PlayerSkin>empty() : skin, profile);
              return null;
            },
            client);
  }

  private static void store(
      Minecraft client, UUID id, Optional<PlayerSkin> resolved, GameProfile profile) {
    PlayerSkin skin = resolved.orElseGet(() -> DefaultPlayerSkin.get(profile));
    Identifier path = skin.body().texturePath();
    try {
      byte[] png = readPng(client, path);
      if (png == null) {
        requested.remove(id);
        return;
      }
      SkinResolver.put(id, png);
    } catch (IOException e) {
      ChatNative.LOGGER.warn("Cannot read resolved skin of {}", id, e);
      requested.remove(id);
      return;
    }
    ChatNative.pipeline().republishPlayers();
  }

  private static byte[] readPng(Minecraft client, Identifier path) throws IOException {
    AbstractTexture texture = client.getTextureManager().getTexture(path);
    if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
      Path file = Files.createTempFile("chatnative-skin", ".png");
      try {
        dynamic.getPixels().writeToFile(file);
        return Files.readAllBytes(file);
      } finally {
        Files.delete(file);
      }
    }
    Optional<Resource> resource = client.getResourceManager().getResource(path);
    if (resource.isEmpty()) {
      return null;
    }
    try (InputStream in = resource.get().open()) {
      return in.readAllBytes();
    }
  }
}
