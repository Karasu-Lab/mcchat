package com.karasu256.capture;

@FunctionalInterface
public interface CaptureSink {
  void accept(CapturedPacket packet);
}
