package com.karasu256.mcchat.common.component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class ChatTextComponent {
  public abstract String type();

  public abstract String plain();

  public List<UUID> mentioned() {
    return Collections.emptyList();
  }

  public Optional<UUID> replyTarget() {
    return Optional.empty();
  }

  public boolean notifies(UUID me, UUID sender) {
    return false;
  }

  public RawText stripped() {
    return new RawText(plain());
  }

  public boolean valid() {
    return true;
  }
}
