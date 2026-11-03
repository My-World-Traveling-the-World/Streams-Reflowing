package dev.streamsreflowing.compat.streamscontrol.gpu;

import java.util.List;

/**
 * OpenCL 鍣０琛ㄣ€? */
public record OpenCLNoiseTables(
        List<OpenCLNormalNoise> normalNoises,
        List<OpenCLPerlinNoise> perlinNoises,
        List<OpenCLImprovedNoise> improvedNoises
) {
    public OpenCLNoiseTables {
        normalNoises = List.copyOf(normalNoises == null ? List.of() : normalNoises);
        perlinNoises = List.copyOf(perlinNoises == null ? List.of() : perlinNoises);
        improvedNoises = List.copyOf(improvedNoises == null ? List.of() : improvedNoises);
    }

    public boolean isEmpty() {
        return normalNoises.isEmpty() && perlinNoises.isEmpty() && improvedNoises.isEmpty();
    }
}
