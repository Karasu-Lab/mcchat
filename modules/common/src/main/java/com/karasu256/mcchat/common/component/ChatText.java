package com.karasu256.mcchat.common.component;

import com.karasu256.mcchat.common.PlayerRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ChatText {
  public static final ChatText EMPTY = new ChatText(Collections.emptyList());

  private final List<ChatTextComponent> components;

  public ChatText(List<ChatTextComponent> components) {
    List<ChatTextComponent> merged = merge(components);
    this.components = merged.isEmpty() ? null : Collections.unmodifiableList(merged);
  }

  public static ChatText raw(String text) {
    return new ChatText(Collections.singletonList(new RawText(text)));
  }

  public List<ChatTextComponent> components() {
    return components == null ? Collections.emptyList() : components;
  }

  public String plain() {
    return components().stream().map(ChatTextComponent::plain).collect(Collectors.joining());
  }

  public List<UUID> mentioned() {
    LinkedHashSet<UUID> ids = new LinkedHashSet<>();
    components().forEach(c -> ids.addAll(c.mentioned()));
    return new ArrayList<>(ids);
  }

  public Optional<UUID> replyTarget() {
    return components().stream()
        .map(ChatTextComponent::replyTarget)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  public boolean notifies(UUID me, UUID sender) {
    return components().stream().anyMatch(c -> c.notifies(me, sender));
  }

  public ChatText withoutReplyUnless(Predicate<UUID> targetExists) {
    Optional<UUID> target = replyTarget();
    if (!target.isPresent() || targetExists.test(target.get())) {
      return this;
    }
    return new ChatText(
        components().stream()
            .filter(component -> !component.replyTarget().isPresent())
            .collect(Collectors.toList()));
  }

  public ChatText withReply(ReplyData reply) {
    List<ChatTextComponent> next = new ArrayList<>();
    next.add(reply);
    next.addAll(components());
    return new ChatText(next);
  }

  public ChatText stripped() {
    return new ChatText(
        components().stream().map(ChatTextComponent::stripped).collect(Collectors.toList()));
  }

  public ChatText replace(int start, int end, List<ChatTextComponent> insertion) {
    List<ChatTextComponent> before = new ArrayList<>();
    List<ChatTextComponent> after = new ArrayList<>();
    int offset = 0;
    for (ChatTextComponent component : components()) {
      int length = component.plain().length();
      int from = offset;
      int to = offset + length;
      offset = to;
      if (to <= start) {
        before.add(component);
      } else if (from >= end) {
        after.add(component);
      } else if (component instanceof RawText) {
        RawText raw = (RawText) component;
        if (start > from) {
          before.add(raw.slice(0, start - from));
        }
        if (end < to) {
          after.add(raw.slice(end - from, length));
        }
      }
    }
    List<ChatTextComponent> next = new ArrayList<>(before);
    next.addAll(insertion);
    next.addAll(after);
    return new ChatText(next);
  }

  public static ChatText parse(String raw, Collection<PlayerRef> players) {
    List<ChatTextComponent> parts = new ArrayList<>();
    StringBuilder buffer = new StringBuilder();
    int i = 0;
    while (i < raw.length()) {
      PlayerRef mentioned =
          raw.charAt(i) == MentionData.PREFIX && !isLetterOrDigit(raw, i - 1)
              ? match(raw, i + 1, players)
              : null;
      if (mentioned == null) {
        buffer.append(raw.charAt(i));
        i++;
        continue;
      }
      parts.add(new RawText(buffer.toString()));
      buffer.setLength(0);
      parts.add(new MentionData(mentioned.uuid(), mentioned.name()));
      i += 1 + mentioned.name().length();
    }
    parts.add(new RawText(buffer.toString()));
    return new ChatText(parts);
  }

  static boolean isLetterOrDigit(String text, int index) {
    return index >= 0 && index < text.length() && Character.isLetterOrDigit(text.charAt(index));
  }

  private static PlayerRef match(String raw, int from, Collection<PlayerRef> players) {
    PlayerRef best = null;
    for (PlayerRef player : players) {
      String name = player.name();
      boolean matches = raw.regionMatches(true, from, name, 0, name.length());
      if (matches && (best == null || name.length() > best.name().length())) {
        best = player;
      }
    }
    return best;
  }

  private static List<ChatTextComponent> merge(List<ChatTextComponent> components) {
    List<ChatTextComponent> merged = new ArrayList<>();
    for (ChatTextComponent component : components) {
      if (component instanceof RawText) {
        RawText raw = (RawText) component;
        if (raw.isEmpty()) {
          continue;
        }
        int last = merged.size() - 1;
        if (last >= 0 && merged.get(last) instanceof RawText) {
          merged.set(last, ((RawText) merged.get(last)).plus(raw));
          continue;
        }
      }
      merged.add(component);
    }
    return merged;
  }

  public boolean valid() {
    return components().stream().allMatch(c -> c != null && c.valid());
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ChatText && components().equals(((ChatText) o).components());
  }

  @Override
  public int hashCode() {
    return Objects.hash(components());
  }
}
