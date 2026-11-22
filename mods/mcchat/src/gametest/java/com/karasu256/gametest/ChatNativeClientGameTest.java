package com.karasu256.gametest;

import com.karasu256.ChatNative;
import com.karasu256.capture.CapturedData;
import com.karasu256.capture.CapturedData.Kind;
import com.karasu256.capture.CapturedPacket;
import com.karasu256.capture.Origin;
import com.karasu256.capture.WorldData;
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

public class ChatNativeClientGameTest implements FabricClientGameTest {
  private static final int WAIT_TICKS = 600;

  @Override
  public void runTest(ClientGameTestContext context) {
    try (TcpCapture capture = new TcpCapture(ChatNative.config().clientPort)) {
      clientCacheFlowsWithoutUpstream(context, capture);
      serverDataFlowsThroughUpstream(context, capture);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private void clientCacheFlowsWithoutUpstream(ClientGameTestContext context, TcpCapture capture) {
    ChatNative.config().useUpstream = false;
    capture.clear();

    try (TestDedicatedServerContext server = context.worldBuilder().createServer();
        TestDedicatedServerConnection connection = server.connect()) {
      TestDedicatedServerConnection unused = connection;

      CapturedPacket reset = capture.await(context, p -> p.type() == CapturedPacket.Type.RESET);
      require(reset.authority() == Origin.CLIENT, "authority must be client without upstream");

      CapturedPacket list = capture.await(context, p -> hasKind(p, Kind.PLAYER_LIST));
      require(list.data().get().origin() == Origin.CLIENT, "player list must be client cache");
      CapturedPacket joined = capture.await(context, p -> hasKind(p, Kind.PLAYER_JOINED));
      require(joined.data().get().player().isPresent(), "join event must carry the player");

      context.runOnClient(client -> client.getConnection().sendChat("hello-cache"));
      CapturedPacket chat =
          capture.await(context, p -> hasContent(p, Kind.CHAT_SENT, "hello-cache"));
      require(chat.data().get().origin() == Origin.CLIENT, "chat must be client cache data");

      context.runOnClient(client -> client.getConnection().sendCommand("time set day"));
      CapturedPacket command =
          capture.await(context, p -> hasContent(p, Kind.COMMAND_SENT, "time set day"));
      require(command.data().get().origin() == Origin.CLIENT, "command must be client cache data");

      capture.send(appPacket(Kind.CHAT_SENT, "from-app"));
      capture.await(context, p -> hasContent(p, Kind.CHAT_SENT, "from-app"));

      capture.clear();
    }

    try (TestDedicatedServerContext server = context.worldBuilder().createServer();
        TestDedicatedServerConnection connection = server.connect()) {
      TestDedicatedServerConnection unused = connection;
      capture.await(context, p -> p.type() == CapturedPacket.Type.RESET);
    }
  }

  private void serverDataFlowsThroughUpstream(ClientGameTestContext context, TcpCapture capture) {
    ChatNative.config().useUpstream = true;
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
          capture.packets().stream().noneMatch(p -> hasKind(p, Kind.COMMAND_EXECUTED)),
          "commands executed on the server must never reach the client capture");

      server.runCommand("say from-server");
      CapturedPacket said =
          capture.await(context, p -> contentContains(p, "from-server"));
      require(said.authority() == Origin.SERVER, "server data must carry server authority");
      require(said.data().get().origin() == Origin.SERVER, "server data must not be a cache");

      context.runOnClient(client -> client.getConnection().sendChat("hello-server"));
      CapturedPacket chat =
          capture.await(context, p -> hasContent(p, Kind.CHAT_RECEIVED, "hello-server"));
      require(chat.data().get().origin() == Origin.SERVER, "chat must come from the server");

      context.waitTicks(20);
      boolean cacheLeaked =
          capture.packets().stream()
              .anyMatch(
                  p ->
                      p.type() == CapturedPacket.Type.EVENT
                          && p.data().get().origin() == Origin.CLIENT);
      require(!cacheLeaked, "client cache must not be delivered while the server is the truth");
    }
  }

  private static CapturedPacket appPacket(Kind kind, String content) {
    return CapturedPacket.event(
        Origin.CLIENT,
        WorldData.empty("", ""),
        CapturedData.of(Origin.CLIENT, kind, null, content));
  }

  private static boolean hasKind(CapturedPacket packet, Kind kind) {
    return packet.type() == CapturedPacket.Type.EVENT && packet.data().get().kind() == kind;
  }

  private static boolean hasContent(CapturedPacket packet, Kind kind, String content) {
    return packet.type() == CapturedPacket.Type.EVENT
        && packet.data().get().kind() == kind
        && packet.data().get().content().equals(Optional.of(content));
  }

  private static boolean contentContains(CapturedPacket packet, String text) {
    return packet.type() == CapturedPacket.Type.EVENT
        && packet.data().get().content().map(c -> c.contains(text)).orElse(false);
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
      context.waitFor(client -> packets.stream().anyMatch(predicate), WAIT_TICKS);
      return packets.stream().filter(predicate).findFirst().orElseThrow();
    }

    @Override
    public void close() throws IOException {
      socket.close();
    }
  }
}
