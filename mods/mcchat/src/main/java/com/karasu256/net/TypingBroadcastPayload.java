package com.karasu256.net;

import com.karasu256.ChatNative;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record TypingBroadcastPayload(UUID player, boolean started) implements FabricPacket {
  public static final PacketType<TypingBroadcastPayload> TYPE =
      PacketType.create(
          new ResourceLocation(ChatNative.MOD_ID, "typing_broadcast"),
          buf -> new TypingBroadcastPayload(buf.readUUID(), buf.readBoolean()));

  @Override
  public void write(FriendlyByteBuf buf) {
    buf.writeUUID(player);
    buf.writeBoolean(started);
  }

  @Override
  public PacketType<?> getType() {
    return TYPE;
  }
}
