package com.karasu256;

public final class Threads {
  private Threads() {}

  public static Thread daemon(String name, Runnable task) {
    Thread thread = new Thread(task, name);
    thread.setDaemon(true);
    return thread;
  }

  public static Thread start(String name, Runnable task) {
    Thread thread = daemon(name, task);
    thread.start();
    return thread;
  }
}
