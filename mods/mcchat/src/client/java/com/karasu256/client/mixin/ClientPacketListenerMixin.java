package com.karasu256.client.mixin;

import com.karasu256.client.capture.ClientCapture;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
  @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
  private void chatnative$onPlayersAdded(
      ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
    for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.newEntries()) {
      ClientCapture.playerJoined(entry.profile());
    }
  }

  @Inject(method = "handlePlayerInfoRemove", at = @At("HEAD"))
  private void chatnative$onPlayersRemoved(
      ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
    ClientPacketListener self = (ClientPacketListener) (Object) this;
    for (UUID id : packet.profileIds()) {
      ClientCapture.playerLeft(self.getPlayerInfo(id));
    }
  }
}
