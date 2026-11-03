/* 鏂囦欢鑱岃矗锛氬畾涔?density graph 鐨勭揣鍑戣妭鐐规暟鎹竷灞€銆?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

/**
 * Density graph 绱у噾鑺傜偣銆? */
public record DensityGraphNode(
        DensityGraphNodeType type,
        int left,
        int right,
        int extraA,
        int extraB,
        double valueA,
        double valueB,
        double valueC,
        double valueD
) {}

