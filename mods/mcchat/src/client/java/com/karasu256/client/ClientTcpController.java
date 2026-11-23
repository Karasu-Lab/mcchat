package com.karasu256.client;

import com.karasu256.ChatNative;
import com.karasu256.capture.CapturePipeline;
import com.karasu256.client.capture.ClientCapture;
import com.karasu256.client.event.ClientChatSentEvent;
import com.karasu256.client.event.ClientCommandExecutedEvent;
import com.karasu256.client.event.ClientCommandSentEvent;
import com.karasu256.client.event.ClientTypingStartedEvent;
import com.karasu256.client.event.ClientTypingStoppedEvent;
import com.karasu256.config.TcpConfig;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.event.EventRegistry;
import com.karasu256.net.TCPServer;
import com.karasu256.net.TCPSink;
import java.io.IOException;
import java.util.function.Consumer;

final class ClientTcpController {
  private final TcpConfig config;
  private final CapturePipeline pipeline;
  private final Consumer<CapturedPacket> handler;
  private final EventRegistry registry =
      new EventRegistry()
          .register(ClientChatSentEvent.class)
          .register(ClientCommandSentEvent.class)
          .register(ClientCommandExecutedEvent.class)
          .register(ClientTypingStartedEvent.class)
          .register(ClientTypingStoppedEvent.class);

  private volatile TCPServer tcp;
  private TCPSink sink;

  ClientTcpController(
      TcpConfig config, CapturePipeline pipeline, Consumer<CapturedPacket> handler) {
    this.config = config;
    this.pipeline = pipeline;
    this.handler = handler;
    config.onSaved(this::apply);
    apply();
  }

  TCPServer tcp() {
    return tcp;
  }

  synchronized void stop() {
    if (tcp == null) {
      return;
    }
    pipeline.removeSink(sink);
    ClientCapture.setUpstream(null);
    pipeline.setAuthority(Origin.CLIENT);
    tcp.close();
    tcp = null;
    sink = null;
  }

  private synchronized void apply() {
    if (tcp != null && (!config.enabled || tcp.port() != config.port)) {
      stop();
    }
    if (tcp == null && config.enabled) {
      start();
    }
  }

  private void start() {
    TCPServer server = new TCPServer(pipeline::snapshot, handler, registry);
    try {
      server.start(config.port);
    } catch (IOException e) {
      ChatNative.LOGGER.error("Failed to open TCP port {}", config.port, e);
      return;
    }
    tcp = server;
    sink = new TCPSink(server);
    pipeline.addSink(sink);
    ClientCapture.setUpstream(server::sendUpstream);
  }
}
