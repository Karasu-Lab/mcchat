package com.karasu256.net;

import com.karasu256.ChatNative;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TypingBroadcastPayload(UUID player, boolean started) implements CustomPacketPayload {
  public static final Type<TypingBroadcastPayload> TYPE =
      new Type<>(Identifier.fromNamespaceAndPath(ChatNative.MOD_ID, "typing_broadcast"));

  public static final StreamCodec<RegistryFriendlyByteBuf, TypingBroadcastPayload> CODEC =
      StreamCodec.composite(
          UUIDUtil.STREAM_CODEC,
          TypingBroadcastPayload::player,
          ByteBufCodecs.BOOL,
          TypingBroadcastPayload::started,
          TypingBroadcastPayload::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
