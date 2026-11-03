/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.bank.BankFeatureLoader;
import dev.streamsreflowing.worldgen.bank.BankStyleLoader;

public final class StreamsReflowing {
    public static void init() {
        Constants.LOG.info("StreamsReflowing initializing on {} ({} environment)", (Object)Services.PLATFORM.getPlatformName(), (Object)Services.PLATFORM.getEnvironmentName());
        BankStyleLoader.seed();
        BankFeatureLoader.seed();
    }

    private StreamsReflowing() {
    }
}

