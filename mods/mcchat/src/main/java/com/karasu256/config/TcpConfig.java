package com.karasu256.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.karasu256.ChatNative;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.loader.api.FabricLoader;

public final class TcpConfig {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public boolean enabled = false;
  public int port;

  private transient Path path;
  private transient List<Runnable> listeners;

  private TcpConfig(int port) {
    this.port = port;
  }

  public void onSaved(Runnable listener) {
    if (listeners == null) {
      listeners = new CopyOnWriteArrayList<>();
    }
    listeners.add(listener);
  }

  public static TcpConfig load(String fileName, int defaultPort) {
    Path path = FabricLoader.getInstance().getConfigDir().resolve(fileName);
    TcpConfig config = new TcpConfig(defaultPort);
    try {
      if (Files.exists(path)) {
        TcpConfig read = GSON.fromJson(Files.readString(path), TcpConfig.class);
        if (read != null) {
          config = read;
        }
      }
    } catch (IOException | RuntimeException e) {
      ChatNative.LOGGER.error("Failed to load {}, using defaults", path, e);
    }
    config.path = path;
    config.save();
    return config;
  }

  public void save() {
    try {
      Files.writeString(path, GSON.toJson(this));
    } catch (IOException e) {
      ChatNative.LOGGER.error("Failed to save {}", path, e);
    }
    if (listeners != null) {
      listeners.forEach(Runnable::run);
    }
  }
}
