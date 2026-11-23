package com.karasu256.client;

import com.google.common.net.HostAndPort;
import com.karasu256.ChatNative;
import com.karasu256.capture.CapturePipeline;
import com.karasu256.capture.ServerCapture;
import com.karasu256.capture.ServerWorldSource;
import com.karasu256.client.capture.ClientCapture;
import com.karasu256.client.capture.RemoteWorldSource;
import com.karasu256.client.chat.ClientChatTextProcessor;
import com.karasu256.client.ui.ChatLog;
import com.karasu256.client.ui.ChatTheme;
import com.karasu256.client.ui.ChatUi;
import com.karasu256.client.ui.Shapes;
import com.karasu256.mcchat.common.CapturedPacket;
import com.karasu256.mcchat.common.Origin;
import com.karasu256.mcchat.common.WorldData;
import com.karasu256.mcchat.common.WorldSource;
import com.karasu256.mcchat.common.event.CapturedEvent;
import com.karasu256.mcchat.common.event.CommandExecutedEvent;
import com.karasu256.mcchat.common.event.PlayerListEvent;
import com.karasu256.net.CaptureEndpointPayload;
import com.karasu256.net.ReceivedChatTextPayload;
import com.karasu256.net.TCPServer;
import com.karasu256.net.TypingBroadcastPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

public class ChatNativeClient implements ClientModInitializer {
  private static ChatUi chatUi;

  private ClientTcpController tcp;

  public static ChatUi chatUi() {
    return chatUi;
  }

  @Override
  public void onInitializeClient() {
    CapturePipeline pipeline = ChatNative.pipeline();
    ChatTheme theme = ChatTheme.dark();
    Shapes.register();
    chatUi = new ChatUi(theme);
    registerChatUi();
    ClientChatTextProcessor.register();
    ClientCapture.register();
    pipeline.setOnline(false, ClientCapture.sessionPlayer());

    tcp = new ClientTcpController(ChatNative.clientConfig(), pipeline, this::receive);
    ChatNative.serverTcp().runWithoutServer();

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
      TCPServer server = tcp.tcp();
      if (server != null) {
        server.disconnectUpstream();
      }
      resetWorld(pipeline, client);
      pipeline.setOnline(true, ClientCapture.sessionPlayer());
      ClientCapture.submitPlayerList(client);
      WorldData world = worldSource(client).world();
      ChatLog.INSTANCE.worldJoined(world.id(), world.worldName());
    });

    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
      ChatLog.INSTANCE.clearTyping();
      pipeline.clearIcon();
      pipeline.setOnline(false, ClientCapture.sessionPlayer());
    });

    ClientPlayNetworking.registerGlobalReceiver(
        CaptureEndpointPayload.TYPE,
        (payload, player, sender) -> connectUpstream(pipeline, payload, Minecraft.getInstance()));

    ClientPlayNetworking.registerGlobalReceiver(
        ReceivedChatTextPayload.TYPE,
        (payload, player, sender) -> ClientChatTextProcessor.structured(payload));

    ClientPlayNetworking.registerGlobalReceiver(
        TypingBroadcastPayload.TYPE,
        (payload, player, sender) -> ChatLog.INSTANCE.typing(payload.player(), payload.started()));

    ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
      tcp.stop();
      pipeline.close();
    });
  }

  private void registerChatUi() {
    ClientChatTextProcessor.listen(chat ->
        ChatLog.INSTANCE.received(
            chat, ClientCapture.sessionPlayer().uuid(), Minecraft.getInstance().gui.getGuiTicks()));

    ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
      if (screen instanceof ChatScreen) {
        ScreenEvents.beforeRender(screen)
            .register((current, graphics, mouseX, mouseY, delta) -> {
              if (chatUi.isActive()) {
                chatUi.render(current, graphics);
              }
            });
        ScreenEvents.afterRender(screen)
            .register((current, graphics, mouseX, mouseY, delta) ->
                chatUi.renderHover(current, graphics, mouseX, mouseY));
        ScreenMouseEvents.allowMouseClick(screen)
            .register((current, mouseX, mouseY, button) ->
                !chatUi.mouseClicked(current, mouseX, mouseY, button));
        ScreenMouseEvents.allowMouseScroll(screen)
            .register((current, mouseX, mouseY, horizontal, vertical) ->
                !chatUi.mouseScrolled(current, mouseX, mouseY, vertical));
      }
    });
  }

  private static WorldSource worldSource(Minecraft client) {
    IntegratedServer local = client.getSingleplayerServer();
    return local != null
        ? new ServerWorldSource(local)
        : new RemoteWorldSource(client.getCurrentServer());
  }

  private static void resetWorld(CapturePipeline pipeline, Minecraft client) {
    pipeline.reset(worldSource(client));
    IntegratedServer local = client.getSingleplayerServer();
    if (local != null) {
      local.execute(() ->
          new PlayerListEvent(pipeline, Origin.SERVER, ServerCapture.onlinePlayers(local))
              .handle());
    }
  }

  private void connectUpstream(
      CapturePipeline pipeline, CaptureEndpointPayload payload, Minecraft client) {
    TCPServer upstream = tcp.tcp();
    if (upstream == null) {
      return;
    }
    ServerData server = client.getCurrentServer();
    String host = payload.host().isEmpty() && server != null ? hostOf(server.ip) : payload.host();
    upstream.connect(
        host, payload.port(), up -> pipeline.setAuthority(up ? Origin.SERVER : Origin.CLIENT));
  }

  private void receive(CapturedPacket packet) {
    CapturePipeline pipeline = ChatNative.pipeline();
    if (packet.type() == CapturedPacket.Type.PING) {
      pipeline.requestStatus();
      return;
    }
    boolean fromServer = packet.authority() == Origin.SERVER;
    packet.bind(fromServer ? pipeline : event -> {});
    if (fromServer && packet.type() == CapturedPacket.Type.SNAPSHOT) {
      pipeline.adoptContext(packet.world().context().without(CommandExecutedEvent.class));
    } else if (packet.type() == CapturedPacket.Type.EVENT) {
      packet.event().ifPresent(CapturedEvent::handle);
    }
  }

  private static String hostOf(String address) {
    return HostAndPort.fromString(address).getHost();
  }
}
