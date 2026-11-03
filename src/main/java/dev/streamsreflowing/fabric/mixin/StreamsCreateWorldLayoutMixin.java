package dev.streamsreflowing.fabric.mixin;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreateWorldScreen.class, priority = 1)
public abstract class StreamsCreateWorldLayoutMixin {
    private static final int RIVER_WIDTH = 210;
    private static final int CONFIG_ROW_WIDTH = 310;
    private static final int BUTTON_HEIGHT = 20;
    private static final int HORIZONTAL_GAP = 4;
    private static final int VERTICAL_GAP = 8;
    private static final int SCREEN_MARGIN = 20;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void plaincraft$layoutStreamsAndExistingConfigButtons(GuiGraphics graphics, int mouseX,
                                                                   int mouseY, float partialTick,
                                                                   CallbackInfo ci) {
        CreateWorldScreen screen = (CreateWorldScreen) (Object) this;
        Map<Target, Button> buttons = new EnumMap<>(Target.class);
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button) {
                Target target = classify(button);
                if (target != null) {
                    buttons.putIfAbsent(target, button);
                }
            }
        }

        Button river = buttons.get(Target.RIVER);
        if (river == null) {
            return;
        }

        int riverY = river.getY();
        int configY = riverY + BUTTON_HEIGHT + VERTICAL_GAP;
        int bottomLimit = screen.height - 4;
        if (configY + BUTTON_HEIGHT > bottomLimit) {
            int shift = configY + BUTTON_HEIGHT - bottomLimit;
            riverY = Math.max(4, riverY - shift);
            configY = riverY + BUTTON_HEIGHT + VERTICAL_GAP;
        }

        int riverWidth = Math.min(RIVER_WIDTH, Math.max(120, screen.width - SCREEN_MARGIN * 2));
        river.setRectangle(riverWidth, BUTTON_HEIGHT, (screen.width - riverWidth) / 2, riverY);

        Target[] configTargets = {
                Target.AUTOTUNE,
                Target.WORLDGEN,
                Target.CHUNKY,
                Target.HUGE_TREES
        };
        int configCount = 0;
        for (Target target : configTargets) {
            if (buttons.containsKey(target)) {
                configCount++;
            }
        }
        if (configCount == 0) {
            return;
        }

        int rowWidth = Math.min(CONFIG_ROW_WIDTH, Math.max(120, screen.width - SCREEN_MARGIN * 2));
        int availableWidth = rowWidth - HORIZONTAL_GAP * (configCount - 1);
        int buttonWidth = availableWidth / configCount;
        int remainder = availableWidth % configCount;
        int x = (screen.width - rowWidth) / 2;
        int index = 0;
        for (Target target : configTargets) {
            Button button = buttons.get(target);
            if (button == null) {
                continue;
            }
            int width = buttonWidth + (index < remainder ? 1 : 0);
            button.setRectangle(width, BUTTON_HEIGHT, x, configY);
            x += width + HORIZONTAL_GAP;
            index++;
        }
    }

    private static Target classify(Button button) {
        String label = button.getMessage().getString();
        if (label == null || label.isBlank()) {
            return null;
        }
        String lower = label.toLowerCase(Locale.ROOT);
        if (label.contains("河流配置") || lower.contains("streams reflowing settings")) {
            return Target.RIVER;
        }
        if (label.contains("自动调优") || lower.contains("autotune")
                || lower.contains("auto tune") || lower.contains("plaincraft autotune")) {
            return Target.AUTOTUNE;
        }
        if (label.contains("世界生成") || lower.contains("worldgen")
                || lower.contains("world gen") || lower.contains("tectonic") || lower.contains("terralith")) {
            return Target.WORLDGEN;
        }
        if (label.contains("区块预生成") || label.contains("预生成配置")
                || lower.contains("chunky")) {
            return Target.CHUNKY;
        }
        if (label.contains("巨树") || lower.contains("huge tree")
                || lower.contains("tan's huge trees") || lower.contains("tanshugetrees")) {
            return Target.HUGE_TREES;
        }
        return null;
    }

    private enum Target {
        RIVER,
        AUTOTUNE,
        WORLDGEN,
        CHUNKY,
        HUGE_TREES
    }
}
