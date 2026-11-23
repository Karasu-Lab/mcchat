package com.karasu256.mcchat.common;

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
  private final CapturedData data;

  public CapturedPacket(Type type, Origin authority, WorldData world, Optional<CapturedData> data) {
    this.type = type;
    this.authority = authority;
    this.world = world;
    this.data = data.orElse(null);
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

  public Optional<CapturedData> data() {
    return Optional.ofNullable(data);
  }

  public static CapturedPacket event(Origin authority, WorldData world, CapturedData data) {
    return new CapturedPacket(Type.EVENT, authority, world.withoutContext(), Optional.of(data));
  }

  public String encode() {
    return Json.GSON.toJson(this);
  }

  public static Optional<CapturedPacket> decode(String line) {
    try {
      CapturedPacket packet = Json.GSON.fromJson(line, CapturedPacket.class);
      boolean valid =
          packet != null
              && packet.type != null
              && packet.authority != null
              && packet.world != null
              && packet.world.valid()
              && (packet.data == null || packet.data.valid());
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
        && data().equals(((CapturedPacket) o).data());
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, authority, world, data());
  }
}
