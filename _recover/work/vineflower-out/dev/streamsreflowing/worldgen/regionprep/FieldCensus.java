package dev.streamsreflowing.worldgen.regionprep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.slf4j.Logger;

public final class FieldCensus {
   private static final Set<String> a = Set.of(
      "Constant",
      "YClampedGradient",
      "Ap2",
      "MulOrAdd",
      "Mapped",
      "Noise",
      "ShiftedNoise",
      "Shift",
      "ShiftA",
      "ShiftB",
      "RangeChoice",
      "Clamp",
      "Spline",
      "BlendAlpha",
      "BlendOffset",
      "BlendDensity",
      "Marker",
      "MarkerOrMarked",
      "HolderHolder",
      "WeirdScaledSampler"
   );

   private FieldCensus() {
   }

   public static FieldCensus.Result scan(DensityFunction var0) {
      TreeMap var1 = new TreeMap();
      ArrayList var2 = new ArrayList();
      int[] var3 = new int[]{0};

      try {
         LinkedHashMap var4 = new LinkedHashMap();
         var0.mapAll(var4x -> {
            var3[0]++;
            String var5x = a(var4x);
            var1.merge(var5x, 1, Integer::sum);
            if (!a.contains(var5x) && var4.putIfAbsent(var5x, Boolean.TRUE) == null) {
               var2.add(var5x + " (" + var4x.getClass().getName() + ")");
            }

            return var4x;
         });
      } catch (RuntimeException var5) {
         var2.add("tree rejected visitor walk: " + var5);
         return new FieldCensus.Result(false, var3[0], var1, var2);
      }

      return new FieldCensus.Result(var2.isEmpty(), var3[0], var1, var2);
   }

   private static String a(DensityFunction var0) {
      String var1 = var0.getClass().getSimpleName();
      if (var1.isEmpty()) {
         var1 = var0.getClass().getName();
         int var2 = var1.lastIndexOf(46);
         if (var2 >= 0) {
            var1 = var1.substring(var2 + 1);
         }
      }

      return var1;
   }

   public static FieldCensus.Result scanWithDiag(DensityFunction var0, Logger var1) {
      FieldCensus.Result var2 = scan(var0);
      if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
         var1.info("[SR-PREP] density tree: {} nodes, supported={}", var2.totalNodes(), var2.supported());

         for (Entry var4 : var2.counts().entrySet()) {
            var1.info("[SR-PREP]   {} x{}{}", var4.getKey(), var4.getValue(), a.contains(var4.getKey()) ? "" : "  <-- UNRECOGNIZED");
         }

         for (String var6 : var2.unrecognized()) {
            var1.info("[SR-PREP]   unrecognized: {}", var6);
         }
      }

      return var2;
   }

   public static record Result() {
      private final boolean supported;
      private final int totalNodes;
      private final Map<String, Integer> counts;
      private final List<String> unrecognized;

      public Result(boolean var1, int var2, Map<String, Integer> var3, List<String> var4) {
         this.supported = var1;
         this.totalNodes = var2;
         this.counts = var3;
         this.unrecognized = var4;
      }
   }
}
