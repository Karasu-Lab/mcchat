package com.karasu256.mcchat.common.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ReplyData extends ChatTextComponent {
  private final UUID targetId;
  private final List<UUID> mentioned;

  public ReplyData(UUID targetId, List<UUID> mentioned) {
    this.targetId = targetId;
    this.mentioned = mentioned.isEmpty() ? null : Collections.unmodifiableList(new ArrayList<>(mentioned));
  }

  public static ReplyData reply(UUID targetId, UUID targetSender, boolean mention) {
    return new ReplyData(
        targetId,
        mention && targetSender != null
            ? Collections.singletonList(targetSender)
            : Collections.emptyList());
  }

  public Optional<UUID> targetId() {
    return Optional.ofNullable(targetId);
  }

  public boolean isReply() {
    return targetId != null;
  }

  @Override
  public Optional<UUID> replyTarget() {
    return targetId();
  }

  @Override
  public boolean notifies(UUID me, UUID sender) {
    return me != null && !me.equals(sender) && mentioned().contains(me);
  }

  @Override
  public String type() {
    return "reply";
  }

  @Override
  public String plain() {
    return "";
  }

  @Override
  public List<UUID> mentioned() {
    return mentioned == null ? Collections.emptyList() : mentioned;
  }

  @Override
  public boolean valid() {
    return mentioned().stream().allMatch(Objects::nonNull);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ReplyData
        && type().equals(((ReplyData) o).type())
        && Objects.equals(targetId, ((ReplyData) o).targetId)
        && mentioned().equals(((ReplyData) o).mentioned());
  }

  @Override
  public int hashCode() {
    return Objects.hash(type(), targetId, mentioned());
  }
}
