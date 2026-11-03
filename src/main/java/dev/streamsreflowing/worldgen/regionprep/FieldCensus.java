/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.regionprep;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.slf4j.Logger;

public final class FieldCensus {
    private static final Set<String> a = Set.of("Constant", "YClampedGradient", "Ap2", "MulOrAdd", "Mapped", "Noise", "ShiftedNoise", "Shift", "ShiftA", "ShiftB", "RangeChoice", "Clamp", "Spline", "BlendAlpha", "BlendOffset", "BlendDensity", "Marker", "MarkerOrMarked", "HolderHolder", "WeirdScaledSampler");

    private FieldCensus() {
    }

    public static Result scan(DensityFunction densityFunction2) {
        TreeMap<String, Integer> treeMap = new TreeMap<String, Integer>();
        ArrayList<String> arrayList = new ArrayList<String>();
        int[] nArray = new int[]{0};
        try {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            densityFunction2.mapAll(densityFunction -> {
                nArray[0] = nArray[0] + 1;
                String string = FieldCensus.a(densityFunction);
                treeMap.merge(string, 1, Integer::sum);
                if (!a.contains(string) && linkedHashMap.putIfAbsent(string, Boolean.TRUE) == null) {
                    arrayList.add(string + " (" + densityFunction.getClass().getName() + ")");
                }
                return densityFunction;
            });
        }
        catch (RuntimeException runtimeException) {
            arrayList.add("tree rejected visitor walk: " + String.valueOf(runtimeException));
            return new Result(false, nArray[0], treeMap, arrayList);
        }
        return new Result(arrayList.isEmpty(), nArray[0], treeMap, arrayList);
    }

    private static String a(DensityFunction densityFunction) {
        int n;
        String string = densityFunction.getClass().getSimpleName();
        if (string.isEmpty() && (n = (string = densityFunction.getClass().getName()).lastIndexOf(46)) >= 0) {
            string = string.substring(n + 1);
        }
        return string;
    }

    public static Result scanWithDiag(DensityFunction densityFunction, Logger logger) {
        Result result = FieldCensus.scan(densityFunction);
        if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
            logger.info("[SR-PREP] density tree: {} nodes, supported={}", (Object)result.totalNodes(), (Object)result.supported());
            for (Map.Entry<String, Integer> object : result.counts().entrySet()) {
                logger.info("[SR-PREP]   {} x{}{}", object.getKey(), object.getValue(), a.contains(object.getKey()) ? "" : "  <-- UNRECOGNIZED");
            }
            for (String string : result.unrecognized()) {
                logger.info("[SR-PREP]   unrecognized: {}", (Object)string);
            }
        }
        return result;
    }

    public record Result(boolean supported, int totalNodes, Map<String, Integer> counts, List<String> unrecognized) {
    }
}

