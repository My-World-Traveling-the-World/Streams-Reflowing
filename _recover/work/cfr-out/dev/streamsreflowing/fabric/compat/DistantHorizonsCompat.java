/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.compat;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkProcessingEvent;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DistantHorizonsCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("streamsreflowing/dh");
    private static final boolean PROBE = Boolean.getBoolean("streamsreflowing.dhprobe");
    private static volatile IDhApiBlockStateWrapper waterWrapper;

    private DistantHorizonsCompat() {
    }

    public static void init() {
        DhApiEventRegister.on(DhApiChunkProcessingEvent.class, (IDhApiEvent)new StreamReplacer());
        LOGGER.debug("[StreamsReflowing] Distant Horizons LOD hook registered{}", (Object)(PROBE ? " (probe on)" : ""));
    }

    private static IDhApiBlockStateWrapper water(IDhApiLevelWrapper levelWrapper) {
        IDhApiBlockStateWrapper w = waterWrapper;
        if (w == null) {
            waterWrapper = w = DistantHorizonsCompat.wrapperFor("minecraft:water", levelWrapper);
        }
        return w;
    }

    private static IDhApiBlockStateWrapper wrapperFor(String state, IDhApiLevelWrapper levelWrapper) {
        try {
            return DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(state, levelWrapper);
        }
        catch (Throwable t) {
            return null;
        }
    }

    private static final class StreamReplacer
    extends DhApiChunkProcessingEvent {
        private StreamReplacer() {
        }

        public void blockOrBiomeChangedDuringChunkProcessing(DhApiEventParam<DhApiChunkProcessingEvent.EventParam> event) {
            try {
                DhApiChunkProcessingEvent.EventParam p = (DhApiChunkProcessingEvent.EventParam)event.value;
                String serial = p.currentBlock.getSerialString();
                if (serial == null || !serial.startsWith("streamsreflowing:")) {
                    return;
                }
                IDhApiBlockStateWrapper w = DistantHorizonsCompat.water(p.levelWrapper);
                if (w != null) {
                    p.setBlockOverride(w);
                    if (PROBE) {
                        LOGGER.info("[dhprobe] swap {} -> water", (Object)serial);
                    }
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }
}

