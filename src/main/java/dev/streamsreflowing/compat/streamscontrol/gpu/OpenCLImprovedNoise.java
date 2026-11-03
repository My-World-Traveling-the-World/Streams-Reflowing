package dev.streamsreflowing.compat.streamscontrol.gpu;

/**
 * OpenCL 渚?ImprovedNoise 鎻忚堪銆? */
public record OpenCLImprovedNoise(
        double xo,
        double yo,
        double zo,
        byte[] permutation
) {
    public OpenCLImprovedNoise {
        permutation = permutation == null ? new byte[0] : permutation.clone();
    }

    @Override
    public byte[] permutation() {
        return permutation.clone();
    }
}
