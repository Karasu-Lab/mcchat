package com.karasu256.net;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.event.EventRegistry;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReceivedChatTextPayload(UUID messageId, UUID sender, ChatText text)
    implements CustomPacketPayload {
  public static final Type<ReceivedChatTextPayload> TYPE =
      new Type<>(Identifier.fromNamespaceAndPath(ChatNative.MOD_ID, "received_chat_text"));

  private static final StreamCodec<RegistryFriendlyByteBuf, ChatText> TEXT_CODEC =
      ByteBufCodecs.STRING_UTF8
          .<RegistryFriendlyByteBuf>cast()
          .map(
              json -> EventRegistry.common().gson().fromJson(json, ChatText.class),
              text -> EventRegistry.common().gson().toJson(text));

  public static final StreamCodec<RegistryFriendlyByteBuf, ReceivedChatTextPayload> CODEC =
      StreamCodec.composite(
          UUIDUtil.STREAM_CODEC,
          ReceivedChatTextPayload::messageId,
          UUIDUtil.STREAM_CODEC,
          ReceivedChatTextPayload::sender,
          TEXT_CODEC,
          ReceivedChatTextPayload::text,
          ReceivedChatTextPayload::new);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
