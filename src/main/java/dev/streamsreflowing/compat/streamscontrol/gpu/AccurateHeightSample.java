/* 鏂囦欢鑱岃矗锛氫繚瀛樺崟涓笘鐣屽垪鐨勪笁绫荤簿纭珮搴﹀浘鍊笺€?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

/**
 * 鍗曚釜涓栫晫鍒楃殑绮剧‘楂樺害閲囨牱缁撴灉銆? */
public record AccurateHeightSample(int worldSurfaceWg,
                                   int oceanFloorWg,
                                   int motionBlockingNoLeaves) {
    public static long key(int blockX, int blockZ) {
        return ((long) blockX << 32) | (blockZ & 0xFFFFFFFFL);
    }
}

