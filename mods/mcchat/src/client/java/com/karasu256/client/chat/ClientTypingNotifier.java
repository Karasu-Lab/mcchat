package com.karasu256.client.chat;

import com.karasu256.client.capture.ClientCapture;
import com.karasu256.client.ui.ChatLog;
import com.karasu256.net.TypingPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientTypingNotifier {
  private static boolean typing;

  private ClientTypingNotifier() {}

  public static void edited(String text) {
    typing(!text.isEmpty());
  }

  public static void closed() {
    typing(false);
  }

  public static void typing(boolean started) {
    if (typing == started) {
      return;
    }
    typing = started;
    ChatLog.INSTANCE.typing(ClientCapture.sessionPlayer().uuid(), started);
    if (ClientPlayNetworking.canSend(TypingPayload.TYPE)) {
      ClientPlayNetworking.send(new TypingPayload(started));
    }
  }
}
