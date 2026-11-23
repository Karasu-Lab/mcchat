package com.karasu256.client;

import com.google.common.net.HostAndPort;
import com.karasu256.ChatNative;
import com.karasu256.capture.CapturePipeline;
import com.karasu256.capture.ServerCapture;
import com.karasu256.mcchat.common.CapturedData;
import com.karasu256.mcchat.common.CapturedData.Kind;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
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
    pipeline.setOnline(false, ClientCapture.sessionPlayer());

    int port = ChatNative.config().clientPort;
    tcp = new TCPServer(pipeline::snapshot, this::receive);
    try {
      tcp.start(port);
      pipeline.addSink(new TCPSink(tcp));
      ClientCapture.setUpstream(tcp::sendUpstream);
    } catch (IOException e) {
      ChatNative.LOGGER.error("Failed to open TCP port {}", port, e);
      tcp = null;
    }

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
      if (tcp != null) {
        tcp.disconnectUpstream();
      }
      resetWorld(pipeline, client);
      pipeline.setOnline(true, ClientCapture.sessionPlayer());
      ClientCapture.submitPlayerList(client);
    });

    ClientPlayConnectionEvents.DISCONNECT.register(
        (handler, client) -> pipeline.setOnline(false, ClientCapture.sessionPlayer()));

    ClientPlayNetworking.registerGlobalReceiver(
        CaptureEndpointPayload.TYPE,
        (payload, player, sender) ->
            connectUpstream(pipeline, payload, Minecraft.getInstance()));

    ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
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
      local.execute(() ->
          pipeline.submit(
              CapturedData.playerList(Origin.SERVER, ServerCapture.onlinePlayers(local))));
      return;
    }
    ServerData server = client.getCurrentServer();
    String host = server == null ? "" : hostOf(server.ip);
    String name = server == null ? "" : server.name;
    pipeline.reset(host, name, Origin.CLIENT);
  }

  private void connectUpstream(
      CapturePipeline pipeline, CaptureEndpointPayload payload, Minecraft client) {
    if (tcp == null) {
      return;
    }
    ServerData server = client.getCurrentServer();
    String host = payload.host().isEmpty() && server != null ? hostOf(server.ip) : payload.host();
    tcp.connect(
        host, payload.port(), up -> pipeline.setAuthority(up ? Origin.SERVER : Origin.CLIENT));
  }

  private void receive(CapturedPacket packet) {
    CapturePipeline pipeline = ChatNative.pipeline();
    if (packet.type() == CapturedPacket.Type.PING) {
      pipeline.requestStatus();
      return;
    }
    if (packet.authority() == Origin.SERVER) {
      switch (packet.type()) {
        case SNAPSHOT ->
            pipeline.adoptContext(packet.world().context().without(Kind.COMMAND_EXECUTED));
        case EVENT ->
            packet
                .data()
                .filter(data -> data.kind() != Kind.COMMAND_EXECUTED && !isOwnTyping(data))
                .ifPresent(pipeline::submit);
        case RESET -> {}
      }
      return;
    }
    if (packet.type() != CapturedPacket.Type.EVENT || packet.data().isEmpty()) {
      return;
    }
    CapturedData data = packet.data().get();
    Minecraft client = Minecraft.getInstance();
    if (client.player == null || client.getConnection() == null) {
      return;
    }
    if (data.kind() == Kind.TYPING_STARTED || data.kind() == Kind.TYPING_STOPPED) {
      boolean started = data.kind() == Kind.TYPING_STARTED;
      client.execute(() -> ClientCapture.relayTyping(started));
      return;
    }
    String text = data.content().orElse("");
    if (text.isBlank()) {
      return;
    }
    switch (data.kind()) {
      case CHAT_SENT -> client.execute(() -> withConnection(client, c -> c.sendChat(text)));
      case COMMAND_SENT ->
          client.execute(() ->
              withConnection(
                  client, c -> c.sendCommand(text.startsWith("/") ? text.substring(1) : text)));
      default -> {}
    }
  }

  private static boolean isOwnTyping(CapturedData data) {
    if (data.kind() != Kind.TYPING_STARTED && data.kind() != Kind.TYPING_STOPPED) {
      return false;
    }
    Minecraft client = Minecraft.getInstance();
    return client.player != null
        && data.player().filter(p -> p.uuid().equals(client.player.getUUID())).isPresent();
  }

  private static void withConnection(
      Minecraft client, java.util.function.Consumer<ClientPacketListener> action) {
    ClientPacketListener connection = client.getConnection();
    if (connection != null) {
      action.accept(connection);
    }
  }

  private static String hostOf(String address) {
    return HostAndPort.fromString(address).getHost();
  }
}
