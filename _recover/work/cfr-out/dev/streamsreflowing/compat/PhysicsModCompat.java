/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.compat;

import dev.streamsreflowing.Constants;
import java.lang.reflect.Field;

public final class PhysicsModCompat {
    private static volatile boolean silenced;

    private PhysicsModCompat() {
    }

    public static void silenceWaterEffect() {
        if (silenced) {
            return;
        }
        silenced = true;
        try {
            Field f = Class.forName("net.diebuddies.config.ConfigClient").getField("liquidPhysics");
            f.setAccessible(true);
            if (f.getBoolean(null)) {
                f.setBoolean(null, false);
                Constants.LOG.info("Streams Reflowing: turned off the Physics Mod's flowing-water effect so stream currents animate cleanly (setting 'physicsModWater').");
            }
        }
        catch (LinkageError | ReflectiveOperationException | SecurityException e) {
            Constants.LOG.debug("Streams Reflowing: no Physics Mod water compat ({}).", (Object)e.toString());
        }
    }
}

