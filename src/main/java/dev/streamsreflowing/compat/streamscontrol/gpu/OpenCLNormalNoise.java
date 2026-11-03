package dev.streamsreflowing.compat.streamscontrol.gpu;

import java.util.List;

/**
 * OpenCL 渚?NormalNoise 鎻忚堪銆? */
public record OpenCLNormalNoise(
        int firstPerlinIndex,
        int secondPerlinIndex,
        double valueFactor,
        double maxValue
) {}
