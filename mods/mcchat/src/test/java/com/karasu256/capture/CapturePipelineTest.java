package com.karasu256.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.ChatReceivedEvent;
import com.karasu256.mcchat.common.event.MessageEvent;
import com.karasu256.mcchat.common.event.PlayerJoinedEvent;
import com.karasu256.mcchat.common.event.PlayerLeftEvent;
import com.karasu256.mcchat.common.event.PlayerListEvent;
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

  private static Optional<String> content(CapturedEvent event) {
    return ((MessageEvent) event).content();
  }

  private ChatReceivedEvent chat(Origin origin, String text) {
    return new ChatReceivedEvent(pipeline, origin, ALEX, text);
  }

  @Test
  void preservesSubmissionOrderAndTrimsHistory() throws Exception {
    pipeline.setAuthority(Origin.CLIENT);
    for (int i = 0; i <= 100; i++) {
      chat(Origin.CLIENT, "m" + i).handle();
    }
    for (int i = 0; i <= 100; i++) {
      assertEquals(Optional.of("m" + i), content(next().event().get()));
    }

    List<CapturedEvent> history = pipeline.snapshot().world().context().history();
    assertEquals(100, history.size());
    assertEquals(Optional.of("m1"), content(history.get(0)));
    assertEquals(Optional.of("m100"), content(history.get(99)));
  }

  @Test
  void serverIsTheTruthWhenAuthorityIsServer() throws Exception {
    pipeline.reset("host", "world", Origin.SERVER);
    assertEquals(CapturedPacket.Type.RESET, next().type());

    chat(Origin.CLIENT, "cache").handle();
    chat(Origin.SERVER, "truth").handle();

    CapturedPacket packet = next();
    assertEquals(Origin.SERVER, packet.authority());
    assertEquals(Optional.of("truth"), content(packet.event().get()));
    assertTrue(out.isEmpty());
  }

  @Test
  void clientIsTheTruthWithoutAServer() throws Exception {
    pipeline.reset("host", "world", Origin.CLIENT);
    next();

    chat(Origin.SERVER, "ignored").handle();
    chat(Origin.CLIENT, "kept").handle();

    assertEquals(Optional.of("kept"), content(next().event().get()));
  }

  @Test
  void resetClearsTheDataTree() throws Exception {
    pipeline.reset("a", "one", Origin.CLIENT);
    new PlayerListEvent(pipeline, Origin.CLIENT, List.of(ALEX)).handle();
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
    new PlayerJoinedEvent(pipeline, Origin.CLIENT, ALEX).handle();
    next();
    assertEquals(List.of(ALEX), pipeline.snapshot().world().context().players());

    new PlayerLeftEvent(pipeline, Origin.CLIENT, ALEX).handle();
    next();
    assertTrue(pipeline.snapshot().world().context().players().isEmpty());
  }
}
