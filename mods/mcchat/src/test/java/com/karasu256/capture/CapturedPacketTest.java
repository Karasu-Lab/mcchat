package com.karasu256.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.karasu256.capture.CapturedData.Kind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CapturedPacketTest {
  private static final PlayerRef PLAYER =
      new PlayerRef(
          UUID.fromString("11111111-2222-3333-4444-555555555555"),
          "Steve",
          Optional.of(new PlayerRef.Skin("tex", Optional.of("sig"))));

  @Test
  void eventRoundTrips() {
    CapturedData data = CapturedData.of(Origin.SERVER, Kind.CHAT_RECEIVED, PLAYER, "hello");
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
            .apply(CapturedData.playerList(Origin.CLIENT, List.of(PLAYER)), 10)
            .apply(CapturedData.of(Origin.CLIENT, Kind.COMMAND_SENT, PLAYER, "time set day"), 10);
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
