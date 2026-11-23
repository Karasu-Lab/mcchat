package com.karasu256;

import com.karasu256.capture.CapturePipeline;
import com.karasu256.capture.ServerCapture;
import com.karasu256.capture.ServerChatTextProcessor;
import com.karasu256.capture.ServerTypingRelay;
import com.karasu256.capture.ServerWorldSource;
import com.karasu256.config.TcpConfig;
import com.karasu256.net.CaptureEndpointPayload;
import com.karasu256.net.ChatTextPayload;
import com.karasu256.net.ServerTcpController;
import com.karasu256.net.TCPServer;
import com.karasu256.net.TypingPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatNative implements ModInitializer {
  public static final String MOD_ID = "chatnative";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  private static TcpConfig serverConfig;
  private static TcpConfig clientConfig;
  private static CapturePipeline pipeline;
  private static CapturePipeline dedicatedPipeline;
  private static ServerTcpController serverTcp;

  public static synchronized TcpConfig serverConfig() {
    if (serverConfig == null) {
      serverConfig = TcpConfig.load(MOD_ID + "-server.json", 25566);
    }
    return serverConfig;
  }

  public static synchronized TcpConfig clientConfig() {
    if (clientConfig == null) {
      clientConfig = TcpConfig.load(MOD_ID + "-client.json", 25570);
    }
    return clientConfig;
  }

  public static CapturePipeline pipeline() {
    return pipeline;
  }

  public static String levelId(MinecraftServer server) {
    return server
        .getWorldPath(LevelResource.ROOT)
        .toAbsolutePath()
        .normalize()
        .getFileName()
        .toString();
  }

  public static CapturePipeline pipelineFor(MinecraftServer server) {
    return server.isDedicatedServer() && dedicatedPipeline != null ? dedicatedPipeline : pipeline;
  }

  @Override
  public void onInitialize() {
    pipeline = new CapturePipeline();
    serverTcp = new ServerTcpController(serverConfig());
    ServerCapture.register();
    ServerPlayNetworking.registerGlobalReceiver(
        ChatTextPayload.TYPE,
        (payload, player, sender) -> ServerChatTextProcessor.hold(player, payload.text()));
    ServerPlayNetworking.registerGlobalReceiver(
        TypingPayload.TYPE,
        (payload, player, sender) -> ServerTypingRelay.relay(player, payload.started()));

    ServerLifecycleEvents.SERVER_STARTED.register(server -> {
      if (server.isDedicatedServer()) {
        dedicatedPipeline = new CapturePipeline();
        dedicatedPipeline.reset(new ServerWorldSource(server));
        dedicatedPipeline.setOnline(true, null);
      }
      serverTcp.attach(server);
    });

    ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
      serverTcp.detach();
      if (dedicatedPipeline != null) {
        dedicatedPipeline.close();
        dedicatedPipeline = null;
      }
    });

    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> ServerTypingRelay.relay(handler.getPlayer(), false));

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      TCPServer tcp = serverTcp.tcp();
      if (server.isDedicatedServer()
          && tcp != null
          && ServerPlayNetworking.canSend(handler, CaptureEndpointPayload.TYPE)) {
        sender.sendPacket(new CaptureEndpointPayload("", tcp.port()));
      }
    });
  }
}
