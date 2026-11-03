/* 鏂囦欢鑱岃矗锛氫繚瀛樿鍒欏尯鍩熺簿纭噰鏍疯繑鍥炵殑涓夊紶鍘熺増楂樺害鍥炬暟缁勩€?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import java.util.Objects;

/**
 * 涓庤姹傜綉鏍肩储寮曚竴涓€瀵瑰簲鐨勭簿纭珮搴︾粨鏋溿€? */
public record AccurateHeightGrid(AccurateHeightGridRequest request,
                                 int[] worldSurface,
                                 int[] oceanFloor,
                                 int[] motionBlocking) {
    public AccurateHeightGrid {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(worldSurface, "worldSurface");
        Objects.requireNonNull(oceanFloor, "oceanFloor");
        Objects.requireNonNull(motionBlocking, "motionBlocking");
        int expected = request.sampleCount();
        if (worldSurface.length != expected || oceanFloor.length != expected || motionBlocking.length != expected) {
            throw new IllegalArgumentException("accurate height grid arrays do not match request");
        }
    }
}

