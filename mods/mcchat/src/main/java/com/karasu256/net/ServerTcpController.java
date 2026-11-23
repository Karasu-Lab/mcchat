package com.karasu256.net;

import com.karasu256.ChatNative;
import com.karasu256.capture.CapturePipeline;
import com.karasu256.config.TcpConfig;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.event.EventRegistry;
import com.karasu256.mcchat.common.event.TypingEvent;
import java.io.IOException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerTcpController {
  private final TcpConfig config;

  private MinecraftServer server;
  private boolean runsWithoutServer;
  private CapturePipeline pipeline;
  private volatile TCPServer tcp;
  private TCPSink sink;

  public ServerTcpController(TcpConfig config) {
    this.config = config;
    config.onSaved(this::apply);
  }

  public TCPServer tcp() {
    return tcp;
  }

  public synchronized void attach(MinecraftServer server) {
    this.server = server;
    apply();
  }

  public synchronized void runWithoutServer() {
    runsWithoutServer = true;
    apply();
  }

  public synchronized void detach() {
    if (!runsWithoutServer) {
      stop();
    }
    server = null;
  }

  private synchronized void apply() {
    if (server == null && !runsWithoutServer) {
      return;
    }
    if (tcp != null && (!config.enabled || tcp.port() != config.port)) {
      stop();
    }
    if (tcp == null && config.enabled) {
      start();
    }
  }

  private void start() {
    CapturePipeline target = server == null ? ChatNative.pipeline() : ChatNative.pipelineFor(server);
    TCPServer opened =
        new TCPServer(
            target::snapshot,
            packet -> {
              if (packet.type() == CapturedPacket.Type.PING) {
                target.requestStatus();
              } else if (packet.type() == CapturedPacket.Type.EVENT) {
                packet
                    .event()
                    .filter(TypingEvent.class::isInstance)
                    .map(TypingEvent.class::cast)
                    .ifPresent(typing -> target.submit(typing.relayedAs(target, Origin.SERVER)));
              }
            },
            EventRegistry.common());
    try {
      opened.start(config.port);
    } catch (IOException e) {
      ChatNative.LOGGER.error("Failed to open TCP port {}", config.port, e);
      return;
    }
    tcp = opened;
    pipeline = target;
    sink = new TCPSink(opened);
    target.addSink(sink);
    if (server != null && server.isDedicatedServer()) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        if (ServerPlayNetworking.canSend(player, CaptureEndpointPayload.TYPE)) {
          ServerPlayNetworking.send(player, new CaptureEndpointPayload("", opened.port()));
        }
      }
    }
  }

  private void stop() {
    if (tcp == null) {
      return;
    }
    pipeline.removeSink(sink);
    tcp.close();
    tcp = null;
    sink = null;
    pipeline = null;
  }
}
