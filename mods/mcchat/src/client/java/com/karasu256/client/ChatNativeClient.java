package com.karasu256.client;

import com.google.common.net.HostAndPort;
import com.karasu256.ChatNative;
import com.karasu256.capture.CapturePipeline;
import com.karasu256.capture.ServerCapture;
import com.karasu256.capture.ServerWorldSource;
import com.karasu256.client.capture.ClientCapture;
import com.karasu256.client.capture.RemoteWorldSource;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.CommandExecutedEvent;
import com.karasu256.mcchat.common.event.PlayerListEvent;
import com.karasu256.net.CaptureEndpointPayload;
import com.karasu256.net.TCPServer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public class ChatNativeClient implements ClientModInitializer {
  private ClientTcpController tcp;

  @Override
  public void onInitializeClient() {
    CapturePipeline pipeline = ChatNative.pipeline();
    ClientCapture.register();
    pipeline.setOnline(false, ClientCapture.sessionPlayer());

    tcp = new ClientTcpController(ChatNative.clientConfig(), pipeline, this::receive);

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
      TCPServer server = tcp.tcp();
      if (server != null) {
        server.disconnectUpstream();
      }
      resetWorld(pipeline, client);
      pipeline.setOnline(true, ClientCapture.sessionPlayer());
      ClientCapture.submitPlayerList(client);
    });

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      pipeline.clearIcon();
      pipeline.setOnline(false, ClientCapture.sessionPlayer());
    });

    ClientPlayNetworking.registerGlobalReceiver(
        CaptureEndpointPayload.TYPE,
        (payload, player, sender) -> connectUpstream(pipeline, payload, Minecraft.getInstance()));

    ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
      tcp.stop();
      pipeline.close();
    });
  }

  private static void resetWorld(CapturePipeline pipeline, Minecraft client) {
    IntegratedServer local = client.getSingleplayerServer();
    if (local != null) {
      pipeline.reset(new ServerWorldSource(local));
      local.execute(() ->
          new PlayerListEvent(pipeline, Origin.SERVER, ServerCapture.onlinePlayers(local))
              .handle());
      return;
    }
    pipeline.reset(new RemoteWorldSource(client.getCurrentServer()));
  }

  private void connectUpstream(
      CapturePipeline pipeline, CaptureEndpointPayload payload, Minecraft client) {
    TCPServer upstream = tcp.tcp();
    if (upstream == null) {
      return;
    }
    ServerData server = client.getCurrentServer();
    String host = payload.host().isEmpty() && server != null ? hostOf(server.ip) : payload.host();
    upstream.connect(
        host, payload.port(), up -> pipeline.setAuthority(up ? Origin.SERVER : Origin.CLIENT));
  }

  private void receive(CapturedPacket packet) {
    CapturePipeline pipeline = ChatNative.pipeline();
    if (packet.type() == CapturedPacket.Type.PING) {
      pipeline.requestStatus();
      return;
    }
    boolean fromServer = packet.authority() == Origin.SERVER;
    packet.bind(fromServer ? pipeline : event -> {});
    if (fromServer && packet.type() == CapturedPacket.Type.SNAPSHOT) {
      pipeline.adoptContext(packet.world().context().without(CommandExecutedEvent.class));
    } else if (packet.type() == CapturedPacket.Type.EVENT) {
      packet.event().ifPresent(CapturedEvent::handle);
    }
  }

  private static String hostOf(String address) {
    return HostAndPort.fromString(address).getHost();
  }
}
