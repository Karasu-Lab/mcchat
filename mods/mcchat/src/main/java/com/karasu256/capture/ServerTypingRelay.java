package com.karasu256.capture;

import com.karasu256.net.TypingBroadcastPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class ServerTypingRelay {
  private ServerTypingRelay() {}

  public static void relay(ServerPlayer typer, boolean started) {
    TypingBroadcastPayload payload = new TypingBroadcastPayload(typer.getUUID(), started);
    for (ServerPlayer player : typer.level().getServer().getPlayerList().getPlayers()) {
      if (player != typer && ServerPlayNetworking.canSend(player, TypingBroadcastPayload.TYPE)) {
        ServerPlayNetworking.send(player, payload);
      }
    }
  }
}
