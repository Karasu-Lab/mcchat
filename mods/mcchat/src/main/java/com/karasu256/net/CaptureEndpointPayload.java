package com.karasu256.net;

import com.karasu256.ChatNative;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CaptureEndpointPayload(String host, int port) implements CustomPacketPayload {
  public static final Type<CaptureEndpointPayload> TYPE =
      new Type<>(Identifier.fromNamespaceAndPath(ChatNative.MOD_ID, "capture_endpoint"));

  public static final StreamCodec<RegistryFriendlyByteBuf, CaptureEndpointPayload> CODEC =
      StreamCodec.composite(
          ByteBufCodecs.STRING_UTF8,
          CaptureEndpointPayload::host,
          ByteBufCodecs.VAR_INT,
          CaptureEndpointPayload::port,
          CaptureEndpointPayload::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
