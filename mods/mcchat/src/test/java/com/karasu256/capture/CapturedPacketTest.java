package com.karasu256.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;
import com.karasu256.mcchat.common.WorldData;
import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.ChatReceivedEvent;
import com.karasu256.mcchat.common.event.CommandSentEvent;
import com.karasu256.mcchat.common.event.PlayerListEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CapturedPacketTest {
  private static final PlayerRef PLAYER =
      new PlayerRef(
          UUID.fromString("11111111-2222-3333-4444-555555555555"),
          "Steve",
          Optional.of(new PlayerRef.Skin(new byte[] {1, 2, 3})));

  @Test
  void eventRoundTrips() {
    CapturedEvent data = new ChatReceivedEvent(null, Origin.SERVER, PLAYER, "hello");
    CapturedPacket packet =
        CapturedPacket.event(Origin.SERVER, WorldData.empty("example.com", "main"), data);

    String line = packet.encode();

    assertTrue(!line.contains("\n"));
    assertEquals(Optional.of(packet), CapturedPacket.decode(line));
  }

  @Test
  void snapshotRoundTripsWithContext() {
    WorldContext context =
        WorldContext.EMPTY
            .apply(new PlayerListEvent(null, Origin.CLIENT, List.of(PLAYER)), 10)
            .apply(new CommandSentEvent(null, Origin.CLIENT, PLAYER, "time set day"), 10);
    CapturedPacket packet =
        new CapturedPacket(
            CapturedPacket.Type.SNAPSHOT,
            Origin.CLIENT,
            new WorldData("localhost", "world", context),
            Optional.empty());

    assertEquals(Optional.of(packet), CapturedPacket.decode(packet.encode()));
  }

  @Test
  void invalidLinesAreRejected() {
    assertTrue(CapturedPacket.decode("not json").isEmpty());
    assertTrue(CapturedPacket.decode("{\"type\":\"bogus\"}").isEmpty());
  }
}
