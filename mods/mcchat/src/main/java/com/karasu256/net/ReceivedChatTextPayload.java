package com.karasu256.net;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.event.EventRegistry;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ReceivedChatTextPayload(UUID sender, ChatText text) implements FabricPacket {
  public static final PacketType<ReceivedChatTextPayload> TYPE =
      PacketType.create(
          new ResourceLocation(ChatNative.MOD_ID, "received_chat_text"),
          buf ->
              new ReceivedChatTextPayload(
                  buf.readUUID(),
                  EventRegistry.common().gson().fromJson(buf.readUtf(), ChatText.class)));

  @Override
  public void write(FriendlyByteBuf buf) {
    buf.writeUUID(sender);
    buf.writeUtf(EventRegistry.common().gson().toJson(text));
  }

  @Override
  public PacketType<?> getType() {
    return TYPE;
  }
}
