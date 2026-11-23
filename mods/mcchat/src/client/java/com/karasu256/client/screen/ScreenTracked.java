package com.karasu256.client.screen;

import com.karasu256.client.capture.ClientCapture;
import com.karasu256.mcchat.common.MinecraftScreen;

public interface ScreenTracked {
  MinecraftScreen chatnative$screen();

  default void chatnative$opened() {
    ClientCapture.screenChanged(chatnative$screen());
  }

  default void chatnative$closed() {
    ClientCapture.screenChanged(null);
  }
}
