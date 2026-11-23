package com.karasu256.net;

import com.karasu256.ChatNative;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TypingPayload(boolean started) implements CustomPacketPayload {
  public static final Type<TypingPayload> TYPE =
      new Type<>(Identifier.fromNamespaceAndPath(ChatNative.MOD_ID, "typing"));

  public static final StreamCodec<RegistryFriendlyByteBuf, TypingPayload> CODEC =
      StreamCodec.composite(ByteBufCodecs.BOOL, TypingPayload::started, TypingPayload::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
