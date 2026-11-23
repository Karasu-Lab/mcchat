package com.karasu256.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class ChatNativeModMenu implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
      return parent -> parent;
    }
    return ChatNativeConfigScreen::create;
  }
}
