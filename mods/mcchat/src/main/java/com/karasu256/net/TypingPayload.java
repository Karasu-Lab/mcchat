package com.karasu256.net;

import com.karasu256.ChatNative;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record TypingPayload(boolean started) implements FabricPacket {
  public static final PacketType<TypingPayload> TYPE =
      PacketType.create(
          new ResourceLocation(ChatNative.MOD_ID, "typing"),
          buf -> new TypingPayload(buf.readBoolean()));

  @Override
  public void write(FriendlyByteBuf buf) {
    buf.writeBoolean(started);
  }

  @Override
  public PacketType<?> getType() {
    return TYPE;
  }
}
