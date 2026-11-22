package com.karasu256;

import com.karasu256.capture.CapturePipeline;
import com.karasu256.capture.Origin;
import com.karasu256.capture.ServerCapture;
import com.karasu256.config.ChatNativeConfig;
import com.karasu256.net.CaptureEndpointPayload;
import com.karasu256.net.TCPServer;
import com.karasu256.net.TCPSink;
import java.io.IOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatNative implements ModInitializer {
  public static final String MOD_ID = "chatnative";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  private static ChatNativeConfig config;
  private static CapturePipeline pipeline;
  private static CapturePipeline dedicatedPipeline;
  private static TCPServer dedicatedServer;

  public static ChatNativeConfig config() {
    return config;
  }

  public static CapturePipeline pipeline() {
    return pipeline;
  }

  public static CapturePipeline pipelineFor(MinecraftServer server) {
    return server.isDedicatedServer() && dedicatedPipeline != null ? dedicatedPipeline : pipeline;
  }

  @Override
  public void onInitialize() {
    config = ChatNativeConfig.load();
    pipeline = new CapturePipeline(config.historySize);
    ServerCapture.register();

    PayloadTypeRegistry.clientboundPlay()
        .register(CaptureEndpointPayload.TYPE, CaptureEndpointPayload.CODEC);

    ServerLifecycleEvents.SERVER_STARTED.register(server -> {
      if (server.isDedicatedServer()) {
        startDedicatedCapture(server);
      }
    });

    ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
      if (server.isDedicatedServer()) {
        stopDedicatedCapture();
      }
    });

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      if (server.isDedicatedServer()
          && dedicatedServer != null
          && ServerPlayNetworking.canSend(handler, CaptureEndpointPayload.TYPE)) {
        sender.sendPacket(new CaptureEndpointPayload("", dedicatedServer.port()));
      }
    });
  }

  private static void startDedicatedCapture(MinecraftServer server) {
    dedicatedPipeline = new CapturePipeline(config.historySize);
    dedicatedPipeline.reset("", server.getMotd(), Origin.SERVER);
    if (config.serverPort <= 0) {
      return;
    }
    TCPServer tcp = new TCPServer(dedicatedPipeline::snapshot, packet -> {});
    try {
      tcp.start(config.serverPort);
    } catch (IOException e) {
      LOGGER.error("Failed to open TCP port {}", config.serverPort, e);
      return;
    }
    dedicatedPipeline.addSink(new TCPSink(tcp));
    dedicatedServer = tcp;
  }

  private static void stopDedicatedCapture() {
    if (dedicatedServer != null) {
      dedicatedServer.close();
      dedicatedServer = null;
    }
    if (dedicatedPipeline != null) {
      dedicatedPipeline.close();
      dedicatedPipeline = null;
    }
  }
}
