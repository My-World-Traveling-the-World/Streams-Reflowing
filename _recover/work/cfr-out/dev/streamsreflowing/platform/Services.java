/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.platform;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.platform.services.IPlatformHelper;
import java.util.ServiceLoader;

public final class Services {
    public static final IPlatformHelper PLATFORM = Services.load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        T loadedService = ServiceLoader.load(clazz).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", (Object)loadedService, (Object)clazz);
        return loadedService;
    }

    private Services() {
    }
}

