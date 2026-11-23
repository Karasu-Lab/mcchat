package com.karasu256;

import com.karasu256.capture.CapturePipeline;
import com.karasu256.capture.ServerCapture;
import com.karasu256.config.ChatNativeConfig;
import com.karasu256.mcchat.common.CapturedData;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.net.CaptureEndpointPayload;
import com.karasu256.net.TCPServer;
import com.karasu256.net.TCPSink;
import java.io.IOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
  private static TCPServer integratedServer;
  private static TCPSink integratedSink;

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
    pipeline = new CapturePipeline();
    ServerCapture.register();

    ServerLifecycleEvents.SERVER_STARTED.register(server -> {
      if (server.isDedicatedServer()) {
        startDedicatedCapture(server);
      } else {
        startIntegratedCapture();
      }
    });

    ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
      if (server.isDedicatedServer()) {
        stopDedicatedCapture();
      } else {
        stopIntegratedCapture();
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
    dedicatedPipeline = new CapturePipeline();
    dedicatedPipeline.reset("", server.getMotd(), Origin.SERVER);
    dedicatedPipeline.setOnline(true, null);
    dedicatedServer = openServerPort(dedicatedPipeline);
  }

  private static void startIntegratedCapture() {
    integratedServer = openServerPort(pipeline);
    if (integratedServer != null) {
      integratedSink = new TCPSink(integratedServer);
      pipeline.addSink(integratedSink);
    }
  }

  private static TCPServer openServerPort(CapturePipeline target) {
    TCPServer tcp =
        new TCPServer(
            target::snapshot,
            packet -> {
              if (packet.type() == CapturedPacket.Type.PING) {
                target.requestStatus();
              } else if (packet.type() == CapturedPacket.Type.EVENT) {
                packet
                    .data()
                    .filter(ChatNative::isTyping)
                    .ifPresent(data -> relayTyping(target, data));
              }
            });
    try {
      tcp.start(config.serverPort);
    } catch (IOException e) {
      LOGGER.error("Failed to open TCP port {}", config.serverPort, e);
      return null;
    }
    if (target == dedicatedPipeline) {
      target.addSink(new TCPSink(tcp));
    }
    return tcp;
  }

  private static void stopIntegratedCapture() {
    if (integratedSink != null) {
      pipeline.removeSink(integratedSink);
      integratedSink = null;
    }
    if (integratedServer != null) {
      integratedServer.close();
      integratedServer = null;
    }
  }

  private static boolean isTyping(CapturedData data) {
    return data.kind() == CapturedData.Kind.TYPING_STARTED
        || data.kind() == CapturedData.Kind.TYPING_STOPPED;
  }

  private static void relayTyping(CapturePipeline pipeline, CapturedData data) {
    pipeline.submit(
        new CapturedData(
            data.id(),
            data.timestampMillis(),
            Origin.SERVER,
            data.kind(),
            data.player(),
            data.content(),
            data.players()));
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
