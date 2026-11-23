package com.karasu256.capture;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.CapturedData;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;
import com.karasu256.mcchat.common.WorldData;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CapturePipeline implements AutoCloseable {
  private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1024);
  private final List<CaptureSink> sinks = new CopyOnWriteArrayList<>();
  private final Thread worker;

  private volatile WorldData world = WorldData.empty("", "");
  private volatile Origin authority = Origin.CLIENT;
  private volatile boolean online;
  private volatile PlayerRef player;
  private volatile boolean running = true;

  public CapturePipeline() {
    this.worker = Thread.ofVirtual().name("chatnative-pipeline").unstarted(this::run);
    this.worker.start();
  }

  public void addSink(CaptureSink sink) {
    sinks.add(sink);
  }

  public Origin authority() {
    return authority;
  }

  public CapturedPacket snapshot() {
    WorldData current = world;
    WorldContext context = current.context();
    WorldContext withSkins =
        new WorldContext(SkinResolver.attachAll(context.players()), context.history());
    return new CapturedPacket(
        CapturedPacket.Type.SNAPSHOT, authority, current.withContext(withSkins), Optional.empty());
  }

  public void republishPlayers() {
    enqueue(() ->
        emit(
            CapturedPacket.event(
                authority,
                world,
                CapturedData.playerList(
                    authority, SkinResolver.attachAll(world.context().players())))));
  }

  public void submit(CapturedData data) {
    enqueue(() -> process(data));
  }

  public void reset(String host, String worldName, Origin newAuthority) {
    enqueue(() -> {
      authority = newAuthority;
      world = WorldData.empty(host, worldName);
      emit(new CapturedPacket(CapturedPacket.Type.RESET, authority, world, Optional.empty()));
    });
  }

  public void adoptContext(WorldContext context) {
    enqueue(() -> {
      world = world.withContext(context);
      emit(new CapturedPacket(CapturedPacket.Type.SNAPSHOT, authority, world, Optional.empty()));
    });
  }

  public void publish(CapturedData data) {
    enqueue(() -> emit(CapturedPacket.event(authority, world, data)));
  }

  public void setOnline(boolean value, PlayerRef who) {
    enqueue(() -> {
      online = value;
      player = who;
      emit(status());
    });
  }

  public void requestStatus() {
    enqueue(() -> emit(status()));
  }

  private CapturedPacket status() {
    CapturedData state =
        CapturedData.of(
            authority, online ? CapturedData.Kind.ONLINE : CapturedData.Kind.OFFLINE, player, null);
    return new CapturedPacket(
        CapturedPacket.Type.STATUS, authority, world.withoutContext(), Optional.of(state));
  }

  public void setAuthority(Origin newAuthority) {
    enqueue(() -> authority = newAuthority);
  }

  private void enqueue(Runnable task) {
    while (!queue.offer(task)) {
      queue.poll();
      ChatNative.LOGGER.warn("Capture pipeline queue full, dropping the oldest item");
    }
  }

  private void run() {
    while (running) {
      try {
        queue.take().run();
      } catch (InterruptedException e) {
        return;
      } catch (RuntimeException e) {
        ChatNative.LOGGER.error("Capture pipeline stage failed", e);
      }
    }
  }

  private void process(CapturedData captured) {
    if (captured.origin() != authority) {
      return;
    }
    CapturedData data = SkinResolver.attach(captured);
    WorldData current = world;
    world = current.withContext(current.context().apply(data, 100));
    emit(CapturedPacket.event(authority, world, data));
  }

  private void emit(CapturedPacket packet) {
    for (CaptureSink sink : sinks) {
      try {
        sink.accept(packet);
      } catch (RuntimeException e) {
        ChatNative.LOGGER.error("Capture sink failed", e);
      }
    }
  }

  @Override
  public void close() {
    running = false;
    worker.interrupt();
  }
}
