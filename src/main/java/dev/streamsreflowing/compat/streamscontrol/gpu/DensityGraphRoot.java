/* 鏂囦欢鑱岃矗锛氭爣璇嗙簿纭湴褰㈤噰鏍锋墍闇€鐨?density graph 杈撳嚭鏍广€?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

/**
 * OpenCL 绮鹃噰鏍蜂竴娆＄紪璇戝叡浜殑 density 杈撳嚭銆? */
public enum DensityGraphRoot {
    FINAL_DENSITY,
    INITIAL_DENSITY_WITHOUT_JAGGEDNESS,
    BARRIER,
    FLUID_FLOODEDNESS,
    FLUID_SPREAD,
    LAVA,
    EROSION,
    DEPTH
}

