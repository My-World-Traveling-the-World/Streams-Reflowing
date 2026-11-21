package dev.streamsreflowing.compat.streamscontrol;

/** 按逻辑屏幕约束完整卡片；先缩短装饰，再等比缩放，避免大GUI比例裁掉进度。 */
public record LoadingCardLayout(int width, int height, int bandHeight, float scale, float x, float y) {
    public static LoadingCardLayout fit(int screenWidth, int screenHeight, int minimumWidth, int detailLines) {
        int availableWidth = Math.max(1, screenWidth - 16);
        int availableHeight = Math.max(1, screenHeight - 16);
        int width = Math.max(minimumWidth, screenWidth * 9 / 10);
        // 文字区保持原字号，只有空间确实不足时才缩放整张卡片。
        int textHeight = 82 + Math.max(0, detailLines) * 12;
        int band = Math.max(32, Math.min(120, availableHeight - textHeight));
        int height = textHeight + band;
        float scale = Math.min(1F, Math.min(availableWidth / (float) width, availableHeight / (float) height));
        float x = (screenWidth - width * scale) / 2F;
        float y = Math.max(8F, screenHeight - height * scale - Math.max(40, screenHeight * 0.12F));
        return new LoadingCardLayout(width, height, band, scale, x, y);
    }
}
