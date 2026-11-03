/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.client;

import dev.streamsreflowing.client.StreamTint;
import net.minecraft.client.Minecraft;

public final class ClientTintDebug {
    private ClientTintDebug() {
    }

    public static void run() {
        Minecraft.getInstance().execute(StreamTint::debugMap);
    }
}

