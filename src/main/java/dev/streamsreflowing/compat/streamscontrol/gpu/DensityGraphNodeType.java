/* 鏂囦欢鑱岃矗锛氬畾涔?Java density graph 涓?OpenCL evaluator 鍏变韩鐨勮妭鐐圭紪鍙枫€?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

/**
 * Density graph 鑺傜偣绫诲瀷銆? */
public enum DensityGraphNodeType {
    CONSTANT,
    ADD,
    MUL,
    MIN,
    MAX,
    CLAMP,
    ABS,
    SQUARE,
    CUBE,
    HALF_NEGATIVE,
    QUARTER_NEGATIVE,
    SQUEEZE,
    INVERT,
    Y_CLAMPED_GRADIENT,
    RANGE_CHOICE,
    NOISE,
    SHIFTED_NOISE,
    SHIFT_A,
    SHIFT_B,
    SHIFT,
    SPLINE,
    WEIRD_SCALED_SAMPLER,
    CLAMP_TO_NEAREST_UNIT,
    MARKER,
    INTERPOLATED,
    BLENDED_NOISE,
    END_ISLAND,
    AXIS,
    MIX,
    RANGE_CHOICE_INCLUSIVE,
    SIN,
    COS,
    FLOOR,
    CEIL,
    SQRT_SIGNED,
    CELLULAR_FAST_NOISE,
    TECTONIC_CONFIG_NOISE
}

