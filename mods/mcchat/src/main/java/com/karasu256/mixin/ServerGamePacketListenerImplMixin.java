package com.karasu256.mixin;

import com.karasu256.capture.ServerCapture;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
  @Shadow public ServerPlayer player;

  @Inject(method = "handleChatCommand", at = @At("HEAD"))
  private void chatnative$onCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
    ServerCapture.commandExecuted(player, packet.command());
  }
}
