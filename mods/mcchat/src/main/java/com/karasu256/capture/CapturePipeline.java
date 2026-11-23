package com.karasu256.capture;

import com.karasu256.ChatNative;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.MinecraftScreen;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.PlayerRef;
import com.karasu256.mcchat.common.WorldContext;
import com.karasu256.mcchat.common.WorldData;
import com.karasu256.mcchat.common.WorldSource;
import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.EventContext;
import com.karasu256.mcchat.common.event.OfflineEvent;
import com.karasu256.mcchat.common.event.OnlineEvent;
import com.karasu256.mcchat.common.event.PlayerListEvent;
import com.karasu256.mcchat.common.event.WorldChangedEvent;
import com.karasu256.mcchat.common.event.WorldIconEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CapturePipeline implements AutoCloseable, EventContext {
  private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1024);
  private final List<CaptureSink> sinks = new CopyOnWriteArrayList<>();
  private final Thread worker;

  private volatile WorldData world = WorldData.empty("", "");
  private volatile Origin authority = Origin.CLIENT;
  private volatile boolean online;
  private volatile PlayerRef player;
  private volatile MinecraftScreen screen;
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
        new WorldContext(
            SkinResolver.attachAll(context.players()),
            context.history(),
            context.icon().orElse(null));
    return new CapturedPacket(
        CapturedPacket.Type.SNAPSHOT, authority, current.withContext(withSkins), Optional.empty());
  }

  public void republishPlayers() {
    enqueue(() ->
        emit(
            CapturedPacket.event(
                authority,
                world,
                new PlayerListEvent(
                    this, authority, SkinResolver.attachAll(world.context().players())))));
  }

  @Override
  public void accept(CapturedEvent event) {
    submit(event);
  }

  public void submit(CapturedEvent event) {
    enqueue(() -> process(event));
  }

  public void reset(String host, String worldName, Origin newAuthority) {
    reset(new WorldSource(host, worldName, newAuthority));
  }

  public void reset(WorldSource source) {
    WorldData next = source.world();
    enqueue(() -> {
      authority = source.origin();
      world = next;
      emit(
          new CapturedPacket(
              CapturedPacket.Type.RESET,
              authority,
              world,
              Optional.of(new WorldChangedEvent(this, authority, world.worldName()))));
    });
  }

  public void adoptContext(WorldContext context) {
    enqueue(() -> {
      WorldContext adopted =
          context.icon().isPresent()
              ? context
              : context.withIcon(world.context().icon().orElse(null));
      world = world.withContext(adopted);
      emit(new CapturedPacket(CapturedPacket.Type.SNAPSHOT, authority, world, Optional.empty()));
    });
  }

  public void clearIcon() {
    enqueue(() -> process(new WorldIconEvent(this, authority, null)));
  }

  public void publish(CapturedEvent event) {
    enqueue(() -> emit(CapturedPacket.event(authority, world, event)));
  }

  public void setOnline(boolean value, PlayerRef who) {
    enqueue(() -> {
      online = value;
      player = who;
      emit(status());
    });
  }

  public void setScreen(MinecraftScreen current) {
    enqueue(() -> {
      screen = current;
      emit(status());
    });
  }

  public void requestStatus() {
    enqueue(() -> emit(status()));
  }

  private CapturedPacket status() {
    CapturedEvent state =
        online
            ? new OnlineEvent(this, authority, player, screen)
            : new OfflineEvent(this, authority, player, screen);
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

  private void process(CapturedEvent captured) {
    if (captured.origin() != authority) {
      return;
    }
    CapturedEvent event = SkinResolver.attach(captured);
    WorldData current = world;
    world = current.withContext(current.context().apply(event, 100));
    emit(CapturedPacket.event(authority, world, event));
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
