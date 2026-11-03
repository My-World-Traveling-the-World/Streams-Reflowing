package dev.streamsreflowing.compat;

import dev.streamsreflowing.Constants;
import java.lang.reflect.Method;

public final class FlowingFluidsCompat {
   private static boolean warned;

   private FlowingFluidsCompat() {
   }

   public static boolean exemptFluids(Object server, Object... fluids) {
      Class<?> apiCls;
      try {
         apiCls = Class.forName("traben.flowing_fluids.api.FlowingFluidsAPI");
      } catch (ClassNotFoundException var14) {
         return true;
      }

      try {
         Object api = apiCls.getMethod("getInstance", String.class).invoke(null, "streamsreflowing");
         boolean allOk = true;

         for (Object fluid : fluids) {
            Method disable = null;

            for (Method m : apiCls.getMethods()) {
               if (m.getName().equals("disableThisFluid")
                  && m.getParameterCount() == 2
                  && m.getParameterTypes()[0].isInstance(fluid)
                  && (server == null || m.getParameterTypes()[1].isInstance(server))) {
                  disable = m;
                  break;
               }
            }

            if (disable == null) {
               allOk = false;
            } else {
               disable.invoke(api, fluid, server);
            }
         }

         if (allOk) {
            Constants.LOG.info("Streams Reflowing: stream water protected from Flowing Fluids draining.");
         } else if (!warned) {
            warned = true;
            Constants.LOG
               .warn(
                  "Streams Reflowing: could not fully protect stream water from Flowing Fluids (API mismatch) -- streams may drain. Consider updating Flowing Fluids."
               );
         }

         return allOk;
      } catch (RuntimeException | ReflectiveOperationException var15) {
         if (!warned) {
            warned = true;
            Constants.LOG.warn("Streams Reflowing: could not protect stream water from Flowing Fluids -- streams may drain. ({})", var15.toString());
         }

         return false;
      }
   }
}
