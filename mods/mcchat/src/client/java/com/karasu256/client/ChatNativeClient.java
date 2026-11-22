package com.karasu256.client;

import com.google.common.net.HostAndPort;
import com.karasu256.ChatNative;
import com.karasu256.capture.CapturePipeline;
import com.karasu256.capture.CapturedData;
import com.karasu256.capture.CapturedData.Kind;
import com.karasu256.capture.CapturedPacket;
import com.karasu256.capture.Origin;
import com.karasu256.client.capture.ClientCapture;
import com.karasu256.net.CaptureEndpointPayload;
import com.karasu256.net.TCPServer;
import com.karasu256.net.TCPSink;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public class ChatNativeClient implements ClientModInitializer {
  private TCPServer tcp;

  @Override
  public void onInitializeClient() {
    CapturePipeline pipeline = ChatNative.pipeline();
    ClientCapture.register();

    int port = ChatNative.config().clientPort;
    if (port > 0) {
      tcp = new TCPServer(pipeline::snapshot, this::receive);
      try {
        tcp.start(port);
        pipeline.addSink(new TCPSink(tcp));
      } catch (IOException e) {
        ChatNative.LOGGER.error("Failed to open TCP port {}", port, e);
        tcp = null;
      }
    }

    ClientPlayConnectionEvents.JOIN.register(
        (handler, sender, client) -> {
          if (tcp != null) {
            tcp.disconnectUpstream();
          }
          resetWorld(pipeline, client);
          ClientCapture.submitPlayerList(client);
        });

    ClientPlayNetworking.registerGlobalReceiver(
        CaptureEndpointPayload.TYPE,
        (payload, context) -> connectUpstream(pipeline, payload, context.client()));

    ClientLifecycleEvents.CLIENT_STOPPING.register(
        client -> {
          if (tcp != null) {
            tcp.close();
          }
          pipeline.close();
        });
  }

  private static void resetWorld(CapturePipeline pipeline, Minecraft client) {
    IntegratedServer local = client.getSingleplayerServer();
    if (local != null) {
      pipeline.reset("localhost", local.getWorldData().getLevelName(), Origin.SERVER);
      return;
    }
    ServerData server = client.getCurrentServer();
    String host = server == null ? "" : hostOf(server.ip);
    String name = server == null ? "" : server.name;
    pipeline.reset(host, name, Origin.CLIENT);
  }

  private void connectUpstream(
      CapturePipeline pipeline, CaptureEndpointPayload payload, Minecraft client) {
    if (tcp == null || !ChatNative.config().useUpstream) {
      return;
    }
    ServerData server = client.getCurrentServer();
    String host = payload.host().isEmpty() && server != null ? hostOf(server.ip) : payload.host();
    tcp.connect(
        host, payload.port(), up -> pipeline.setAuthority(up ? Origin.SERVER : Origin.CLIENT));
  }

  private void receive(CapturedPacket packet) {
    CapturePipeline pipeline = ChatNative.pipeline();
    if (packet.authority() == Origin.SERVER) {
      switch (packet.type()) {
        case SNAPSHOT ->
            pipeline.adoptContext(packet.world().context().without(Kind.COMMAND_EXECUTED));
        case EVENT ->
            packet
                .data()
                .filter(data -> data.kind() != Kind.COMMAND_EXECUTED)
                .ifPresent(pipeline::submit);
        case RESET -> {}
      }
      return;
    }
    if (packet.type() != CapturedPacket.Type.EVENT || packet.data().isEmpty()) {
      return;
    }
    CapturedData data = packet.data().get();
    String text = data.content().orElse("");
    if (text.isBlank()) {
      return;
    }
    Minecraft client = Minecraft.getInstance();
    switch (data.kind()) {
      case CHAT_SENT -> client.execute(() -> withConnection(client, c -> c.sendChat(text)));
      case COMMAND_SENT ->
          client.execute(
              () ->
                  withConnection(
                      client, c -> c.sendCommand(text.startsWith("/") ? text.substring(1) : text)));
      default -> {}
    }
  }

  private static void withConnection(Minecraft client, java.util.function.Consumer<ClientPacketListener> action) {
    ClientPacketListener connection = client.getConnection();
    if (connection != null) {
      action.accept(connection);
    }
  }

  private static String hostOf(String address) {
    return HostAndPort.fromString(address).getHost();
  }
}
