package com.karasu256.net;

import com.karasu256.capture.CaptureSink;
import com.karasu256.capture.CapturedPacket;

public final class TCPSink implements CaptureSink {
  private final TCPServer server;

  public TCPSink(TCPServer server) {
    this.server = server;
  }

  @Override
  public void accept(CapturedPacket packet) {
    server.send(packet);
  }
}
