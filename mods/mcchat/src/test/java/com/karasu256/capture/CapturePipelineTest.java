package com.karasu256.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.karasu256.capture.CapturedData.Kind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CapturePipelineTest {
  private static final PlayerRef ALEX = new PlayerRef(UUID.randomUUID(), "Alex", Optional.empty());

  private CapturePipeline pipeline;
  private final BlockingQueue<CapturedPacket> out = new LinkedBlockingQueue<>();

  @BeforeEach
  void setUp() {
    pipeline = new CapturePipeline();
    pipeline.addSink(out::add);
  }

  @AfterEach
  void tearDown() {
    pipeline.close();
  }

  private CapturedPacket next() throws InterruptedException {
    CapturedPacket packet = out.poll(2, TimeUnit.SECONDS);
    assertNotNull(packet, "expected a packet");
    return packet;
  }

  @Test
  void preservesSubmissionOrderAndTrimsHistory() throws Exception {
    pipeline.setAuthority(Origin.CLIENT);
    for (int i = 0; i <= 100; i++) {
      pipeline.submit(CapturedData.of(Origin.CLIENT, Kind.CHAT_RECEIVED, ALEX, "m" + i));
    }
    for (int i = 0; i <= 100; i++) {
      assertEquals(Optional.of("m" + i), next().data().get().content());
    }

    List<CapturedData> history = pipeline.snapshot().world().context().history();
    assertEquals(100, history.size());
    assertEquals(Optional.of("m1"), history.get(0).content());
    assertEquals(Optional.of("m100"), history.get(99).content());
  }

  @Test
  void serverIsTheTruthWhenAuthorityIsServer() throws Exception {
    pipeline.reset("host", "world", Origin.SERVER);
    assertEquals(CapturedPacket.Type.RESET, next().type());

    pipeline.submit(CapturedData.of(Origin.CLIENT, Kind.CHAT_RECEIVED, ALEX, "cache"));
    pipeline.submit(CapturedData.of(Origin.SERVER, Kind.CHAT_RECEIVED, ALEX, "truth"));

    CapturedPacket packet = next();
    assertEquals(Origin.SERVER, packet.authority());
    assertEquals(Optional.of("truth"), packet.data().get().content());
    assertTrue(out.isEmpty());
  }

  @Test
  void clientIsTheTruthWithoutAServer() throws Exception {
    pipeline.reset("host", "world", Origin.CLIENT);
    next();

    pipeline.submit(CapturedData.of(Origin.SERVER, Kind.CHAT_RECEIVED, ALEX, "ignored"));
    pipeline.submit(CapturedData.of(Origin.CLIENT, Kind.CHAT_RECEIVED, ALEX, "kept"));

    assertEquals(Optional.of("kept"), next().data().get().content());
  }

  @Test
  void resetClearsTheDataTree() throws Exception {
    pipeline.reset("a", "one", Origin.CLIENT);
    pipeline.submit(CapturedData.playerList(Origin.CLIENT, List.of(ALEX)));
    next();
    next();
    assertEquals(1, pipeline.snapshot().world().context().players().size());

    pipeline.reset("b", "two", Origin.CLIENT);
    CapturedPacket reset = next();

    assertEquals(CapturedPacket.Type.RESET, reset.type());
    assertEquals("two", reset.world().worldName());
    assertTrue(pipeline.snapshot().world().context().players().isEmpty());
  }

  @Test
  void tracksPlayerJoinAndLeave() throws Exception {
    pipeline.reset("h", "w", Origin.CLIENT);
    next();
    pipeline.submit(CapturedData.of(Origin.CLIENT, Kind.PLAYER_JOINED, ALEX, null));
    next();
    assertEquals(List.of(ALEX), pipeline.snapshot().world().context().players());

    pipeline.submit(CapturedData.of(Origin.CLIENT, Kind.PLAYER_LEFT, ALEX, null));
    next();
    assertTrue(pipeline.snapshot().world().context().players().isEmpty());
  }
}
