/* 鏂囦欢鑱岃矗锛氬皢 Minecraft density function DAG 缂栬瘧涓?OpenCL 鍙墽琛岀殑绱у噾澶氭牴鍥俱€?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minecraft density graph 鍒?OpenCL 鑺傜偣琛ㄧ殑缂栬瘧鍣ㄣ€? */
public final class DensityGraphCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger("PlainCraft Streams GPU");
    private final Map<DensityFunction, Integer> seen = new IdentityHashMap<>();
    private final List<DensityGraphNode> nodes = new ArrayList<>();
    private final List<Double> constants = new ArrayList<>();
    private final NoiseTableExtractor noiseTables = new NoiseTableExtractor();
    private final List<OpenCLSpline> splines = new ArrayList<>();
    private final List<Integer> interpolatedNodes = new ArrayList<>();
    private final Map<String, Integer> extensionNodeCounts = new HashMap<>();
    private final boolean preserveInterpolation;

    private DensityGraphCompiler(boolean preserveInterpolation) {
        this.preserveInterpolation = preserveInterpolation;
    }

    public static DensityGraphCompileResult compile(DensityFunction root) {
        if (root == null) {
            return DensityGraphCompileResult.unsupported("density root is null");
        }
        DensityGraphCompiler compiler = new DensityGraphCompiler(false);
        try {
            int rootNode = compiler.compileNode(root);
            compiler.logExtensionNodes();
            return DensityGraphCompileResult.supported(new DensityGraphProgram(
                    rootNode,
                    compiler.nodes,
                    compiler.constants,
                    compiler.noiseTables.toTables(),
                    compiler.splines));
        } catch (TemporaryDensityGraphException e) {
            return DensityGraphCompileResult.retryable(e.getMessage());
        } catch (UnsupportedDensityGraphException e) {
            return DensityGraphCompileResult.unsupported(e.getMessage());
        } catch (Throwable t) {
            LOGGER.info("OpenCL density graph compile failed: root={} error={} message={}",
                    root.getClass().getName(),
                    t.getClass().getName(),
                    t.getMessage(),
                    t);
            return DensityGraphCompileResult.unsupported("density graph compile failed: " + t.getClass().getSimpleName());
        }
    }

    public static DensityGraphCompileResult compile(Map<DensityGraphRoot, DensityFunction> roots) {
        if (roots == null || roots.isEmpty() || roots.get(DensityGraphRoot.FINAL_DENSITY) == null) {
            return DensityGraphCompileResult.unsupported("accurate density roots are incomplete");
        }
        DensityGraphCompiler compiler = new DensityGraphCompiler(true);
        try {
            EnumMap<DensityGraphRoot, Integer> compiledRoots = new EnumMap<>(DensityGraphRoot.class);
            for (DensityGraphRoot root : DensityGraphRoot.values()) {
                DensityFunction function = roots.get(root);
                if (function != null) {
                    compiledRoots.put(root, compiler.compileNode(function));
                }
            }
            int finalRoot = compiledRoots.get(DensityGraphRoot.FINAL_DENSITY);
            compiler.logExtensionNodes();
            return DensityGraphCompileResult.supported(new DensityGraphProgram(
                    finalRoot,
                    compiledRoots,
                    compiler.interpolatedNodes,
                    compiler.nodes,
                    compiler.constants,
                    compiler.noiseTables.toTables(),
                    compiler.splines));
        } catch (TemporaryDensityGraphException e) {
            return DensityGraphCompileResult.retryable(e.getMessage());
        } catch (UnsupportedDensityGraphException e) {
            return DensityGraphCompileResult.unsupported(e.getMessage());
        } catch (Throwable failure) {
            LOGGER.info("OpenCL accurate density graph compile failed: error={} message={}",
                    failure.getClass().getName(), failure.getMessage(), failure);
            return DensityGraphCompileResult.unsupported("accurate density graph compile failed: "
                    + failure.getClass().getSimpleName());
        }
    }

    private int compileNode(DensityFunction function) {
        DensityFunction unwrapped = DensityGraphReflection.unwrapC2meCompiledDensity(function);
        if (unwrapped != function) {
            function = unwrapped;
        }
        Integer cached = seen.get(function);
        if (cached != null) {
            return cached;
        }

        int index = compileUncached(function);
        seen.put(function, index);
        return index;
    }

    private int compileUncached(DensityFunction function) {
        Integer extensionNode = compileExtension(function);
        if (extensionNode != null) {
            return extensionNode;
        }
        String typeId = densityTypeId(function);
        return switch (typeId) {
            case "constant", "Constant" -> constant(function);
            case "blend_alpha", "BlendAlpha" -> constant(1.0D);
            case "blend_offset", "BlendOffset", "beardifier", "BeardifierMarker" -> constant(0.0D);
            case "blend_density", "BlendDensity" -> compileNode(recordFunction(function, 0, "blend_density input"));
            case "add", "mul", "min", "max" -> twoArgument(function, typeId);
            case "Ap2", "MulOrAdd" -> twoArgument(function, null);
            case "clamp", "Clamp" -> clamp(function);
            case "clamp_to_nearest_unit", "ClampToNearestUnit" -> clampToNearestUnit(function);
            case "abs", "square", "cube", "half_negative", "quarter_negative", "squeeze" -> mapped(function, typeId);
            case "Mapped" -> mapped(function, null);
            case "y_clamped_gradient", "YClampedGradient" -> yClampedGradient(function);
            case "range_choice", "RangeChoice" -> rangeChoice(function);
            case "marker", "Marker" -> marker(function);
            case "holder", "HolderHolder" -> holder(function);
            case "noise", "Noise" -> noise(function);
            case "shifted_noise", "ShiftedNoise" -> shiftedNoise(function);
            case "shift_a", "ShiftA" -> shift(function, DensityGraphNodeType.SHIFT_A);
            case "shift_b", "ShiftB" -> shift(function, DensityGraphNodeType.SHIFT_B);
            case "shift", "Shift" -> shift(function, DensityGraphNodeType.SHIFT);
            case "weird_scaled_sampler", "WeirdScaledSampler" -> weirdScaledSampler(function);
            case "spline", "Spline" -> spline(function);
            case "old_blended_noise", "BlendedNoise" -> blendedNoise(function);
            case "end_islands", "EndIslandDensityFunction" -> endIsland(function);
            default -> unsupported("unsupported density node: " + function.getClass().getName());
        };
    }

    private Integer compileExtension(DensityFunction function) {
        String className = function.getClass().getName();
        int node = switch (className) {
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.FastNoiseDensityFunction"
                    -> lithostitchedFastNoise(function);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.AxisDensityFunction"
                    -> lithostitchedAxis(function);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.MixDensityFunction"
                    -> lithostitchedMix(function);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.SelectDensityFunction"
                    -> lithostitchedSelect(function);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.ShiftDensityFunction"
                    -> lithostitchedShift(function);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.marker.MergedDensityFunction"
                    -> compileNode(recordFunction(function, 2, "Lithostitched merged full input"));
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.SinDensityFunction"
                    -> lithostitchedTransformer(function, DensityGraphNodeType.SIN);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.CosDensityFunction"
                    -> lithostitchedTransformer(function, DensityGraphNodeType.COS);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.FloorDensityFunction"
                    -> lithostitchedTransformer(function, DensityGraphNodeType.FLOOR);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.CeilDensityFunction"
                    -> lithostitchedTransformer(function, DensityGraphNodeType.CEIL);
            case "dev.worldgen.lithostitched.impl.worldgen.densityfunction.SqrtDensityFunction"
                    -> lithostitchedTransformer(function, DensityGraphNodeType.SQRT_SIGNED);
            case "dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise"
                    -> tectonicConfigNoise(function);
            case "dev.worldgen.tectonic.worldgen.densityfunction.ConfigConstant"
                    -> tectonicConfigConstant(function);
            case "dev.worldgen.tectonic.worldgen.densityfunction.ConfigClamp"
                    -> tectonicConfigClamp(function);
            case "dev.worldgen.tectonic.worldgen.densityfunction.Invert"
                    -> tectonicInvert(function);
            default -> -1;
        };
        if (node < 0) {
            return null;
        }
        extensionNodeCounts.merge(className, 1, Integer::sum);
        return node;
    }

    private static String densityTypeId(DensityFunction function) {
        if (function instanceof DensityFunctions.HolderHolder) {
            return "holder";
        }
        if (function instanceof DensityFunctions.MarkerOrMarked) {
            return "marker";
        }
        try {
            ResourceLocation id = BuiltInRegistries.DENSITY_FUNCTION_TYPE.getKey(function.codec().codec());
            if (id != null) {
                return id.getPath();
            }
        } catch (RuntimeException ignored) {
        }
        return function.getClass().getSimpleName();
    }

    private int constant(DensityFunction function) {
        double value = function.minValue();
        if (Double.isNaN(value)) {
            throw new UnsupportedDensityGraphException("constant node missing value");
        }
        return constant(value);
    }

    private int constant(double value) {
        constants.add(value);
        return add(new DensityGraphNode(DensityGraphNodeType.CONSTANT, -1, -1, constants.size() - 1, -1, value, 0.0D, 0.0D, 0.0D));
    }

    private int twoArgument(DensityFunction function, String operation) {
        Object firstInput = DensityGraphReflection.readRecord(function, 1);
        Object secondInput = DensityGraphReflection.readRecord(function, 2);
        if (!(firstInput instanceof DensityFunction firstFunction)) {
            throw new UnsupportedDensityGraphException("two-argument node missing input");
        }
        int left;
        int right;
        if (secondInput instanceof DensityFunction secondFunction) {
            left = compileNode(firstFunction);
            right = compileNode(secondFunction);
        } else {
            double argument = DensityGraphReflection.readRecordDouble(function, 4, Double.NaN);
            if (Double.isNaN(argument)) {
                throw new UnsupportedDensityGraphException("two-argument node missing constant input");
            }
            left = constant(argument);
            right = compileNode(firstFunction);
        }
        String op = operation == null ? serializedName(DensityGraphReflection.readRecord(function, 0)) : operation;
        DensityGraphNodeType nodeType = switch (op == null ? "" : op.toLowerCase()) {
            case "add" -> DensityGraphNodeType.ADD;
            case "mul" -> DensityGraphNodeType.MUL;
            case "min" -> DensityGraphNodeType.MIN;
            case "max" -> DensityGraphNodeType.MAX;
            default -> throw new UnsupportedDensityGraphException("unsupported two-argument op: " + op);
        };
        return add(new DensityGraphNode(nodeType, left, right, -1, -1, 0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int clamp(DensityFunction function) {
        DensityFunction input = recordFunction(function, 0, "clamp input");
        int child = compileNode(input);
        double min = function.minValue();
        double max = function.maxValue();
        return add(new DensityGraphNode(DensityGraphNodeType.CLAMP, child, -1, -1, -1, min, max, 0.0D, 0.0D));
    }

    private int tectonicInvert(DensityFunction function) {
        DensityFunction input = recordFunction(function, 0, "Tectonic invert input");
        int child = compileNode(input);
        return add(new DensityGraphNode(DensityGraphNodeType.INVERT, child, -1, -1, -1, 0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int tectonicConfigConstant(DensityFunction function) {
        double value = DensityGraphReflection.readRecordDouble(function, 0, Double.NaN);
        if (Double.isNaN(value)) {
            throw new UnsupportedDensityGraphException("Tectonic config constant is missing value");
        }
        return constant(value);
    }

    private int tectonicConfigClamp(DensityFunction function) {
        int input = compileNode(recordFunction(function, 0, "Tectonic config clamp input"));
        int min = compileNode(recordFunction(function, 1, "Tectonic config clamp min"));
        int max = compileNode(recordFunction(function, 2, "Tectonic config clamp max"));
        int lowerBounded = binary(DensityGraphNodeType.MAX, input, min);
        return binary(DensityGraphNodeType.MIN, lowerBounded, max);
    }

    private int tectonicConfigNoise(DensityFunction function) {
        Object noiseHolder = DensityGraphReflection.readRecord(function, 0);
        int noiseIndex = normalNoiseIndex(noiseHolder);
        int shiftX = compileNode(recordFunction(function, 1, "Tectonic config noise shiftX"));
        int shiftZ = compileNode(recordFunction(function, 2, "Tectonic config noise shiftZ"));
        double scale = DensityGraphReflection.readRecordDouble(function, 3, Double.NaN);
        double multiplier = DensityGraphReflection.readRecordDouble(function, 4, Double.NaN);
        double offset = DensityGraphReflection.readRecordDouble(function, 5, Double.NaN);
        Object smootherValue = DensityGraphReflection.readRecord(function, 6);
        if (Double.isNaN(scale) || Double.isNaN(multiplier) || Double.isNaN(offset)
                || !(smootherValue instanceof Boolean smootherScaling)) {
            throw new UnsupportedDensityGraphException("Tectonic config noise is missing parameters");
        }
        return add(new DensityGraphNode(
                DensityGraphNodeType.TECTONIC_CONFIG_NOISE,
                shiftX,
                -1,
                noiseIndex,
                shiftZ,
                scale,
                multiplier,
                offset,
                smootherScaling ? 1.0D : 0.0D));
    }

    private int lithostitchedAxis(DensityFunction function) {
        String axis = serializedName(DensityGraphReflection.readRecord(function, 0));
        int axisCode = switch (axis == null ? "" : axis.toLowerCase()) {
            case "x" -> 0;
            case "y" -> 1;
            case "z" -> 2;
            default -> throw new UnsupportedDensityGraphException("unsupported Lithostitched axis: " + axis);
        };
        return add(new DensityGraphNode(DensityGraphNodeType.AXIS, -1, -1, axisCode, -1,
                0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int lithostitchedMix(DensityFunction function) {
        int input = compileNode(recordFunction(function, 0, "Lithostitched mix input"));
        int argument1 = compileNode(recordFunction(function, 1, "Lithostitched mix argument1"));
        int argument2 = compileNode(recordFunction(function, 2, "Lithostitched mix argument2"));
        return add(new DensityGraphNode(DensityGraphNodeType.MIX, input, argument1, argument2, -1,
                0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int lithostitchedSelect(DensityFunction function) {
        int selector = compileNode(recordFunction(function, 0, "Lithostitched select input"));
        int result = compileNode(recordFunction(function, 1, "Lithostitched select fallback"));
        Object selectionValue = DensityGraphReflection.readRecord(function, 2);
        if (!(selectionValue instanceof List<?> selections)) {
            throw new UnsupportedDensityGraphException("Lithostitched select is missing selections");
        }
        for (int i = selections.size() - 1; i >= 0; i--) {
            Object selection = selections.get(i);
            Object range = DensityGraphReflection.readRecord(selection, 0);
            Object minValue = DensityGraphReflection.readRecord(range, 0);
            Object maxValue = DensityGraphReflection.readRecord(range, 1);
            Object branchValue = DensityGraphReflection.readRecord(selection, 1);
            if (!(minValue instanceof Number min) || !(maxValue instanceof Number max)
                    || !(branchValue instanceof DensityFunction branch)) {
                throw new UnsupportedDensityGraphException("Lithostitched select has an invalid selection");
            }
            int branchNode = compileNode(branch);
            result = add(new DensityGraphNode(
                    DensityGraphNodeType.RANGE_CHOICE_INCLUSIVE,
                    selector,
                    branchNode,
                    result,
                    -1,
                    min.doubleValue(),
                    max.doubleValue(),
                    0.0D,
                    0.0D));
        }
        return result;
    }

    private int lithostitchedShift(DensityFunction function) {
        DensityFunction input = recordFunction(function, 0, "Lithostitched shift input");
        DensityFunction shiftX = recordFunction(function, 1, "Lithostitched shiftX");
        DensityFunction shiftY = recordFunction(function, 2, "Lithostitched shiftY");
        DensityFunction shiftZ = recordFunction(function, 3, "Lithostitched shiftZ");
        if (!isAlwaysZero(shiftX) || !isAlwaysZero(shiftY) || !isAlwaysZero(shiftZ)) {
            throw new UnsupportedDensityGraphException("Lithostitched non-zero coordinate shift is unsupported");
        }
        return compileNode(input);
    }

    private int lithostitchedTransformer(DensityFunction function, DensityGraphNodeType nodeType) {
        Object argumentValue = DensityGraphReflection.read(function, "argument");
        if (!(argumentValue instanceof DensityFunction argument)) {
            throw new UnsupportedDensityGraphException("Lithostitched transformer is missing argument");
        }
        int child = compileNode(argument);
        return add(new DensityGraphNode(nodeType, child, -1, -1, -1,
                0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int lithostitchedFastNoise(DensityFunction function) {
        if (!LithostitchedKernelSupport.hasRandomVectors()) {
            throw new UnsupportedDensityGraphException("Lithostitched FastNoise random vectors are unavailable");
        }
        Object holderValue = DensityGraphReflection.readRecord(function, 0);
        if (!(holderValue instanceof Holder<?> holder)) {
            throw new UnsupportedDensityGraphException("Lithostitched FastNoise config holder is missing");
        }
        Object config = holder.value();
        if (config == null || !config.getClass().getName().equals(
                "dev.worldgen.lithostitched.impl.worldgen.fastnoise.CellularNoiseType")) {
            throw new UnsupportedDensityGraphException("unsupported Lithostitched FastNoise config: "
                    + (config == null ? "null" : config.getClass().getName()));
        }
        String distanceFunction = enumName(DensityGraphReflection.read(config, "distanceFunction"));
        if (!"EUCLIDEAN".equals(distanceFunction)) {
            throw new UnsupportedDensityGraphException(
                    "unsupported Lithostitched cellular distance function: " + distanceFunction);
        }
        String returnType = enumName(DensityGraphReflection.read(config, "returnType"));
        int returnTypeCode = cellularReturnTypeCode(returnType);
        double frequency = DensityGraphReflection.readDouble(config, "frequency", Double.NaN);
        double jitter = DensityGraphReflection.readDouble(config, "jitter", Double.NaN);
        Object fnl = DensityGraphReflection.read(config, "fnl");
        int seed = DensityGraphReflection.readInt(fnl, "mSeed", Integer.MIN_VALUE);
        double xzScale = DensityGraphReflection.readRecordDouble(function, 1, Double.NaN);
        double yScale = DensityGraphReflection.readRecordDouble(function, 2, Double.NaN);
        if (Double.isNaN(frequency) || Double.isNaN(jitter) || seed == Integer.MIN_VALUE
                || Double.isNaN(xzScale) || Double.isNaN(yScale)) {
            throw new UnsupportedDensityGraphException("Lithostitched FastNoise runtime state is incomplete");
        }
        int shiftX = compileNode(recordFunction(function, 3, "Lithostitched FastNoise shiftX"));
        int shiftY = compileNode(recordFunction(function, 4, "Lithostitched FastNoise shiftY"));
        int shiftZ = compileNode(recordFunction(function, 5, "Lithostitched FastNoise shiftZ"));
        int parameters = add(new DensityGraphNode(
                DensityGraphNodeType.CONSTANT,
                -1,
                -1,
                -1,
                -1,
                (double) seed,
                frequency,
                jitter,
                (double) returnTypeCode));
        return add(new DensityGraphNode(
                DensityGraphNodeType.CELLULAR_FAST_NOISE,
                shiftX,
                shiftY,
                shiftZ,
                parameters,
                xzScale,
                yScale,
                0.0D,
                0.0D));
    }

    private int binary(DensityGraphNodeType type, int left, int right) {
        return add(new DensityGraphNode(type, left, right, -1, -1,
                0.0D, 0.0D, 0.0D, 0.0D));
    }

    private static boolean isAlwaysZero(DensityFunction function) {
        return Double.compare(function.minValue(), 0.0D) == 0
                && Double.compare(function.maxValue(), 0.0D) == 0;
    }

    private static String enumName(Object value) {
        return value instanceof Enum<?> enumValue ? enumValue.name() : serializedName(value);
    }

    private static int cellularReturnTypeCode(String returnType) {
        return switch (returnType == null ? "" : returnType) {
            case "CELL_VALUE" -> 0;
            case "DISTANCE" -> 1;
            case "DISTANCE_2" -> 2;
            case "DISTANCE_2_ADD" -> 3;
            case "DISTANCE_2_SUB" -> 4;
            case "DISTANCE_2_MUL" -> 5;
            case "DISTANCE_2_DIV" -> 6;
            default -> throw new UnsupportedDensityGraphException(
                    "unsupported Lithostitched cellular return type: " + returnType);
        };
    }

    private void logExtensionNodes() {
        if (!extensionNodeCounts.isEmpty()) {
            LOGGER.info("OpenCL extension density nodes compiled: {}", extensionNodeCounts);
        }
    }

    private int clampToNearestUnit(DensityFunction function) {
        DensityFunction input = (DensityFunction) DensityGraphReflection.read(function, "function");
        if (input == null) {
            throw new UnsupportedDensityGraphException("clamp_to_nearest_unit node missing function");
        }
        int child = compileNode(input);
        int resolution = DensityGraphReflection.readInt(function, "resolution", Integer.MIN_VALUE);
        if (resolution == Integer.MIN_VALUE) {
            throw new UnsupportedDensityGraphException("clamp_to_nearest_unit node missing resolution");
        }
        return add(new DensityGraphNode(DensityGraphNodeType.CLAMP_TO_NEAREST_UNIT, child, -1, resolution, -1, 0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int mapped(DensityFunction function, String operation) {
        DensityFunction input = recordFunction(function, 1, "mapped input");
        int child = compileNode(input);
        String op = operation == null ? serializedName(DensityGraphReflection.readRecord(function, 0)) : operation;
        DensityGraphNodeType nodeType = switch (op == null ? "" : op.toLowerCase()) {
            case "abs" -> DensityGraphNodeType.ABS;
            case "square" -> DensityGraphNodeType.SQUARE;
            case "cube" -> DensityGraphNodeType.CUBE;
            case "half_negative" -> DensityGraphNodeType.HALF_NEGATIVE;
            case "quarter_negative" -> DensityGraphNodeType.QUARTER_NEGATIVE;
            case "squeeze" -> DensityGraphNodeType.SQUEEZE;
            case "invert" -> DensityGraphNodeType.INVERT;
            default -> throw new UnsupportedDensityGraphException("unsupported mapped op: " + op);
        };
        return add(new DensityGraphNode(nodeType, child, -1, -1, -1, 0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int yClampedGradient(DensityFunction function) {
        int fromY = DensityGraphReflection.readRecordInt(function, 0, Integer.MIN_VALUE);
        int toY = DensityGraphReflection.readRecordInt(function, 1, Integer.MIN_VALUE);
        double fromValue = DensityGraphReflection.readRecordDouble(function, 2, Double.NaN);
        double toValue = DensityGraphReflection.readRecordDouble(function, 3, Double.NaN);
        if (fromY == Integer.MIN_VALUE || toY == Integer.MIN_VALUE || Double.isNaN(fromValue) || Double.isNaN(toValue)) {
            throw new UnsupportedDensityGraphException("y_clamped_gradient node missing values");
        }
        return add(new DensityGraphNode(DensityGraphNodeType.Y_CLAMPED_GRADIENT, -1, -1, fromY, toY, fromValue, toValue, 0.0D, 0.0D));
    }

    private int rangeChoice(DensityFunction function) {
        DensityFunction input = recordFunction(function, 0, "range_choice input");
        Object inRangeValue = DensityGraphReflection.readRecord(function, 3);
        Object outOfRangeValue = DensityGraphReflection.readRecord(function, 4);
        DensityFunction whenInRange = inRangeValue instanceof DensityFunction value ? value : null;
        DensityFunction whenOutOfRange = outOfRangeValue instanceof DensityFunction value ? value : null;
        if (input == null || whenInRange == null || whenOutOfRange == null) {
            throw new UnsupportedDensityGraphException("range_choice node missing input");
        }
        int condition = compileNode(input);
        int inRange = compileNode(whenInRange);
        int outOfRange = compileNode(whenOutOfRange);
        double min = DensityGraphReflection.readRecordDouble(function, 1, Double.NaN);
        double max = DensityGraphReflection.readRecordDouble(function, 2, Double.NaN);
        if (Double.isNaN(min) || Double.isNaN(max)) {
            throw new UnsupportedDensityGraphException("range_choice node missing bounds");
        }
        return add(new DensityGraphNode(DensityGraphNodeType.RANGE_CHOICE, condition, inRange, outOfRange, -1, min, max, 0.0D, 0.0D));
    }

    private int marker(DensityFunction function) {
        if (!(function instanceof DensityFunctions.MarkerOrMarked marker)) {
            throw new UnsupportedDensityGraphException("marker node missing wrapped function");
        }
        int wrapped = compileNode(marker.wrapped());
        Object markerType = marker.type();
        String markerName = markerType instanceof StringRepresentable named
                ? named.getSerializedName()
                : String.valueOf(markerType);
        if (!preserveInterpolation || !"interpolated".equalsIgnoreCase(markerName)) {
            return wrapped;
        }
        int slot = interpolatedNodes.size();
        int node = add(new DensityGraphNode(
                DensityGraphNodeType.INTERPOLATED,
                wrapped,
                -1,
                slot,
                -1,
                0.0D,
                0.0D,
                0.0D,
                0.0D));
        interpolatedNodes.add(node);
        return node;
    }

    private int holder(DensityFunction function) {
        if (!(function instanceof DensityFunctions.HolderHolder holderHolder)) {
            throw new TemporaryDensityGraphException("holder node is not bound");
        }
        DensityFunction nested = holderHolder.function().value();
        if (nested == null) {
            throw new TemporaryDensityGraphException("holder node is not bound");
        }
        return compileNode(nested);
    }

    private int noise(DensityFunction function) {
        int noiseIndex = normalNoiseIndex(DensityGraphReflection.readRecord(function, 0));
        double xzScale = DensityGraphReflection.readRecordDouble(function, 1, Double.NaN);
        double yScale = DensityGraphReflection.readRecordDouble(function, 2, Double.NaN);
        if (Double.isNaN(xzScale) || Double.isNaN(yScale)) {
            throw new UnsupportedDensityGraphException("noise node missing scale");
        }
        return add(new DensityGraphNode(DensityGraphNodeType.NOISE, -1, -1, noiseIndex, -1, xzScale, yScale, 0.0D, 0.0D));
    }

    private int shiftedNoise(DensityFunction function) {
        int x = compileNode(recordFunction(function, 0, "shifted_noise shiftX"));
        int y = compileNode(recordFunction(function, 1, "shifted_noise shiftY"));
        int z = compileNode(recordFunction(function, 2, "shifted_noise shiftZ"));
        int noiseIndex = normalNoiseIndex(DensityGraphReflection.readRecord(function, 5));
        double xzScale = DensityGraphReflection.readRecordDouble(function, 3, Double.NaN);
        double yScale = DensityGraphReflection.readRecordDouble(function, 4, Double.NaN);
        if (Double.isNaN(xzScale) || Double.isNaN(yScale)) {
            throw new UnsupportedDensityGraphException("shifted_noise node missing scale");
        }
        return add(new DensityGraphNode(DensityGraphNodeType.SHIFTED_NOISE, x, y, z, noiseIndex, xzScale, yScale, 0.0D, 0.0D));
    }

    private int shift(DensityFunction function, DensityGraphNodeType nodeType) {
        int noiseIndex = normalNoiseIndex(DensityGraphReflection.readRecord(function, 0));
        return add(new DensityGraphNode(nodeType, -1, -1, noiseIndex, -1, 0.25D, 4.0D, 0.0D, 0.0D));
    }

    private int weirdScaledSampler(DensityFunction function) {
        int mapperType = rarityMapperType(DensityGraphReflection.readRecord(function, 2));
        int child = compileNode(recordFunction(function, 0, "weird_scaled_sampler input"));
        int noiseIndex = normalNoiseIndex(DensityGraphReflection.readRecord(function, 1));
        return add(new DensityGraphNode(DensityGraphNodeType.WEIRD_SCALED_SAMPLER, child, -1, noiseIndex, mapperType, 0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int blendedNoise(DensityFunction function) {
        if (!(function instanceof BlendedNoise blendedNoise)) {
            throw new UnsupportedDensityGraphException("blended noise node has unexpected implementation");
        }
        PerlinNoise minLimit = (PerlinNoise) DensityGraphReflection.read(blendedNoise, "minLimitNoise");
        PerlinNoise maxLimit = (PerlinNoise) DensityGraphReflection.read(blendedNoise, "maxLimitNoise");
        PerlinNoise main = (PerlinNoise) DensityGraphReflection.read(blendedNoise, "mainNoise");
        double xzMultiplier = DensityGraphReflection.readDouble(blendedNoise, "xzMultiplier", Double.NaN);
        double yMultiplier = DensityGraphReflection.readDouble(blendedNoise, "yMultiplier", Double.NaN);
        double xzFactor = DensityGraphReflection.readDouble(blendedNoise, "xzFactor", Double.NaN);
        double yFactor = DensityGraphReflection.readDouble(blendedNoise, "yFactor", Double.NaN);
        double smearScale = DensityGraphReflection.readDouble(blendedNoise, "smearScaleMultiplier", Double.NaN);
        if (minLimit == null || maxLimit == null || main == null
                || Double.isNaN(xzMultiplier) || Double.isNaN(yMultiplier)
                || Double.isNaN(xzFactor) || Double.isNaN(yFactor) || Double.isNaN(smearScale)) {
            throw new UnsupportedDensityGraphException("blended noise node is missing runtime state");
        }
        int smearNode = constant(smearScale);
        return add(new DensityGraphNode(
                DensityGraphNodeType.BLENDED_NOISE,
                noiseTables.perlinNoiseIndex(minLimit),
                noiseTables.perlinNoiseIndex(maxLimit),
                noiseTables.perlinNoiseIndex(main),
                smearNode,
                xzMultiplier,
                yMultiplier,
                xzFactor,
                yFactor));
    }

    private int endIsland(DensityFunction function) {
        Object simplexObject = DensityGraphReflection.read(function, "islandNoise");
        if (!(simplexObject instanceof SimplexNoise simplexNoise)) {
            throw new UnsupportedDensityGraphException("end island node is missing simplex noise");
        }
        return add(new DensityGraphNode(
                DensityGraphNodeType.END_ISLAND,
                -1,
                -1,
                noiseTables.simplexNoiseIndex(simplexNoise),
                -1,
                0.0D,
                0.0D,
                0.0D,
                0.0D));
    }

    private int spline(DensityFunction function) {
        if (!(function instanceof DensityFunctions.Spline spline)) {
            throw new UnsupportedDensityGraphException("spline node has unexpected implementation");
        }
        return compileSplineValueNode(spline.spline());
    }

    private int compileSplineValueNode(Object spline) {
        if (spline == null) {
            throw new UnsupportedDensityGraphException("spline node missing spline");
        }
        if (spline instanceof CubicSpline.Constant<?, ?> constantSpline) {
            return constant(constantSpline.value());
        }
        if (!(spline instanceof CubicSpline.Multipoint<?, ?>)) {
            throw new UnsupportedDensityGraphException("unsupported spline type: " + spline.getClass().getName());
        }
        int splineIndex = compileSplineTable(spline);
        return add(new DensityGraphNode(DensityGraphNodeType.SPLINE, -1, -1, splineIndex, -1, 0.0D, 0.0D, 0.0D, 0.0D));
    }

    private int compileSplineTable(Object spline) {
        if (spline == null) {
            throw new UnsupportedDensityGraphException("spline node missing spline");
        }

        if (!(spline instanceof CubicSpline.Multipoint<?, ?> multipoint)
                || !(multipoint.coordinate() instanceof DensityFunctions.Spline.Coordinate coordinate)) {
            throw new UnsupportedDensityGraphException("spline coordinate is not a density function");
        }
        int coordinateNode = compileNode(coordinate.function().value());

        float[] locations = multipoint.locations();
        List<?> values = multipoint.values();
        float[] derivatives = multipoint.derivatives();
        if (locations.length != values.size() || locations.length != derivatives.length) {
            throw new UnsupportedDensityGraphException("spline table size mismatch");
        }

        List<Double> locationList = new ArrayList<>(locations.length);
        List<Integer> valueNodes = new ArrayList<>(locations.length);
        List<Double> derivativeList = new ArrayList<>(locations.length);
        for (int i = 0; i < locations.length; i++) {
            locationList.add((double) locations[i]);
            valueNodes.add(compileSplineValueNode(values.get(i)));
            derivativeList.add((double) derivatives[i]);
        }

        int index = splines.size();
        splines.add(new OpenCLSpline(coordinateNode, locationList, valueNodes, derivativeList));
        return index;
    }

    private int normalNoiseIndex(Object noiseHolder) {
        if (!(noiseHolder instanceof DensityFunction.NoiseHolder holder) || holder.noise() == null) {
            throw new UnsupportedDensityGraphException("noise holder has no NormalNoise instance");
        }
        try {
            return noiseTables.normalNoiseIndex(holder.noise());
        } catch (Throwable t) {
            throw new UnsupportedDensityGraphException("noise table extraction failed: " + t.getClass().getSimpleName());
        }
    }

    private static DensityFunction recordFunction(Object owner, int componentIndex, String description) {
        Object value = DensityGraphReflection.readRecord(owner, componentIndex);
        if (!(value instanceof DensityFunction function)) {
            throw new UnsupportedDensityGraphException(description + " missing");
        }
        return function;
    }

    private static int rarityMapperType(Object mapper) {
        if (mapper == null) {
            throw new UnsupportedDensityGraphException("weird_scaled_sampler missing rarity mapper");
        }
        String serializedName = serializedName(mapper);
        if ("type_1".equals(serializedName)) {
            return 1;
        }
        if ("type_2".equals(serializedName)) {
            return 2;
        }
        throw new UnsupportedDensityGraphException("unsupported weird_scaled_sampler mapper: " + mapper);
    }

    private int add(DensityGraphNode node) {
        nodes.add(node);
        return nodes.size() - 1;
    }

    private static String serializedName(Object value) {
        if (value instanceof StringRepresentable representable) {
            return representable.getSerializedName();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return value == null ? null : String.valueOf(value);
    }

    private static int unsupported(String reason) {
        throw new UnsupportedDensityGraphException(reason);
    }

    private static final class TemporaryDensityGraphException extends RuntimeException {
        private TemporaryDensityGraphException(String message) {
            super(message);
        }
    }

    private static final class UnsupportedDensityGraphException extends RuntimeException {
        private UnsupportedDensityGraphException(String message) {
            super(message);
        }
    }
}

