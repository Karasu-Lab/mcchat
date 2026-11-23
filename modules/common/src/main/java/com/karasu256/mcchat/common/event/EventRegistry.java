package com.karasu256.mcchat.common.event;

import com.google.gson.Gson;
import com.karasu256.mcchat.common.Json;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EventRegistry {
  private static final EventRegistry COMMON = new EventRegistry();

  private final Map<String, Class<? extends CapturedEvent>> types = new ConcurrentHashMap<>();
  private final Gson gson = Json.create(this);

  public EventRegistry() {
    register(ChatReceivedEvent.class);
    register(ChatSentEvent.class);
    register(CommandSentEvent.class);
    register(CommandExecutedEvent.class);
    register(PlayerListEvent.class);
    register(PlayerJoinedEvent.class);
    register(PlayerLeftEvent.class);
    register(OnlineEvent.class);
    register(OfflineEvent.class);
    register(TypingStartedEvent.class);
    register(TypingStoppedEvent.class);
    register(WorldChangedEvent.class);
    register(WorldIconEvent.class);
  }

  public static EventRegistry common() {
    return COMMON;
  }

  public EventRegistry register(Class<? extends CapturedEvent> type) {
    types.put(type.getAnnotation(EventType.class).value(), type);
    return this;
  }

  public Class<? extends CapturedEvent> classOf(String name) {
    return types.get(name);
  }

  public Gson gson() {
    return gson;
  }
}
