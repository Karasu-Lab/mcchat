package com.karasu256.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.WorldData;
import com.karasu256.mcchat.common.event.ChatReceivedEvent;
import com.karasu256.mcchat.common.event.CommandSentEvent;
import com.karasu256.mcchat.common.event.EventRegistry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TCPServerTest {
  private static final CapturedPacket SNAPSHOT =
      new CapturedPacket(
          CapturedPacket.Type.SNAPSHOT,
          Origin.CLIENT,
          WorldData.empty("localhost", "world"),
          Optional.empty());

  private final BlockingQueue<CapturedPacket> received = new LinkedBlockingQueue<>();
  private TCPServer server;

  @BeforeEach
  void setUp() throws Exception {
    server = new TCPServer(() -> SNAPSHOT, received::add, EventRegistry.common());
    server.start(0);
  }

  @AfterEach
  void tearDown() {
    server.close();
  }

  @Test
  void sendsSnapshotOnConnectThenBroadcastsEvents() throws Exception {
    try (Socket socket = new Socket("127.0.0.1", server.port());
        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
      socket.setSoTimeout(3000);
      assertEquals(Optional.of(SNAPSHOT), CapturedPacket.decode(reader.readLine()));

      CapturedPacket event =
          CapturedPacket.event(
              Origin.CLIENT,
              SNAPSHOT.world(),
              new ChatReceivedEvent(null, Origin.CLIENT, null, "hi"));
      server.send(event);

      assertEquals(Optional.of(event), CapturedPacket.decode(reader.readLine()));
    }
  }

  @Test
  void receivesPacketsAndIgnoresGarbage() throws Exception {
    CapturedPacket command =
        CapturedPacket.event(
            Origin.CLIENT,
            SNAPSHOT.world(),
            new CommandSentEvent(null, Origin.CLIENT, null, "time set day"));

    try (Socket socket = new Socket("127.0.0.1", server.port());
        Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
      writer.write("garbage\n");
      writer.write(command.encode() + "\n");
      writer.flush();

      CapturedPacket packet = received.poll(3, TimeUnit.SECONDS);
      assertNotNull(packet);
      assertEquals(command, packet);
      assertEquals(0, received.size());
    }
  }
}
