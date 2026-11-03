package dev.streamsreflowing.fabric.compat;

import com.seibel.distanthorizons.api.DhApi.Delayed;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkProcessingEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkProcessingEvent.EventParam;
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
      DhApiEventRegister.on(DhApiChunkProcessingEvent.class, new DistantHorizonsCompat.StreamReplacer());
      LOGGER.debug("[StreamsReflowing] Distant Horizons LOD hook registered{}", PROBE ? " (probe on)" : "");
   }

   private static IDhApiBlockStateWrapper water(IDhApiLevelWrapper levelWrapper) {
      IDhApiBlockStateWrapper w = waterWrapper;
      if (w == null) {
         w = wrapperFor("minecraft:water", levelWrapper);
         waterWrapper = w;
      }

      return w;
   }

   private static IDhApiBlockStateWrapper wrapperFor(String state, IDhApiLevelWrapper levelWrapper) {
      try {
         return Delayed.wrapperFactory.getDefaultBlockStateWrapper(state, levelWrapper);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static final class StreamReplacer extends DhApiChunkProcessingEvent {
      public void blockOrBiomeChangedDuringChunkProcessing(DhApiEventParam<EventParam> event) {
         try {
            EventParam p = (EventParam)event.value;
            String serial = p.currentBlock.getSerialString();
            if (serial == null || !serial.startsWith("streamsreflowing:")) {
               return;
            }

            IDhApiBlockStateWrapper w = DistantHorizonsCompat.water(p.levelWrapper);
            if (w != null) {
               p.setBlockOverride(w);
               if (DistantHorizonsCompat.PROBE) {
                  DistantHorizonsCompat.LOGGER.info("[dhprobe] swap {} -> water", serial);
               }
            }
         } catch (Throwable var5) {
         }
      }
   }
}
