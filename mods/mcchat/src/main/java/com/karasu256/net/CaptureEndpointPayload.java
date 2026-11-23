package com.karasu256.net;

import com.karasu256.ChatNative;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CaptureEndpointPayload(String host, int port) implements FabricPacket {
  public static final PacketType<CaptureEndpointPayload> TYPE =
      PacketType.create(
          new ResourceLocation(ChatNative.MOD_ID, "capture_endpoint"),
          buf -> new CaptureEndpointPayload(buf.readUtf(), buf.readVarInt()));

  @Override
  public void write(FriendlyByteBuf buf) {
    buf.writeUtf(host);
    buf.writeVarInt(port);
  }

  @Override
  public PacketType<?> getType() {
    return TYPE;
  }
}
