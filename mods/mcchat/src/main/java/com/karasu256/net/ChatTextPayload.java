package com.karasu256.net;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.event.EventRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChatTextPayload(ChatText text) implements CustomPacketPayload {
  public static final Type<ChatTextPayload> TYPE =
      new Type<>(Identifier.fromNamespaceAndPath(ChatNative.MOD_ID, "chat_text"));

  public static final StreamCodec<RegistryFriendlyByteBuf, ChatTextPayload> CODEC =
      ByteBufCodecs.STRING_UTF8.map(
          json -> new ChatTextPayload(EventRegistry.common().gson().fromJson(json, ChatText.class)),
          payload -> EventRegistry.common().gson().toJson(payload.text()))
          .cast();

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
