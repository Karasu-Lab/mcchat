package com.karasu256.gametest;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.WorldData;
import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.ChatReceivedEvent;
import com.karasu256.mcchat.common.event.ChatSentEvent;
import com.karasu256.mcchat.common.event.CommandExecutedEvent;
import com.karasu256.mcchat.common.event.CommandSentEvent;
import com.karasu256.mcchat.common.event.MessageEvent;
import com.karasu256.mcchat.common.event.PlayerJoinedEvent;
import com.karasu256.mcchat.common.event.PlayerListEvent;
import com.karasu256.net.CaptureEndpointPayload;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ChatNativeClientGameTest implements FabricClientGameTest {
  @Override
  public void runTest(ClientGameTestContext context) {
    try (TcpCapture capture = new TcpCapture(ChatNative.config().clientPort)) {
      serverDataFlowsThroughUpstream(context, capture);
      clientCacheFlowsWithoutUpstream(context, capture);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private void clientCacheFlowsWithoutUpstream(ClientGameTestContext context, TcpCapture capture) {
    ClientPlayNetworking.unregisterGlobalReceiver(CaptureEndpointPayload.TYPE.id());
    capture.clear();

    try (TestDedicatedServerContext server = context.worldBuilder().createServer();
        TestDedicatedServerConnection connection = server.connect()) {
      TestDedicatedServerConnection unused = connection;

      CapturedPacket reset = capture.await(context, p -> p.type() == CapturedPacket.Type.RESET);
      require(reset.authority() == Origin.CLIENT, "authority must be client without upstream");

      CapturedPacket list = capture.await(context, p -> hasType(p, PlayerListEvent.class));
      require(list.event().get().origin() == Origin.CLIENT, "player list must be client cache");
      CapturedPacket joined = capture.await(context, p -> hasType(p, PlayerJoinedEvent.class));
      require(joined.event().get().player().isPresent(), "join event must carry the player");

      context.runOnClient(client -> client.getConnection().sendChat("hello-cache"));
      CapturedPacket chat =
          capture.await(context, p -> hasContent(p, ChatSentEvent.class, "hello-cache"));
      require(chat.event().get().origin() == Origin.CLIENT, "chat must be client cache data");

      context.runOnClient(client -> client.getConnection().sendCommand("time set day"));
      CapturedPacket command =
          capture.await(context, p -> hasContent(p, CommandSentEvent.class, "time set day"));
      require(command.event().get().origin() == Origin.CLIENT, "command must be client cache data");

      capture.send(appPacket("from-app"));
      capture.await(context, p -> hasContent(p, ChatSentEvent.class, "from-app"));

      capture.clear();
    }

    try (TestDedicatedServerContext server = context.worldBuilder().createServer();
        TestDedicatedServerConnection connection = server.connect()) {
      TestDedicatedServerConnection unused = connection;
      capture.await(context, p -> p.type() == CapturedPacket.Type.RESET);
    }
  }

  private void serverDataFlowsThroughUpstream(ClientGameTestContext context, TcpCapture capture) {
    capture.clear();

    try (TestDedicatedServerContext server = context.worldBuilder().createServer();
        TestDedicatedServerConnection connection = server.connect()) {
      TestDedicatedServerConnection unused = connection;

      CapturedPacket snapshot =
          capture.await(
              context,
              p -> p.type() == CapturedPacket.Type.SNAPSHOT && p.authority() == Origin.SERVER);
      require(
          !snapshot.world().context().players().isEmpty(),
          "server snapshot must contain the connected player");
      capture.clear();

      context.runOnClient(client -> client.getConnection().sendCommand("time set day"));
      server.runCommand("time set night");
      context.waitTicks(40);
      require(
          capture.packets().stream().noneMatch(p -> hasType(p, CommandExecutedEvent.class)),
          "commands executed on the server must never reach the client capture");

      server.runCommand("say from-server");
      CapturedPacket said = capture.await(context, p -> contentContains(p, "from-server"));
      require(said.authority() == Origin.SERVER, "server data must carry server authority");
      require(said.event().get().origin() == Origin.SERVER, "server data must not be a cache");

      context.runOnClient(client -> client.getConnection().sendChat("hello-server"));
      CapturedPacket chat =
          capture.await(context, p -> hasContent(p, ChatReceivedEvent.class, "hello-server"));
      require(chat.event().get().origin() == Origin.SERVER, "chat must come from the server");

      context.waitTicks(20);
      boolean cacheLeaked =
          capture.packets().stream()
              .anyMatch(p ->
                  p.type() == CapturedPacket.Type.EVENT
                      && p.event().get().origin() == Origin.CLIENT);
      require(!cacheLeaked, "client cache must not be delivered while the server is the truth");
    }
  }

  private static CapturedPacket appPacket(String content) {
    return CapturedPacket.event(
        Origin.CLIENT,
        WorldData.empty("", ""),
        new ChatSentEvent(null, Origin.CLIENT, null, content));
  }

  private static boolean hasType(CapturedPacket packet, Class<? extends CapturedEvent> type) {
    return packet.type() == CapturedPacket.Type.EVENT && type.isInstance(packet.event().get());
  }

  private static boolean hasContent(
      CapturedPacket packet, Class<? extends MessageEvent> type, String content) {
    return hasType(packet, type)
        && ((MessageEvent) packet.event().get()).content().equals(Optional.of(content));
  }

  private static boolean contentContains(CapturedPacket packet, String text) {
    return packet.type() == CapturedPacket.Type.EVENT
        && packet.event().get() instanceof MessageEvent message
        && message.content().map(c -> c.contains(text)).orElse(false);
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static final class TcpCapture implements AutoCloseable {
    private final Socket socket;
    private final Writer writer;
    private final List<CapturedPacket> packets = new CopyOnWriteArrayList<>();

    TcpCapture(int port) throws IOException {
      socket = new Socket("127.0.0.1", port);
      writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
      Thread reader =
          new Thread(
              () -> {
                try (BufferedReader in =
                    new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                  String line;
                  while ((line = in.readLine()) != null) {
                    CapturedPacket.decode(line).ifPresent(packets::add);
                  }
                } catch (IOException ignored) {
                }
              },
              "chatnative-test-reader");
      reader.setDaemon(true);
      reader.start();
    }

    List<CapturedPacket> packets() {
      return packets;
    }

    void clear() {
      packets.clear();
    }

    void send(CapturedPacket packet) {
      try {
        writer.write(packet.encode() + "\n");
        writer.flush();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }

    CapturedPacket await(ClientGameTestContext context, Predicate<CapturedPacket> predicate) {
      context.waitFor(client -> packets.stream().anyMatch(predicate), 600);
      return packets.stream().filter(predicate).findFirst().orElseThrow();
    }

    @Override
    public void close() throws IOException {
      socket.close();
    }
  }
}
