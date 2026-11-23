package com.karasu256.client.config;

import com.karasu256.ChatNative;
import com.karasu256.config.TcpConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ChatNativeConfigScreen {
  private ChatNativeConfigScreen() {}

  static Screen create(Screen parent) {
    TcpConfig client = ChatNative.clientConfig();
    TcpConfig server = ChatNative.serverConfig();

    ConfigBuilder builder =
        ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("config.chatnative.title"))
            .setSavingRunnable(() -> {
              client.save();
              server.save();
            });
    ConfigEntryBuilder entries = builder.entryBuilder();

    ConfigCategory clientCategory =
        builder.getOrCreateCategory(Component.translatable("config.chatnative.client"));
    addToggle(clientCategory, entries, client);
    clientCategory.addEntry(
        entries
            .startBooleanToggle(
                Component.translatable("config.chatnative.use_vanilla_chat"), client.useVanillaChat)
            .setDefaultValue(false)
            .setSaveConsumer(value -> client.useVanillaChat = value)
            .build());
    clientCategory.addEntry(
        entries
            .startBooleanToggle(
                Component.translatable("config.chatnative.show_own_typing"), client.showOwnTyping)
            .setDefaultValue(false)
            .setSaveConsumer(value -> client.showOwnTyping = value)
            .build());
    clientCategory.addEntry(
        entries
            .startBooleanToggle(
                Component.translatable("config.chatnative.show_own_name"), client.showOwnName)
            .setDefaultValue(false)
            .setSaveConsumer(value -> client.showOwnName = value)
            .build());
    addToggle(
        builder.getOrCreateCategory(Component.translatable("config.chatnative.server")),
        entries,
        server);

    return builder.build();
  }

  private static void addToggle(
      ConfigCategory category, ConfigEntryBuilder entries, TcpConfig config) {
    category.addEntry(
        entries
            .startBooleanToggle(
                Component.translatable("config.chatnative.tcp_enabled"), config.enabled)
            .setDefaultValue(false)
            .setSaveConsumer(value -> config.enabled = value)
            .build());
  }
}
