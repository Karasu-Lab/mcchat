package com.karasu256.mcchat.common.event;

import com.google.gson.annotations.SerializedName;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public abstract class CapturedEvent {
  private final UUID id;

  @SerializedName("timestamp")
  private final long timestampMillis;

  private final Origin origin;
  private final PlayerRef player;
  private transient EventContext context;

  protected CapturedEvent(EventContext context, Origin origin, PlayerRef player) {
    this(context, UUID.randomUUID(), System.currentTimeMillis(), origin, player);
  }

  protected CapturedEvent(
      EventContext context, UUID id, long timestampMillis, Origin origin, PlayerRef player) {
    this.context = context;
    this.id = id;
    this.timestampMillis = timestampMillis;
    this.origin = origin;
    this.player = player;
  }

  public UUID id() {
    return id;
  }

  public long timestampMillis() {
    return timestampMillis;
  }

  public Origin origin() {
    return origin;
  }

  public Optional<PlayerRef> player() {
    return Optional.ofNullable(player);
  }

  public EventContext context() {
    return context;
  }

  public void bind(EventContext context) {
    this.context = context;
  }

  public final String type() {
    EventType type = getClass().getAnnotation(EventType.class);
    if (type == null) {
      throw new IllegalStateException(getClass() + " has no @EventType");
    }
    return type.value();
  }

  public WorldContext applyTo(WorldContext world, int historySize) {
    return world.record(this, historySize);
  }

  public CapturedEvent mapPlayers(UnaryOperator<PlayerRef> mapper) {
    return this;
  }

  public List<PlayerRef> referencedPlayers() {
    return Collections.emptyList();
  }

  public void handle() {
    if (context != null) {
      context.accept(this);
    }
  }

  public boolean valid() {
    return id != null && origin != null && (player == null || player.valid());
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CapturedEvent
        && type().equals(((CapturedEvent) o).type())
        && encoded().equals(((CapturedEvent) o).encoded());
  }

  @Override
  public int hashCode() {
    return encoded().hashCode();
  }

  private String encoded() {
    return EventRegistry.common().gson().toJson(this, CapturedEvent.class);
  }
}
