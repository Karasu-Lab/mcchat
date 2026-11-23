package com.karasu256.mcchat.common.event;

@FunctionalInterface
public interface EventContext {
  void accept(CapturedEvent event);
}
