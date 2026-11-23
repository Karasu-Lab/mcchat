package com.karasu256.mcchat.common;

import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.EventContext;
import com.karasu256.mcchat.common.event.EventRegistry;
import java.util.Objects;
import java.util.Optional;

public final class CapturedPacket {
  public enum Type {
    SNAPSHOT,
    RESET,
    EVENT,
    PING,
    STATUS
  }

  private final Type type;
  private final Origin authority;
  private final WorldData world;
  private final CapturedEvent event;

  public CapturedPacket(
      Type type, Origin authority, WorldData world, Optional<? extends CapturedEvent> event) {
    this.type = type;
    this.authority = authority;
    this.world = world;
    this.event = event.orElse(null);
  }

  public Type type() {
    return type;
  }

  public Origin authority() {
    return authority;
  }

  public WorldData world() {
    return world;
  }

  public Optional<CapturedEvent> event() {
    return Optional.ofNullable(event);
  }

  public void bind(EventContext context) {
    world.context().bind(context);
    if (event != null) {
      event.bind(context);
    }
  }

  public static CapturedPacket event(Origin authority, WorldData world, CapturedEvent event) {
    return new CapturedPacket(Type.EVENT, authority, world.withoutContext(), Optional.of(event));
  }

  public String encode() {
    return EventRegistry.common().gson().toJson(this);
  }

  public static Optional<CapturedPacket> decode(String line) {
    return decode(line, EventRegistry.common());
  }

  public static Optional<CapturedPacket> decode(String line, EventRegistry registry) {
    try {
      CapturedPacket packet = registry.gson().fromJson(line, CapturedPacket.class);
      boolean valid =
          packet != null
              && packet.type != null
              && packet.authority != null
              && packet.world != null
              && packet.world.valid()
              && (packet.event == null || packet.event.valid());
      return valid ? Optional.of(packet) : Optional.empty();
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CapturedPacket
        && type == ((CapturedPacket) o).type
        && authority == ((CapturedPacket) o).authority
        && world.equals(((CapturedPacket) o).world)
        && event().equals(((CapturedPacket) o).event());
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, authority, world, event());
  }
}
