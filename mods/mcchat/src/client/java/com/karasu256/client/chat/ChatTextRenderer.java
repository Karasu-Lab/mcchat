package com.karasu256.client.chat;

import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.component.ChatTextComponent;
import com.karasu256.mcchat.common.component.MentionData;
import com.karasu256.mcchat.common.component.RawText;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EntityTypes;

public final class ChatTextRenderer {
  private ChatTextRenderer() {}

  public static HoverEvent.EntityTooltipInfo playerInfo(UUID id, String name) {
    return new HoverEvent.EntityTooltipInfo(EntityTypes.PLAYER, id, Component.literal(name));
  }

  private static Style mentionStyle(MentionData mention) {
    HoverEvent hover = new HoverEvent.ShowEntity(playerInfo(mention.player(), mention.name()));
    return Style.EMPTY.withColor(ChatFormatting.GREEN).withHoverEvent(hover);
  }

  public static Component render(ChatText text) {
    MutableComponent out = Component.empty();
    for (ChatTextComponent part : text.components()) {
      if (part instanceof MentionData mention) {
        out.append(Component.literal(part.plain()).withStyle(mentionStyle(mention)));
      } else if (part instanceof RawText) {
        out.append(Component.literal(part.plain()));
      }
    }
    return out;
  }

  public static Component render(ChatText text, Component original) {
    String plain = text.plain();
    if (!original.getString().equals(plain)) {
      return original;
    }
    MentionData[] mentions = new MentionData[plain.length()];
    int offset = 0;
    for (ChatTextComponent part : text.components()) {
      int length = part.plain().length();
      if (part instanceof MentionData mention) {
        Arrays.fill(mentions, offset, offset + length, mention);
      }
      offset += length;
    }
    MutableComponent out = Component.empty();
    int[] position = {0};
    original.visit(
        (style, content) -> {
          int start = 0;
          while (start < content.length()) {
            MentionData mention = mentions[position[0] + start];
            int end = start + 1;
            while (end < content.length() && mentions[position[0] + end] == mention) {
              end++;
            }
            Style styled = mention == null ? style : mentionStyle(mention).applyTo(style);
            out.append(Component.literal(content.substring(start, end)).withStyle(styled));
            start = end;
          }
          position[0] += content.length();
          return Optional.empty();
        },
        Style.EMPTY);
    return out;
  }
}
