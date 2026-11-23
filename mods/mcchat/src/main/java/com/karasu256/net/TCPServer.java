package com.karasu256.net;

import com.karasu256.ChatNative;
import com.karasu256.Threads;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.PacketChannel;
import com.karasu256.mcchat.common.event.EventRegistry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TCPServer implements PacketChannel {
  private final Supplier<CapturedPacket> snapshot;
  private final EventRegistry registry;
  private volatile Consumer<CapturedPacket> handler;
  private final Set<Connection> clients = ConcurrentHashMap.newKeySet();
  private final Set<Connection> upstreams = ConcurrentHashMap.newKeySet();

  private volatile ServerSocket serverSocket;
  private volatile boolean closed;
  private volatile Thread upstreamThread;

  public TCPServer(
      Supplier<CapturedPacket> snapshot,
      Consumer<CapturedPacket> handler,
      EventRegistry registry) {
    this.snapshot = snapshot;
    this.handler = handler;
    this.registry = registry;
  }

  public void start(int port) throws IOException {
    ServerSocket socket = new ServerSocket();
    socket.setReuseAddress(true);
    socket.bind(new InetSocketAddress(port));
    serverSocket = socket;
    Threads.start("chatnative-tcp-accept-" + port, () -> acceptLoop(socket));
    List<String> addresses = classCAddresses();
    if (addresses.isEmpty()) {
      ChatNative.LOGGER.info("TCP capture server listening on port {}", socket.getLocalPort());
    }
    for (String address : addresses) {
      ChatNative.LOGGER.info(
          "TCP capture server listening on {}:{}", address, socket.getLocalPort());
    }
  }

  private static List<String> classCAddresses() {
    List<String> found = new ArrayList<>();
    try {
      for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (!nic.isUp() || nic.isLoopback()) {
          continue;
        }
        for (InetAddress address : Collections.list(nic.getInetAddresses())) {
          byte[] raw = address.getAddress();
          if (raw.length == 4 && (raw[0] & 0xFF) == 192 && (raw[1] & 0xFF) == 168) {
            found.add(address.getHostAddress());
          }
        }
      }
    } catch (SocketException e) {
      ChatNative.LOGGER.warn("Cannot list network interfaces", e);
    }
    return found;
  }

  public int port() {
    ServerSocket socket = serverSocket;
    return socket == null ? -1 : socket.getLocalPort();
  }

  public void sendUpstream(CapturedPacket packet) {
    String line = packet.encode();
    for (Connection upstream : upstreams) {
      upstream.enqueue(line);
    }
  }

  @Override
  public void setListener(Consumer<CapturedPacket> listener) {
    this.handler = listener;
  }

  @Override
  public void send(CapturedPacket packet) {
    String line = packet.encode();
    for (Connection client : clients) {
      client.enqueue(line);
    }
  }

  public void receive(String line) {
    CapturedPacket.decode(line, registry)
        .ifPresentOrElse(
            handler::accept, () -> ChatNative.LOGGER.debug("Ignoring undecodable line"));
  }

  public synchronized void connect(String host, int port, Consumer<Boolean> onState) {
    disconnectUpstream();
    Thread thread =
        Threads.daemon("chatnative-tcp-upstream", () -> upstreamLoop(host, port, onState));
    upstreamThread = thread;
    thread.start();
  }

  public synchronized void disconnectUpstream() {
    Thread thread = upstreamThread;
    upstreamThread = null;
    if (thread != null) {
      thread.interrupt();
    }
    upstreams.forEach(Connection::close);
  }

  private void acceptLoop(ServerSocket socket) {
    while (!closed) {
      try {
        Socket accepted = socket.accept();
        Connection connection = new Connection(accepted, clients);
        clients.add(connection);
        connection.start();
        connection.enqueue(snapshot.get().encode());
      } catch (IOException e) {
        if (!closed) {
          ChatNative.LOGGER.warn("TCP accept failed", e);
        }
        return;
      }
    }
  }

  private void upstreamLoop(String host, int port, Consumer<Boolean> onState) {
    long backoff = 1_000;
    Thread self = Thread.currentThread();
    while (!closed && upstreamThread == self) {
      try {
        Socket socket = new Socket(host, port);
        Connection connection = new Connection(socket, upstreams);
        upstreams.add(connection);
        backoff = 1_000;
        connection.startWriter();
        onState.accept(true);
        connection.readLoop();
      } catch (IOException e) {
        ChatNative.LOGGER.debug("Upstream {}:{} unavailable: {}", host, port, e.toString());
      }
      if (upstreamThread != self) {
        break;
      }
      onState.accept(false);
      try {
        Thread.sleep(backoff);
      } catch (InterruptedException e) {
        break;
      }
      backoff = Math.min(backoff * 2, 30_000);
    }
  }

  @Override
  public void close() {
    closed = true;
    disconnectUpstream();
    try {
      ServerSocket socket = serverSocket;
      if (socket != null) {
        socket.close();
      }
    } catch (IOException ignored) {
    }
    clients.forEach(Connection::close);
  }

  private final class Connection {
    private final Socket socket;
    private final Set<Connection> owner;
    private final BlockingQueue<String> outbox = new LinkedBlockingQueue<>(512);

    Connection(Socket socket, Set<Connection> owner) {
      this.socket = socket;
      this.owner = owner;
    }

    void start() {
      startWriter();
      Threads.start("chatnative-tcp-read", this::readLoop);
    }

    void startWriter() {
      Threads.start("chatnative-tcp-write", this::writeLoop);
    }

    void enqueue(String line) {
      if (!outbox.offer(line)) {
        ChatNative.LOGGER.warn("Dropping slow TCP connection {}", socket.getRemoteSocketAddress());
        close();
      }
    }

    void readLoop() {
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          receive(line);
        }
      } catch (IOException ignored) {
      } finally {
        close();
      }
    }

    private void writeLoop() {
      try {
        Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        while (!socket.isClosed()) {
          writer.write(outbox.take());
          writer.write('\n');
          writer.flush();
        }
      } catch (IOException | InterruptedException ignored) {
      } finally {
        close();
      }
    }

    void close() {
      owner.remove(this);
      try {
        socket.close();
      } catch (IOException ignored) {
      }
    }
  }
}
