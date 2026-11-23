package com.karasu256.client.ui;

import com.karasu256.client.chat.ReceivedChat;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import com.karasu256.mcchat.common.component.ReplyData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;

public final class ChatLog {
  public static final ChatLog INSTANCE = new ChatLog();

  private static final int HISTORY_LIMIT = 500;

  public interface Item {}

  public record Message(
      UUID id,
      PlayerRef sender,
      ChatText text,
      Component component,
      long timestampMillis,
      int addedTick,
      boolean own,
      boolean mention)
      implements Item {}

  public record Divider(String worldId, String worldName) implements Item {}

  private final List<Item> items = new ArrayList<>();
  private final Set<UUID> typing = new LinkedHashSet<>();
  private int version;
  private Message replyTarget;
  private boolean replyMention = true;

  private ChatLog() {}

  public List<Item> items() {
    return items;
  }

  public int version() {
    return version;
  }

  public List<UUID> typing() {
    return new ArrayList<>(typing);
  }

  public Message replyTarget() {
    return replyTarget;
  }

  public boolean replyMention() {
    return replyMention;
  }

  public void received(ReceivedChat chat, UUID self, int tick) {
    boolean own = chat.sender() != null && chat.sender().uuid().equals(self);
    items.add(
        new Message(
            chat.id(),
            chat.sender(),
            chat.text(),
            chat.component(),
            chat.timestampMillis(),
            tick,
            own,
            chat.mention()));
    if (chat.sender() != null) {
      typing.remove(chat.sender().uuid());
    }
    trim();
    version++;
  }

  public void worldJoined(String worldId, String worldName) {
    if (!items.isEmpty()) {
      items.add(new Divider(worldId, worldName));
      trim();
    }
    replyTarget = null;
    typing.clear();
    version++;
  }

  public void typing(UUID player, boolean started) {
    if (started ? typing.add(player) : typing.remove(player)) {
      version++;
    }
  }

  public void clear() {
    items.clear();
    replyTarget = null;
    version++;
  }

  public void clearTyping() {
    if (!typing.isEmpty()) {
      typing.clear();
      version++;
    }
  }

  public Optional<Message> find(UUID id) {
    for (Item item : items) {
      if (item instanceof Message message && message.id().equals(id)) {
        return Optional.of(message);
      }
    }
    return Optional.empty();
  }

  public boolean replyable(Message target) {
    String world = null;
    String current = null;
    boolean found = false;
    for (Item item : items) {
      if (item instanceof Divider divider) {
        current = divider.worldId();
        if (!found) {
          world = current;
        }
      } else if (item == target) {
        found = true;
      }
    }
    return found && java.util.Objects.equals(world, current);
  }

  public void toggleReply(Message target) {
    if (replyTarget == target) {
      replyTarget = null;
    } else if (replyable(target)) {
      replyTarget = target;
      replyMention = true;
    }
    version++;
  }

  public void toggleMention() {
    replyMention = !replyMention;
  }

  public void cancelReply() {
    replyTarget = null;
    version++;
  }

  public ChatText attachReply(ChatText text) {
    Message target = replyTarget;
    if (target == null) {
      return text;
    }
    replyTarget = null;
    version++;
    UUID targetSender = target.sender() == null ? null : target.sender().uuid();
    return text.withReply(ReplyData.reply(target.id(), targetSender, replyMention));
  }

  private void trim() {
    while (items.size() > HISTORY_LIMIT) {
      items.remove(0);
    }
  }
}
