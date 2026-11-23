package com.karasu256.mixin;

import com.karasu256.capture.ServerCapture;
import com.karasu256.capture.ServerChatTextProcessor;
import com.karasu256.net.ChatTextPayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 900)
public abstract class ServerGamePacketListenerImplMixin {
  @Shadow public ServerPlayer player;

  @Inject(method = "handleChatCommand", at = @At("HEAD"))
  private void chatnative$onCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
    ServerCapture.commandExecuted(player, packet.command());
  }

  @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
  private void chatnative$holdChatText(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
    if (packet.payload() instanceof ChatTextPayload payload) {
      ServerChatTextProcessor.hold(player, payload.text());
      ci.cancel();
    }
  }
}
