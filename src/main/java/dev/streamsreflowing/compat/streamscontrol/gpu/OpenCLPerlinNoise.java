package dev.streamsreflowing.compat.streamscontrol.gpu;

import java.util.List;

/**
 * OpenCL 渚?PerlinNoise 鎻忚堪銆? */
public record OpenCLPerlinNoise(
        int firstOctave,
        double lowestFreqInputFactor,
        double lowestFreqValueFactor,
        double maxValue,
        List<Double> amplitudes,
        List<Integer> improvedNoiseIndices
) {
    public OpenCLPerlinNoise {
        amplitudes = List.copyOf(amplitudes == null ? List.of() : amplitudes);
        improvedNoiseIndices = List.copyOf(improvedNoiseIndices == null ? List.of() : improvedNoiseIndices);
    }
}
