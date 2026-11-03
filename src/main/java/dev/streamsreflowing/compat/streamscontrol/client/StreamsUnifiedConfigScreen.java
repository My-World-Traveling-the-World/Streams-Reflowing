package dev.streamsreflowing.compat.streamscontrol.client;

import dev.streamsreflowing.core.config.QualityPreset;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import dev.streamsreflowing.compat.streamscontrol.StreamsPrewarmConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Single config screen for the mod. It has two pages because the controls no longer fit
 * on one screen: at GUI scale 4 a 1080p display only offers ~270 logical pixels, and the
 * gameplay page alone already needs ~293. Paging keeps every page inside the original
 * layout budget instead of introducing a scroll viewport.
 *
 * <p>Page 1 exposes the knobs that previously existed only in
 * {@code config/plaincraft-streams-prewarm.properties}, plus the river height ceiling.
 */
public final class StreamsUnifiedConfigScreen extends Screen {
    private static final int PAGE_GAMEPLAY = 0;
    private static final int PAGE_PERFORMANCE = 1;
    /** Mirrors StreamsRiverHeightGateMixin's own bound; the properties reader clamps to the same range. */
    private static final int MAX_RIVER_PLAN_Y = 3200;

    private record FieldLabel(Component text, AbstractWidget widget) {}

    private final Screen parent;
    private boolean generateRivers;
    private QualityPreset qualityPreset;
    private boolean gpuTerrainSampling;
    private boolean runtimeEnabled;
    private boolean startupEnabled;
    private boolean boatsFloat;
    private int streamParticles;
    private boolean vanillaWhitewater;
    private boolean floatingDebris;
    private boolean loadingNotice;
    private StreamsPrewarmConfig prewarmConfig;
    private int runtimeStrideChunks;
    private int runtimeScanIntervalTicks;
    private int startupSamplerThreads;
    private int runtimeSamplerThreads;
    private int startupPlateWorkers;
    private int maxRiverPlanY;
    private EditBox startupRadius;
    private EditBox runtimeRadius;
    private EditBox runtimeStrideField;
    private EditBox scanIntervalField;
    private EditBox startupSamplersField;
    private EditBox runtimeSamplersField;
    private EditBox plateWorkersField;
    private EditBox maxRiverPlanYField;
    private final List<FieldLabel> fieldLabels = new ArrayList<>();
    private int page = PAGE_GAMEPLAY;

    public StreamsUnifiedConfigScreen(Screen parent) {
        super(Component.translatable("plaincraft.streams.title"));
        this.parent = parent;
        this.generateRivers = StreamsConfigFabric.generateStreams();
        this.qualityPreset = StreamsConfigFabric.qualityPreset();
        this.boatsFloat = StreamsConfigFabric.boatsFloatOnFlowingWater();
        this.streamParticles = StreamsConfigFabric.streamBubbles();
        this.vanillaWhitewater = StreamsConfigFabric.particlesOnVanillaWater();
        this.floatingDebris = StreamsConfigFabric.streamDebris();
        this.loadingNotice = StreamsConfigFabric.loadingNotice();
        this.prewarmConfig = StreamsPrewarmConfig.reload();
        this.gpuTerrainSampling = prewarmConfig.gpuTerrainSampling();
        this.runtimeEnabled = prewarmConfig.runtimeEnabled();
        this.startupEnabled = prewarmConfig.enabled();
        // rawInt, not the record: from() post-processes these (0 = auto resolves through
        // StreamsStartupHardwareProfile, stride is clamped against the runtime radius), so
        // the record no longer carries what the player actually wrote in the file.
        this.runtimeStrideChunks = StreamsPrewarmConfig.rawInt("runtimeStrideChunks", 128);
        this.runtimeScanIntervalTicks = StreamsPrewarmConfig.rawInt("runtimeScanIntervalTicks", 1200);
        this.startupSamplerThreads = StreamsPrewarmConfig.rawInt("startupSamplerThreads", 0);
        this.runtimeSamplerThreads = StreamsPrewarmConfig.rawInt("runtimeSamplerThreads", 2);
        this.startupPlateWorkers = StreamsPrewarmConfig.rawInt("startupPlateWorkers", 0);
        this.maxRiverPlanY = StreamsPrewarmConfig.rawInt("maxRiverPlanY", 140);
    }

    @Override
    protected void init() {
        fieldLabels.clear();
        startupRadius = null;
        runtimeRadius = null;
        runtimeStrideField = null;
        scanIntervalField = null;
        startupSamplersField = null;
        runtimeSamplersField = null;
        plateWorkersField = null;
        maxRiverPlanYField = null;

        boolean compact = this.height < 300;
        int buttonHeight = compact ? 18 : 20;
        int optionRowStep = compact ? 19 : 22;
        int fieldRowStep = compact ? 21 : 24;
        // Deliberately larger than a row step. The radius fields belong to a
        // different section than the toggles above them, and at the old 4-8px the
        // group read as one continuous block of controls.
        int sectionGap = compact ? 14 : 20;
        int controlWidth = Math.min(360, this.width - 40);
        int x = (this.width - controlWidth) / 2;
        int y = compact ? 24 : 28;

        // Row budget per page, so the "keep the done button on-screen" shift below stays
        // exact instead of guessing a single page's height.
        int plainRows = page == PAGE_GAMEPLAY ? 8 : 0;
        int fieldRows = page == PAGE_GAMEPLAY ? 3 : 6;
        int gaps = page == PAGE_GAMEPLAY ? sectionGap : 0;
        int estimatedBottom = y + optionRowStep * plainRows + gaps + fieldRowStep * fieldRows
                + buttonHeight * 2 + (compact ? 4 : sectionGap);
        int availableBottom = this.height - 2;
        if (estimatedBottom > availableBottom) {
            y = Math.max(6, y - (estimatedBottom - availableBottom));
        }

        if (page == PAGE_GAMEPLAY) {
            y = buildGameplayPage(x, y, controlWidth, buttonHeight, optionRowStep, fieldRowStep, sectionGap);
        } else {
            y = buildPerformancePage(x, y, controlWidth, buttonHeight, fieldRowStep);
        }

        int doneY = Math.max(y + buttonHeight + (compact ? 4 : sectionGap), this.height - buttonHeight - 2);
        int navWidth = Math.min(150, Math.max(90, controlWidth / 3));
        int doneWidth = Math.max(120, controlWidth - navWidth - 8);
        int left = (this.width - (navWidth + 8 + doneWidth)) / 2;
        Component navLabel = Component.translatable(page == PAGE_GAMEPLAY
                ? "plaincraft.streams.page.performance"
                : "plaincraft.streams.page.gameplay");
        addRenderableWidget(Button.builder(navLabel, button -> switchPage())
                .bounds(left, doneY, navWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(left + navWidth + 8, doneY, doneWidth, buttonHeight)
                .build());
    }

    private int buildGameplayPage(int x, int y, int controlWidth, int buttonHeight,
                                  int optionRowStep, int fieldRowStep, int sectionGap) {
        addRenderableWidget(CycleButton.onOffBuilder(generateRivers).create(
                x, y, controlWidth, buttonHeight,
                Component.translatable("plaincraft.streams.generate"),
                (button, value) -> generateRivers = value));
        y += optionRowStep;

        addRenderableWidget(CycleButton.<QualityPreset>builder(StreamsUnifiedConfigScreen::qualityName)
                .withValues(QualityPreset.values())
                .withInitialValue(qualityPreset)
                .create(x, y, controlWidth, buttonHeight,
                        Component.translatable("plaincraft.streams.quality"),
                        (button, value) -> qualityPreset = value));
        y += optionRowStep;

        CycleButton<Boolean> gpuButton = CycleButton.onOffBuilder(gpuTerrainSampling).create(
                x, y, controlWidth, buttonHeight,
                Component.translatable("plaincraft.streams.gpu_sampling"),
                (button, value) -> gpuTerrainSampling = value);
        gpuButton.setTooltip(Tooltip.create(Component.translatable("plaincraft.streams.gpu_sampling.tooltip")));
        addRenderableWidget(gpuButton);
        y += optionRowStep;

        addRenderableWidget(CycleButton.onOffBuilder(boatsFloat).create(
                x, y, controlWidth, buttonHeight,
                Component.translatable("plaincraft.streams.boats"),
                (button, value) -> boatsFloat = value));
        y += optionRowStep;

        addRenderableWidget(CycleButton.<Integer>builder(StreamsUnifiedConfigScreen::particleName)
                .withValues(0, 1, 2)
                .withInitialValue(Math.max(0, Math.min(2, streamParticles)))
                .create(x, y, controlWidth, buttonHeight,
                        Component.translatable("plaincraft.streams.particles"),
                        (button, value) -> streamParticles = value));
        y += optionRowStep;

        addRenderableWidget(CycleButton.onOffBuilder(vanillaWhitewater).create(
                x, y, controlWidth, buttonHeight,
                Component.translatable("plaincraft.streams.whitewater"),
                (button, value) -> vanillaWhitewater = value));
        y += optionRowStep;

        addRenderableWidget(CycleButton.onOffBuilder(floatingDebris).create(
                x, y, controlWidth, buttonHeight,
                Component.translatable("plaincraft.streams.debris"),
                (button, value) -> floatingDebris = value));
        y += optionRowStep;

        addRenderableWidget(CycleButton.onOffBuilder(loadingNotice).create(
                x, y, controlWidth, buttonHeight,
                Component.translatable("plaincraft.streams.loading_notice"),
                (button, value) -> loadingNotice = value));
        y += optionRowStep + sectionGap;

        // Opens the prewarm section rather than closing it. This switch governs both
        // radius fields below it - it is the master control for in-play planning, and
        // a radius with planning switched off does nothing - so it reads as the
        // section's heading instead of a trailing footnote after the numbers.
        CycleButton<Boolean> runtimeButton = CycleButton.onOffBuilder(runtimeEnabled).create(
                x, y, controlWidth, buttonHeight,
                Component.translatable("plaincraft.streams.runtime_enabled"),
                (button, value) -> runtimeEnabled = value);
        runtimeButton.setTooltip(Tooltip.create(Component.translatable("plaincraft.streams.runtime_enabled.tooltip")));
        addRenderableWidget(runtimeButton);
        y += fieldRowStep;

        int fieldWidth = Math.min(110, Math.max(70, controlWidth / 3));
        startupRadius = numberField(x, y, controlWidth, fieldWidth, buttonHeight,
                "plaincraft.streams.startup_radius", prewarmConfig.startupRadiusChunks(),
                StreamsPrewarmConfig.MAX_PREWARM_RADIUS_CHUNKS);
        y += fieldRowStep;

        runtimeRadius = numberField(x, y, controlWidth, fieldWidth, buttonHeight,
                "plaincraft.streams.runtime_radius", prewarmConfig.runtimeRadiusChunks(),
                StreamsPrewarmConfig.MAX_PREWARM_RADIUS_CHUNKS);
        y += fieldRowStep;

        return y;
    }

    private int buildPerformancePage(int x, int y, int controlWidth, int buttonHeight, int fieldRowStep) {
        int fieldWidth = Math.min(110, Math.max(70, controlWidth / 3));
        int maxThreads = Math.max(1, Runtime.getRuntime().availableProcessors());

        CycleButton<Boolean> startupButton = CycleButton.onOffBuilder(startupEnabled).create(
                x, y, controlWidth, buttonHeight,
                Component.translatable("plaincraft.streams.startup_enabled"),
                (button, value) -> startupEnabled = value);
        startupButton.setTooltip(Tooltip.create(Component.translatable("plaincraft.streams.startup_enabled.tooltip")));
        addRenderableWidget(startupButton);
        y += fieldRowStep;

        runtimeStrideField = numberField(x, y, controlWidth, fieldWidth, buttonHeight,
                "plaincraft.streams.runtime_stride", runtimeStrideChunks,
                StreamsPrewarmConfig.MAX_PREWARM_RADIUS_CHUNKS);
        y += fieldRowStep;

        scanIntervalField = numberField(x, y, controlWidth, fieldWidth, buttonHeight,
                "plaincraft.streams.scan_interval", runtimeScanIntervalTicks, 4);
        y += fieldRowStep;

        startupSamplersField = numberField(x, y, controlWidth, fieldWidth, buttonHeight,
                "plaincraft.streams.startup_samplers", startupSamplerThreads, maxThreads);
        y += fieldRowStep;

        runtimeSamplersField = numberField(x, y, controlWidth, fieldWidth, buttonHeight,
                "plaincraft.streams.runtime_samplers", runtimeSamplerThreads, maxThreads);
        y += fieldRowStep;

        plateWorkersField = numberField(x, y, controlWidth, fieldWidth, buttonHeight,
                "plaincraft.streams.plate_workers", startupPlateWorkers, maxThreads);
        y += fieldRowStep;

        maxRiverPlanYField = numberField(x, y, controlWidth, fieldWidth, buttonHeight,
                "plaincraft.streams.max_river_plan_y", maxRiverPlanY,
                Integer.toString(MAX_RIVER_PLAN_Y).length());
        y += fieldRowStep;

        return y;
    }

    private EditBox numberField(int x, int y, int controlWidth, int fieldWidth, int buttonHeight,
                                String key, int value, int maxValue) {
        EditBox field = new EditBox(this.font, x + controlWidth - fieldWidth, y, fieldWidth, buttonHeight,
                Component.translatable(key));
        field.setEditable(true);
        field.setValue(Integer.toString(value));
        field.setFilter(StreamsUnifiedConfigScreen::isUnsignedInteger);
        // Length of the largest accepted number, so a 4-digit scan interval (max 1200) is
        // not silently truncated to two characters.
        field.setMaxLength(Integer.toString(Math.max(1, maxValue)).length());
        field.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
        addRenderableWidget(field);
        fieldLabels.add(new FieldLabel(Component.translatable(key), field));
        return field;
    }

    private void switchPage() {
        readFieldsBack();
        page = page == PAGE_GAMEPLAY ? PAGE_PERFORMANCE : PAGE_GAMEPLAY;
        this.rebuildWidgets();
    }

    /**
     * Copies the text boxes into the screen fields. Widgets are destroyed on every
     * {@code init()}, so a half-typed value (or a page switch) would otherwise silently
     * revert to whatever the fields held when the screen opened.
     */
    private void readFieldsBack() {
        runtimeStrideChunks = parseOr(runtimeStrideField, runtimeStrideChunks);
        runtimeScanIntervalTicks = parseOr(scanIntervalField, runtimeScanIntervalTicks);
        startupSamplerThreads = parseOr(startupSamplersField, startupSamplerThreads);
        runtimeSamplerThreads = parseOr(runtimeSamplersField, runtimeSamplerThreads);
        startupPlateWorkers = parseOr(plateWorkersField, startupPlateWorkers);
        maxRiverPlanY = parseOr(maxRiverPlanYField, maxRiverPlanY);
    }

    private static int parseOr(EditBox field, int fallback) {
        if (field == null) return fallback;
        String value = field.getValue();
        if (value == null || value.isEmpty()) return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xffffff);
        graphics.drawCenteredString(this.font,
                Component.translatable(page == PAGE_GAMEPLAY
                        ? "plaincraft.streams.page.gameplay.title"
                        : "plaincraft.streams.page.performance.title"),
                this.width / 2, this.height - this.font.lineHeight - 2, 0xa0a0a0);
        int controlWidth = Math.min(360, this.width - 40);
        int x = (this.width - controlWidth) / 2;
        for (FieldLabel label : fieldLabels) {
            drawFieldLabel(graphics, label.text(), x, label.widget());
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private void saveAndClose() {
        readFieldsBack();
        Map<String, String> common = new HashMap<>();
        Map<String, String> client = new HashMap<>();
        common.put("generateStreams", Boolean.toString(generateRivers));
        // Streams 2.12.x reads this canonical key. The old screen wrote the
        // legacy qualityPreset key, which was ignored after reload.
        common.put("terrainAccuracyLevel", qualityPreset.name());
        common.put("boatsFloatOnFlowingWater", Boolean.toString(boatsFloat));
        client.put("streamBubbles", Integer.toString(streamParticles));
        client.put("particlesOnVanillaWater", Boolean.toString(vanillaWhitewater));
        client.put("streamDebris", Boolean.toString(floatingDebris));
        client.put("loadingNotice", Boolean.toString(loadingNotice));
        StreamsConfigFabric.setValues(common, client);

        int startup = parseRadius(startupRadius, prewarmConfig.startupRadiusChunks());
        int runtime = parseRadius(runtimeRadius, prewarmConfig.runtimeRadiusChunks());
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());

        // Clamps mirror StreamsPrewarmConfig.from(), which would otherwise silently
        // rewrite out-of-range input on the next reload - the screen must not offer a
        // value the loader will reject.
        int stride = Math.max(1, Math.min(StreamsPrewarmConfig.MAX_PREWARM_RADIUS_CHUNKS, runtimeStrideChunks));
        if (runtime > 0) stride = Math.min(stride, runtime);
        int scanInterval = Math.max(20, Math.min(1200, runtimeScanIntervalTicks));
        int startupThreads = Math.max(0, Math.min(processors, startupSamplerThreads));
        int runtimeThreads = Math.max(1, Math.min(Math.min(8, processors), runtimeSamplerThreads));
        int plateWorkers = Math.max(0, Math.min(Math.min(12, processors), startupPlateWorkers));
        int planY = Math.max(0, Math.min(MAX_RIVER_PLAN_Y, maxRiverPlanY));

        Map<String, String> prewarm = new LinkedHashMap<>();
        prewarm.put("enabled", Boolean.toString(startupEnabled));
        prewarm.put("runtimeStrideChunks", Integer.toString(stride));
        prewarm.put("runtimeScanIntervalTicks", Integer.toString(scanInterval));
        prewarm.put("startupSamplerThreads", Integer.toString(startupThreads));
        prewarm.put("runtimeSamplerThreads", Integer.toString(runtimeThreads));
        prewarm.put("startupPlateWorkers", Integer.toString(plateWorkers));
        prewarm.put("maxRiverPlanY", Integer.toString(planY));

        prewarmConfig = StreamsPrewarmConfig.saveUiSettings(startup, runtime,
                gpuTerrainSampling, runtimeEnabled, prewarm);
        onClose();
    }

    private static int parseRadius(EditBox field, int fallback) {
        if (field == null) return fallback;
        try {
            return Math.max(0, Math.min(StreamsPrewarmConfig.MAX_PREWARM_RADIUS_CHUNKS,
                    Integer.parseInt(field.getValue())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void drawFieldLabel(GuiGraphics graphics, Component label, int x, AbstractWidget field) {
        int y = field.getY() + (field.getHeight() - this.font.lineHeight) / 2;
        graphics.drawString(this.font, label, x, y, 0xffffff, false);
    }

    private static boolean isUnsignedInteger(String value) {
        if (value == null || value.isEmpty()) return true;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private static Component qualityName(QualityPreset preset) {
        return Component.translatable("plaincraft.streams.quality."
                + preset.name().toLowerCase(Locale.ROOT));
    }

    private static Component particleName(Integer mode) {
        String suffix = mode == 0 ? "off" : mode == 1 ? "static" : "flowing";
        return Component.translatable("plaincraft.streams.particles." + suffix);
    }
}
