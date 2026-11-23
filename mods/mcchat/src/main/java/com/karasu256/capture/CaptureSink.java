package com.karasu256.capture;

import com.karasu256.mcchat.common.CapturedPacket;

@FunctionalInterface
public interface CaptureSink {
  void accept(CapturedPacket packet);
}
