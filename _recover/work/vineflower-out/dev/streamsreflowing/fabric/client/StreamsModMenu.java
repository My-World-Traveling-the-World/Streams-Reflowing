package dev.streamsreflowing.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class StreamsModMenu implements ModMenuApi {
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return StreamsConfigScreen::new;
   }
}
