package com.karasu256.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.karasu256.ChatNative;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ChatNativeConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public int clientPort = 25570;
  public int serverPort = 25566;

  public static ChatNativeConfig load() {
    Path path = FabricLoader.getInstance().getConfigDir().resolve(ChatNative.MOD_ID + ".json");
    ChatNativeConfig config = new ChatNativeConfig();
    try {
      if (Files.exists(path)) {
        ChatNativeConfig read = GSON.fromJson(Files.readString(path), ChatNativeConfig.class);
        if (read != null) {
          config = read;
        }
      }
      Files.writeString(path, GSON.toJson(config));
    } catch (IOException | RuntimeException e) {
      ChatNative.LOGGER.error("Failed to load {}, using defaults", path, e);
    }
    return config;
  }
}
