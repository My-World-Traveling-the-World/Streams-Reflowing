/* 鏂囦欢鑱岃矗锛氭弿杩颁竴娆¤鍒欏尯鍩熺簿纭珮搴﹂噰鏍风殑涓栫晫鍧愭爣缃戞牸銆?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

/**
 * 鎸夊浐瀹氭闀挎帓鍒楃殑鍖哄煙鍒楄姹傘€? */
public record AccurateHeightGridRequest(int minX, int minZ, int width, int height, int step) {
    public AccurateHeightGridRequest {
        if (width <= 0 || height <= 0 || step <= 0) {
            throw new IllegalArgumentException("invalid accurate height grid");
        }
        Math.multiplyExact(width, height);
    }

    public int sampleCount() {
        return Math.multiplyExact(width, height);
    }

    public int blockX(int index) {
        checkIndex(index);
        return Math.addExact(minX, Math.multiplyExact(index % width, step));
    }

    public int blockZ(int index) {
        checkIndex(index);
        return Math.addExact(minZ, Math.multiplyExact(index / width, step));
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= sampleCount()) {
            throw new IndexOutOfBoundsException(index);
        }
    }
}

