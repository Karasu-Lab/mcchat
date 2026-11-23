package com.karasu256.mcchat.common.event;

import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.component.ChatText;
import java.util.Optional;

public abstract class MessageEvent extends CapturedEvent {
  private final ChatText text;

  protected MessageEvent(EventContext context, Origin origin, PlayerRef player, String content) {
    this(context, origin, player, content == null ? null : ChatText.raw(content));
  }

  protected MessageEvent(EventContext context, Origin origin, PlayerRef player, ChatText text) {
    super(context, origin, player);
    this.text = text;
  }

  public ChatText text() {
    return text == null ? ChatText.EMPTY : text;
  }

  public Optional<String> content() {
    return text == null ? Optional.empty() : Optional.of(text.plain());
  }

  @Override
  public boolean valid() {
    return super.valid() && (text == null || text.valid());
  }
}
