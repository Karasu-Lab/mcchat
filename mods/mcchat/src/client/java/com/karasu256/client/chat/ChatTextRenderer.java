package com.karasu256.client.chat;

import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.component.ChatTextComponent;
import com.karasu256.mcchat.common.component.MentionData;
import com.karasu256.mcchat.common.component.RawText;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EntityType;

public final class ChatTextRenderer {
  private ChatTextRenderer() {}

  public static HoverEvent.EntityTooltipInfo playerInfo(UUID id, String name) {
    return new HoverEvent.EntityTooltipInfo(EntityType.PLAYER, id, Component.literal(name));
  }

  public static Component render(ChatText text) {
    MutableComponent out = Component.empty();
    for (ChatTextComponent part : text.components()) {
      if (part instanceof MentionData mention) {
        HoverEvent hover =
            new HoverEvent(
                HoverEvent.Action.SHOW_ENTITY, playerInfo(mention.player(), mention.name()));
        out.append(
            Component.literal(part.plain())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withHoverEvent(hover)));
      } else if (part instanceof RawText) {
        out.append(Component.literal(part.plain()));
      }
    }
    return out;
  }
}
