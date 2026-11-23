package com.karasu256.mcchat.common;

import java.util.function.Consumer;

public interface PacketChannel extends AutoCloseable {
  void send(CapturedPacket packet);

  void setListener(Consumer<CapturedPacket> listener);

  @Override
  void close();
}
