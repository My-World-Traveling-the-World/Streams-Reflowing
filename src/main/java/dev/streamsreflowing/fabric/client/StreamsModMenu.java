/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.streamsreflowing.fabric.client.StreamsConfigScreen;

public final class StreamsModMenu
implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return StreamsConfigScreen::new;
    }
}

