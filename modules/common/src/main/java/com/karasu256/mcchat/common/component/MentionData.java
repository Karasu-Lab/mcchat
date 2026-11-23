package com.karasu256.mcchat.common.component;

import com.karasu256.mcchat.common.PlayerRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class MentionData extends ReplyData {
  public static final char PREFIX = '@';

  private final String name;

  public MentionData(UUID player, String name) {
    super(null, Collections.singletonList(player));
    this.name = name;
  }

  public UUID player() {
    return mentioned().get(0);
  }

  public String name() {
    return name;
  }

  @Override
  public String type() {
    return "mention";
  }

  @Override
  public String plain() {
    return PREFIX + name;
  }

  @Override
  public boolean valid() {
    return super.valid() && name != null && !mentioned().isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof MentionData && super.equals(o) && Objects.equals(name, ((MentionData) o).name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), name);
  }

  public static Optional<Completion> complete(
      ChatText text, int cursor, Collection<PlayerRef> players) {
    String plain = text.plain();
    if (cursor < 0 || cursor > plain.length()) {
      return Optional.empty();
    }
    int rangeStart = -1;
    int offset = 0;
    for (ChatTextComponent component : text.components()) {
      int length = component.plain().length();
      if (component instanceof RawText && cursor >= offset && cursor <= offset + length) {
        rangeStart = offset;
        break;
      }
      offset += length;
    }
    if (rangeStart < 0) {
      return Optional.empty();
    }
    int at = cursor - 1;
    while (at >= rangeStart
        && plain.charAt(at) != PREFIX
        && !Character.isWhitespace(plain.charAt(at))) {
      at--;
    }
    if (at < rangeStart
        || plain.charAt(at) != PREFIX
        || (at > rangeStart && ChatText.isLetterOrDigit(plain, at - 1))) {
      return Optional.empty();
    }
    String prefix = plain.substring(at + 1, cursor);
    Map<UUID, PlayerRef> unique = new LinkedHashMap<>();
    for (PlayerRef player : players) {
      if (player.name().regionMatches(true, 0, prefix, 0, prefix.length())) {
        unique.putIfAbsent(player.uuid(), player);
      }
    }
    List<PlayerRef> candidates = new ArrayList<>(unique.values());
    candidates.sort(Comparator.comparing(p -> p.name().toLowerCase()));
    if (candidates.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new Completion(text, at, cursor, candidates));
  }

  public static final class Completion {
    private final ChatText text;
    private final int start;
    private final int end;
    private final List<PlayerRef> candidates;

    private Completion(ChatText text, int start, int end, List<PlayerRef> candidates) {
      this.text = text;
      this.start = start;
      this.end = end;
      this.candidates = candidates;
    }

    public List<PlayerRef> candidates() {
      return candidates;
    }

    public Inserted insert(PlayerRef player) {
      String plain = text.plain();
      boolean space = end >= plain.length() || !Character.isWhitespace(plain.charAt(end));
      List<ChatTextComponent> insertion = new ArrayList<>();
      insertion.add(new MentionData(player.uuid(), player.name()));
      if (space) {
        insertion.add(new RawText(" "));
      }
      int cursor = start + 1 + player.name().length() + (space ? 1 : 0);
      return new Inserted(text.replace(start, end, insertion), cursor);
    }
  }

  public static final class Inserted {
    private final ChatText text;
    private final int cursor;

    private Inserted(ChatText text, int cursor) {
      this.text = text;
      this.cursor = cursor;
    }

    public ChatText text() {
      return text;
    }

    public int cursor() {
      return cursor;
    }
  }
}
