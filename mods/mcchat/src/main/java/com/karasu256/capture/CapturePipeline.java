package com.karasu256.capture;

import com.karasu256.ChatNative;
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
    return new CapturedPacket(CapturedPacket.Type.SNAPSHOT, authority, world, Optional.empty());
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

  private void process(CapturedData data) {
    if (data.origin() != authority) {
      return;
    }
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
