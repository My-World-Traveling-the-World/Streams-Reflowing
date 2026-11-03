/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.regionprep;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.worldgen.regionprep.FieldForm;
import dev.streamsreflowing.worldgen.regionprep.FormEval;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class FieldFormBuilder {
    public static volatile String lastDebugJson;
    public static volatile String lastTypeCensus;
    private final RegistryAccess b;
    private final PositionalRandomFactory c;
    private final JsonObject d = new JsonObject();
    private final Map<String, FieldForm.Node> e = new HashMap<String, FieldForm.Node>();
    private final Map<String, FieldForm.Node> f = new HashMap<String, FieldForm.Node>();
    private final Map<String, Integer> g = new HashMap<String, Integer>();
    final Map<String, Integer> a = new TreeMap<String, Integer>();
    private final FieldForm.NoiseRegistry h = new FieldForm.NoiseRegistry();
    private final RegistryOps<JsonElement> i;
    private DensityFunction j;
    private IdentityHashMap<Object, String> k;
    private final Map<String, FieldForm.Spline> l = new HashMap<String, FieldForm.Spline>();
    private final Map<String, DensityFunction.NoiseHolder> m = new HashMap<String, DensityFunction.NoiseHolder>();
    private static final double[][] n;

    private FieldFormBuilder(RegistryAccess registryAccess, long l) {
        this.b = registryAccess;
        this.i = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        this.c = new XoroshiroRandomSource(l).forkPositional();
    }

    public static FieldForm build(RegistryAccess registryAccess, long l, boolean bl, DensityFunction densityFunction, int n, int n2, int n3) {
        if (bl) {
            throw new Unsupported("legacy random source world");
        }
        FieldFormBuilder fieldFormBuilder = new FieldFormBuilder(registryAccess, l);
        fieldFormBuilder.j = densityFunction;
        fieldFormBuilder.c(densityFunction);
        JsonElement jsonElement = fieldFormBuilder.a(densityFunction, "root");
        FieldForm.Node node = fieldFormBuilder.a(jsonElement);
        if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("root", jsonElement);
            jsonObject.add("refs", fieldFormBuilder.d);
            lastDebugJson = jsonObject.toString();
        }
        lastTypeCensus = fieldFormBuilder.a.toString();
        return new FieldForm(node, fieldFormBuilder.h.b, n, n2, n3);
    }

    static DensityFunction a(DensityFunction densityFunction) {
        for (int i = 0; i < 8 && densityFunction != null; ++i) {
            Class<?> clazz = densityFunction.getClass();
            if (!clazz.getName().startsWith("com.ishland.c2me.opts.dfc.")) {
                return densityFunction;
            }
            DensityFunction densityFunction2 = null;
            try {
                for (Class<?> clazz2 = clazz; clazz2 != null && densityFunction2 == null; clazz2 = clazz2.getSuperclass()) {
                    try {
                        Method method = clazz2.getDeclaredMethod("getFallback", new Class[0]);
                        method.setAccessible(true);
                        densityFunction2 = (DensityFunction)method.invoke((Object)densityFunction, new Object[0]);
                        continue;
                    }
                    catch (NoSuchMethodException noSuchMethodException) {
                        // empty catch block
                    }
                }
            }
            catch (Throwable throwable) {
                throw new Unsupported("compiled density function " + clazz.getName() + " not readable: " + String.valueOf(throwable));
            }
            if (densityFunction2 == null || densityFunction2 == densityFunction) {
                throw new Unsupported("compiled density function " + clazz.getName() + " kept no original tree");
            }
            densityFunction = densityFunction2;
        }
        return densityFunction;
    }

    private static DensityFunction b(DensityFunction densityFunction) {
        for (int i = 0; i < 16 && densityFunction != null; ++i) {
            if (densityFunction instanceof DensityFunctions.HolderHolder) {
                DensityFunctions.HolderHolder holderHolder = (DensityFunctions.HolderHolder)densityFunction;
                Holder<DensityFunction> holder = holderHolder.function();
                if (holder.unwrapKey().isPresent()) {
                    return densityFunction;
                }
                DensityFunction densityFunction2 = holder.value();
                if (densityFunction2 == densityFunction) {
                    return densityFunction;
                }
                densityFunction = densityFunction2;
                continue;
            }
            if (densityFunction.getClass().getName().startsWith("com.ishland.c2me.opts.dfc.")) {
                densityFunction = FieldFormBuilder.a(densityFunction);
                continue;
            }
            return densityFunction;
        }
        return densityFunction;
    }

    private JsonElement a(DensityFunction densityFunction, String string) {
        densityFunction = FieldFormBuilder.a(densityFunction);
        densityFunction = densityFunction.mapAll(FieldFormBuilder::b);
        DataResult<JsonElement> dataResult = DensityFunction.HOLDER_HELPER_CODEC.encodeStart(this.i, densityFunction);
        return dataResult.result().orElseThrow(() -> new Unsupported(string + " did not encode: " + dataResult.error().map(Object::toString).orElse("?")));
    }

    private FieldForm.Node a(JsonElement jsonElement) {
        String string;
        if (jsonElement.isJsonPrimitive()) {
            DensityFunction densityFunction;
            if (jsonElement.getAsJsonPrimitive().isNumber()) {
                return new FieldForm.Const(jsonElement.getAsDouble());
            }
            String string2 = jsonElement.getAsString();
            FieldForm.Node node = this.e.get(string2);
            if (node != null) {
                return node;
            }
            Registry<DensityFunction> registry = this.b.registryOrThrow(Registries.DENSITY_FUNCTION);
            ResourceLocation resourceLocation = ResourceLocation.tryParse(string2);
            DensityFunction densityFunction2 = densityFunction = resourceLocation == null ? null : registry.get(resourceLocation);
            if (densityFunction == null) {
                throw new Unsupported("unresolvable density function reference: " + string2);
            }
            JsonElement jsonElement2 = this.a(densityFunction, "reference " + string2);
            this.d.add(string2, jsonElement2);
            FieldForm.Node node2 = this.a(jsonElement2);
            this.e.put(string2, node2);
            return node2;
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String string3 = jsonObject.toString();
        FieldForm.Node node = this.f.get(string3);
        if (node != null) {
            return node;
        }
        String string4 = jsonObject.get("type").getAsString();
        this.a.merge(string4, 1, Integer::sum);
        FieldForm.Node node3 = switch (string = string4.startsWith("minecraft:") ? string4.substring("minecraft:".length()) : string4) {
            case "constant" -> new FieldForm.Const(jsonObject.get("argument").getAsDouble());
            case "blend_alpha" -> new FieldForm.Const(1.0);
            case "blend_offset" -> new FieldForm.Const(0.0);
            case "beardifier" -> new FieldForm.Const(0.0);
            case "blend_density", "cache_once", "cache_all_in_cell" -> this.a(jsonObject.get("argument"));
            case "cache_2d" -> new FieldForm.Cache2D(this.a(jsonObject.get("argument")));
            case "flat_cache" -> new FieldForm.FlatCache(this.a(jsonObject.get("argument")));
            case "interval_select" -> this.b(jsonObject);
            case "interpolated" -> new FieldForm.Interp(this.a(jsonObject.get("argument")));
            case "add", "mul", "min", "max" -> {
                FieldForm.BinOp v2 = switch (string) {
                    case "add" -> FieldForm.BinOp.ADD;
                    case "mul" -> FieldForm.BinOp.MUL;
                    case "min" -> FieldForm.BinOp.MIN;
                    default -> FieldForm.BinOp.MAX;
                };
                yield new FieldForm.Bin(v2, this.a(jsonObject.get("argument1")), this.a(jsonObject.get("argument2")));
            }
            case "abs", "square", "cube", "half_negative", "quarter_negative", "squeeze" -> {
                FieldForm.MapOp v3 = switch (string) {
                    case "abs" -> FieldForm.MapOp.ABS;
                    case "square" -> FieldForm.MapOp.SQUARE;
                    case "cube" -> FieldForm.MapOp.CUBE;
                    case "half_negative" -> FieldForm.MapOp.HALF_NEGATIVE;
                    case "quarter_negative" -> FieldForm.MapOp.QUARTER_NEGATIVE;
                    default -> FieldForm.MapOp.SQUEEZE;
                };
                yield new FieldForm.Map1(v3, this.a(jsonObject.get("argument")));
            }
            case "tectonic:invert" -> new FieldForm.Map1(FieldForm.MapOp.RECIPROCAL, this.a(jsonObject.get("argument")));
            case "larionforgeport:div" -> new FieldForm.Bin(FieldForm.BinOp.DIV, this.a(jsonObject.get("argument1")), this.a(jsonObject.get("argument2")));
            case "larionforgeport:signum" -> new FieldForm.Map1(FieldForm.MapOp.SIGNUM, this.a(jsonObject.get("argument")));
            case "larionforgeport:sine" -> new FieldForm.Map1(FieldForm.MapOp.SIN, this.a(jsonObject.get("argument")));
            case "larionforgeport:z" -> new FieldForm.ZCoord();
            case "clamp" -> new FieldForm.Clamp(this.a(jsonObject.get("input")), jsonObject.get("min").getAsDouble(), jsonObject.get("max").getAsDouble());
            case "range_choice" -> new FieldForm.RangeChoice(this.a(jsonObject.get("input")), jsonObject.get("min_inclusive").getAsDouble(), jsonObject.get("max_exclusive").getAsDouble(), this.a(jsonObject.get("when_in_range")), this.a(jsonObject.get("when_out_of_range")));
            case "y_clamped_gradient" -> new FieldForm.YGradient(jsonObject.get("from_y").getAsInt(), jsonObject.get("to_y").getAsInt(), jsonObject.get("from_value").getAsDouble(), jsonObject.get("to_value").getAsDouble());
            case "noise" -> new FieldForm.NoiseNode(this.c(jsonObject.get("noise").getAsString()), jsonObject.get("xz_scale").getAsDouble(), jsonObject.get("y_scale").getAsDouble());
            case "shift_a" -> new FieldForm.ShiftNoise(FieldForm.ShiftKind.SHIFT_A, this.c(jsonObject.get("argument").getAsString()));
            case "shift_b" -> new FieldForm.ShiftNoise(FieldForm.ShiftKind.SHIFT_B, this.c(jsonObject.get("argument").getAsString()));
            case "shift" -> new FieldForm.ShiftNoise(FieldForm.ShiftKind.SHIFT, this.c(jsonObject.get("argument").getAsString()));
            case "shifted_noise" -> new FieldForm.ShiftedNoise(this.a(jsonObject.get("shift_x")), this.a(jsonObject.get("shift_y")), this.a(jsonObject.get("shift_z")), jsonObject.get("xz_scale").getAsDouble(), jsonObject.get("y_scale").getAsDouble(), this.c(jsonObject.get("noise").getAsString()));
            case "weird_scaled_sampler" -> new FieldForm.WeirdScaled(this.a(jsonObject.get("input")), this.c(jsonObject.get("noise").getAsString()), "type_1".equals(jsonObject.get("rarity_value_mapper").getAsString()) ? FieldForm.RarityKind.TYPE1 : FieldForm.RarityKind.TYPE2);
            case "spline" -> new FieldForm.SplineNode(this.b(jsonObject.get("spline")));
            case "old_blended_noise" -> this.a(jsonObject);
            default -> this.a(jsonElement, string4);
        };
        this.f.put(string3, node3);
        return node3;
    }

    private FieldForm.Node a(JsonElement jsonElement, String string) {
        List<DensityFunction> list = this.a(string);
        DensityFunction densityFunction = null;
        for (DensityFunction object2 : list) {
            JsonElement jsonElement2;
            try {
                jsonElement2 = this.a(object2, "live node");
            }
            catch (RuntimeException runtimeException) {
                continue;
            }
            if (!jsonElement.equals(jsonElement2)) continue;
            densityFunction = object2;
            break;
        }
        if (densityFunction == null && list.size() == 1) {
            densityFunction = list.get(0);
        }
        if (densityFunction == null) {
            throw new Unsupported("density function type: " + string + " (" + list.size() + " live candidates, none matched)");
        }
        this.a.merge(string + " (external)", 1, Integer::sum);
        DensityFunction densityFunction2 = densityFunction;
        ThreadLocal<Ctx> threadLocal = ThreadLocal.withInitial(Ctx::new);
        return new FieldForm.Extern((n, n2, n3) -> {
            Ctx ctx = (Ctx)threadLocal.get();
            ctx.a = n;
            ctx.b = n2;
            ctx.c = n3;
            return densityFunction2.compute(ctx);
        }, string);
    }

    private List<DensityFunction> a(final String string) {
        if (this.k == null) {
            this.k = new IdentityHashMap();
            for (Map.Entry object2 : BuiltInRegistries.DENSITY_FUNCTION_TYPE.entrySet()) {
                this.k.put(object2.getValue(), ((ResourceKey)object2.getKey()).location().toString());
            }
        }
        final ArrayList arrayList = new ArrayList(2);
        final TreeMap treeMap = new TreeMap();
        final int[] nArray = new int[]{0};
        try {
            FieldFormBuilder.a(this.j).mapAll(new DensityFunction.Visitor(){

                @Override
                public DensityFunction apply(DensityFunction densityFunction) {
                    String string2;
                    nArray[0] = nArray[0] + 1;
                    DensityFunction densityFunction2 = FieldFormBuilder.b(densityFunction);
                    try {
                        string2 = FieldFormBuilder.this.k.get(densityFunction2.codec().codec());
                    }
                    catch (RuntimeException runtimeException) {
                        treeMap.merge("<codec() threw: " + densityFunction2.getClass().getSimpleName() + ">", 1, Integer::sum);
                        return densityFunction;
                    }
                    treeMap.merge(string2 == null ? "<unregistered: " + densityFunction2.getClass().getName() + ">" : string2, 1, Integer::sum);
                    if (string.equals(string2) && !arrayList.contains(densityFunction2)) {
                        arrayList.add(densityFunction2);
                    }
                    return densityFunction;
                }
            });
        }
        catch (RuntimeException runtimeException) {
            throw new Unsupported("could not walk the live tree for " + string + ": " + String.valueOf(runtimeException));
        }
        if (Boolean.getBoolean("streamsreflowing.prepdiag")) {
            Constants.LOG.info("[SR-PREP] live walk for {}: {} nodes visited, {} matched; types seen = {}", string, nArray[0], arrayList.size(), treeMap);
        }
        return arrayList;
    }

    private FieldForm.Spline b(JsonElement jsonElement) {
        String string = jsonElement.toString();
        FieldForm.Spline spline = this.l.get(string);
        if (spline != null) {
            return spline;
        }
        FieldForm.Spline spline2 = this.c(jsonElement);
        this.l.put(string, spline2);
        return spline2;
    }

    private FieldForm.Spline c(JsonElement jsonElement) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        FieldForm.Node node = this.a(jsonObject.get("coordinate"));
        JsonArray jsonArray = jsonObject.getAsJsonArray("points");
        int n = jsonArray.size();
        float[] fArray = new float[n];
        float[] fArray2 = new float[n];
        FieldForm.SplineValue[] splineValueArray = new FieldForm.SplineValue[n];
        for (int i = 0; i < n; ++i) {
            JsonObject jsonObject2 = jsonArray.get(i).getAsJsonObject();
            fArray[i] = jsonObject2.get("location").getAsFloat();
            fArray2[i] = jsonObject2.get("derivative").getAsFloat();
            JsonElement jsonElement2 = jsonObject2.get("value");
            splineValueArray[i] = jsonElement2.isJsonPrimitive() && jsonElement2.getAsJsonPrimitive().isNumber() ? new FieldForm.SplineConst(jsonElement2.getAsFloat()) : new FieldForm.SplineNested(this.b(jsonElement2));
        }
        return new FieldForm.Spline(node, fArray, splineValueArray, fArray2);
    }

    private FieldForm.Node a(JsonObject jsonObject) {
        double d = jsonObject.get("xz_scale").getAsDouble();
        double d2 = jsonObject.get("y_scale").getAsDouble();
        double d3 = jsonObject.get("xz_factor").getAsDouble();
        double d4 = jsonObject.get("y_factor").getAsDouble();
        double d5 = jsonObject.get("smear_scale_multiplier").getAsDouble();
        RandomSource randomSource = this.c.fromHashOf(ResourceLocation.tryParse("minecraft:terrain"));
        int n = this.a("blended:minLimit", FieldFormBuilder.a(randomSource, 16));
        int n2 = this.a("blended:maxLimit", FieldFormBuilder.a(randomSource, 16));
        int n3 = this.a("blended:main", FieldFormBuilder.a(randomSource, 8));
        return new FieldForm.Blended(n, n2, n3, 684.412 * d, 684.412 * d2, d3, d4, d5);
    }

    private int a(String string, FieldForm.PerlinState perlinState) {
        this.h.a.add(string);
        this.h.b.add(new FieldForm.NoiseState(string, false, perlinState, null, 1.0));
        return this.h.b.size() - 1;
    }

    private static FieldForm.PerlinState a(RandomSource randomSource, int n) {
        int[][] nArrayArray = new int[n][];
        double[] dArray = new double[n];
        double[] dArray2 = new double[n];
        double[] dArray3 = new double[n];
        double[] dArray4 = new double[n];
        Arrays.fill(dArray4, 1.0);
        int[] nArray = new int[n];
        nArray[0] = n - 1;
        for (int i = 1; i < n; ++i) {
            nArray[i] = n - 1 - i;
        }
        for (int n2 : nArray) {
            int n3;
            dArray[n2] = randomSource.nextDouble() * 256.0;
            dArray2[n2] = randomSource.nextDouble() * 256.0;
            dArray3[n2] = randomSource.nextDouble() * 256.0;
            int[] nArray2 = new int[256];
            for (n3 = 0; n3 < 256; ++n3) {
                nArray2[n3] = n3;
            }
            for (n3 = 0; n3 < 256; ++n3) {
                int n4 = randomSource.nextInt(256 - n3);
                int n5 = nArray2[n3];
                nArray2[n3] = nArray2[n3 + n4];
                nArray2[n3 + n4] = n5;
            }
            nArrayArray[n2] = nArray2;
        }
        return new FieldForm.PerlinState(Math.pow(2.0, -(n - 1)), 1.0, dArray4, nArrayArray, dArray, dArray2, dArray3);
    }

    private void c(DensityFunction densityFunction) {
        try {
            FieldFormBuilder.a(densityFunction).mapAll(new DensityFunction.Visitor(){

                @Override
                public DensityFunction apply(DensityFunction densityFunction) {
                    return densityFunction;
                }

                @Override
                public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noiseHolder) {
                    if (noiseHolder.noise() != null) {
                        noiseHolder.noiseData().unwrapKey().ifPresent(resourceKey -> FieldFormBuilder.this.m.putIfAbsent(resourceKey.location().toString(), noiseHolder));
                    }
                    return noiseHolder;
                }
            });
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private static boolean a(FieldForm.NoiseState noiseState, DensityFunction.NoiseHolder noiseHolder) {
        for (double[] dArray : n) {
            double d;
            double d2 = FormEval.a(noiseState, dArray[0], dArray[1], dArray[2]);
            if (Math.abs(d2 - (d = noiseHolder.getValue(dArray[0], dArray[1], dArray[2]))) <= 1.0E-9) continue;
            return false;
        }
        return true;
    }

    private static List<String> b(String string) {
        int n = string.indexOf(58);
        String string2 = n < 0 ? "minecraft" : string.substring(0, n);
        String string3 = n < 0 ? string : string.substring(n + 1);
        String string4 = string3.contains("/") ? string3.substring(string3.lastIndexOf(47) + 1) : string3;
        return List.of("minecraft:" + string4, string2 + ":" + string4);
    }

    private FieldForm.Node b(JsonObject jsonObject) {
        JsonArray jsonArray = jsonObject.getAsJsonArray("thresholds");
        JsonArray jsonArray2 = jsonObject.getAsJsonArray("functions");
        if (jsonArray == null || jsonArray2 == null || jsonArray2.size() != jsonArray.size() + 1) {
            throw new Unsupported("interval_select with " + String.valueOf(jsonArray == null ? "no" : Integer.valueOf(jsonArray.size())) + " thresholds and " + String.valueOf(jsonArray2 == null ? "no" : Integer.valueOf(jsonArray2.size())) + " functions");
        }
        double[] dArray = new double[jsonArray.size()];
        for (int i = 0; i < dArray.length; ++i) {
            dArray[i] = jsonArray.get(i).getAsDouble();
        }
        FieldForm.Node[] nodeArray = new FieldForm.Node[jsonArray2.size()];
        for (int i = 0; i < nodeArray.length; ++i) {
            nodeArray[i] = this.a(jsonArray2.get(i));
        }
        return new FieldForm.IntervalSelect(this.a(jsonObject.get("input")), dArray, nodeArray);
    }

    private int c(String string) {
        NormalNoise.NoiseParameters noiseParameters;
        Integer n = this.g.get(string);
        if (n != null) {
            return n;
        }
        Registry<NormalNoise.NoiseParameters> registry = this.b.registryOrThrow(Registries.NOISE);
        ResourceLocation resourceLocation = ResourceLocation.tryParse(string);
        NormalNoise.NoiseParameters noiseParameters2 = noiseParameters = resourceLocation == null ? null : registry.get(resourceLocation);
        if (noiseParameters == null) {
            throw new Unsupported("unresolvable noise: " + string);
        }
        FieldForm.NoiseState noiseState = FieldFormBuilder.a(string, this.c.fromHashOf(resourceLocation), noiseParameters);
        DensityFunction.NoiseHolder noiseHolder = this.m.get(string);
        if (noiseHolder != null && noiseHolder.noise() != null && !FieldFormBuilder.a(noiseState, noiseHolder)) {
            FieldForm.NoiseState noiseState2 = null;
            for (String string2 : FieldFormBuilder.b(string)) {
                FieldForm.NoiseState noiseState3;
                ResourceLocation resourceLocation2 = ResourceLocation.tryParse(string2);
                if (resourceLocation2 == null || !FieldFormBuilder.a(noiseState3 = FieldFormBuilder.a(string, this.c.fromHashOf(resourceLocation2), noiseParameters), noiseHolder)) continue;
                noiseState2 = noiseState3;
                break;
            }
            if (noiseState2 == null) {
                throw new Unsupported("noise " + string + " is seeded by a scheme we cannot reproduce");
            }
            noiseState = noiseState2;
        }
        this.h.a.add(string);
        this.h.b.add(noiseState);
        int n2 = this.h.b.size() - 1;
        this.g.put(string, n2);
        return n2;
    }

    private static FieldForm.NoiseState a(String string, RandomSource randomSource, NormalNoise.NoiseParameters noiseParameters) {
        int n = noiseParameters.firstOctave();
        DoubleList doubleList = noiseParameters.amplitudes();
        FieldForm.PerlinState perlinState = FieldFormBuilder.a(randomSource, n, doubleList);
        FieldForm.PerlinState perlinState2 = FieldFormBuilder.a(randomSource, n, doubleList);
        int n2 = Integer.MAX_VALUE;
        int n3 = Integer.MIN_VALUE;
        for (int i = 0; i < doubleList.size(); ++i) {
            if (doubleList.getDouble(i) == 0.0) continue;
            n2 = Math.min(n2, i);
            n3 = Math.max(n3, i);
        }
        double d = 0.1 * (1.0 + 1.0 / (double)(n3 - n2 + 1));
        double d2 = 0.16666666666666666 / d;
        return new FieldForm.NoiseState(string, true, perlinState, perlinState2, d2);
    }

    private static FieldForm.PerlinState a(RandomSource randomSource, int n, DoubleList doubleList) {
        PositionalRandomFactory positionalRandomFactory = randomSource.forkPositional();
        int n2 = doubleList.size();
        double[] dArray = doubleList.toDoubleArray();
        int[][] nArrayArray = new int[n2][];
        double[] dArray2 = new double[n2];
        double[] dArray3 = new double[n2];
        double[] dArray4 = new double[n2];
        for (int i = 0; i < n2; ++i) {
            int n3;
            if (dArray[i] == 0.0) continue;
            RandomSource randomSource2 = positionalRandomFactory.fromHashOf("octave_" + (n + i));
            dArray2[i] = randomSource2.nextDouble() * 256.0;
            dArray3[i] = randomSource2.nextDouble() * 256.0;
            dArray4[i] = randomSource2.nextDouble() * 256.0;
            int[] nArray = new int[256];
            for (n3 = 0; n3 < 256; ++n3) {
                nArray[n3] = n3;
            }
            for (n3 = 0; n3 < 256; ++n3) {
                int n4 = randomSource2.nextInt(256 - n3);
                int n5 = nArray[n3];
                nArray[n3] = nArray[n3 + n4];
                nArray[n3 + n4] = n5;
            }
            nArrayArray[i] = nArray;
        }
        double d = Math.pow(2.0, n);
        double d2 = Math.pow(2.0, n2 - 1) / (Math.pow(2.0, n2) - 1.0);
        return new FieldForm.PerlinState(d, d2, dArray, nArrayArray, dArray2, dArray3, dArray4);
    }

    static {
        n = new double[][]{{0.13, 7.5, -911.2}, {5001.7, -33.0, 12.9}, {-77.3, 120.0, 77.3}, {123456.0, 0.0, -654321.0}};
    }

    public static final class Unsupported
    extends RuntimeException {
        public Unsupported(String string) {
            super(string);
        }
    }

    private static final class Ctx
    implements DensityFunction.FunctionContext {
        int a;
        int b;
        int c;

        private Ctx() {
        }

        @Override
        public int blockX() {
            return this.a;
        }

        @Override
        public int blockY() {
            return this.b;
        }

        @Override
        public int blockZ() {
            return this.c;
        }
    }
}

