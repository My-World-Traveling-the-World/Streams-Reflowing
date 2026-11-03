/* 鏂囦欢鑱岃矗锛氬畾涔夌簿纭珮搴︽壒閲忛噰鏍风殑瀹炴椂杩涘害鍥炶皟杈圭晫銆?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

/**
 * 鍖哄煙绾х簿纭噰鏍疯繘搴︽帴鏀剁銆? */
@FunctionalInterface
public interface AccurateSamplingProgress {
    AccurateSamplingProgress NONE = batch -> {};

    void onBatch(Batch batch);

    record Batch(long completedColumns,
                 long totalColumns,
                 int batchColumns,
                 long elapsedNanos,
                 long batchNanos,
                 long kernelNanos) {}
}

