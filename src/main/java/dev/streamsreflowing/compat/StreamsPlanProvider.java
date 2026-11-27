package dev.streamsreflowing.compat;

import dev.streamsreflowing.api.RiverPlanApi;
import dev.streamsreflowing.compat.streamscontrol.StreamsPrewarmService;
import dev.streamsreflowing.core.RiverEngine;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import java.util.Arrays;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;

/** 只有提供方理解游戏世界和内部计划；对外不泄露ColumnPlan/Lake/PlateKey。 */
public final class StreamsPlanProvider implements RiverPlanApi.Provider {
    @Override public RiverPlanApi.Session open(Object world) {
        if (!(world instanceof WorldGenLevel level)) throw new IllegalArgumentException("需要服务端世界句柄");
        RiverEngine engine = StreamsWorldgen.engine(level);
        ServerLevel serverLevel = level.getLevel();
        return new WorldBoundRiverSession(new EngineSession(engine, serverLevel),
                () -> StreamsWorldgen.engineIfPresent(serverLevel) == engine);
    }

    @Override public RiverPlanApi.Startup startup(Object world) {
        if (!(world instanceof WorldGenLevel level)) throw new IllegalArgumentException("需要服务端世界句柄");
        return StreamsPrewarmService.startupStatus(level.getLevel());
    }

    private record EngineSession(RiverEngine engine, ServerLevel level) implements RiverPlanApi.Session {
        @Override public Object identity() { return engine; }
        @Override public RiverPlanApi.MacroWaterScope macroWaterScope() {
            int radius = StreamsPrewarmService.completedRadiusBlocks(level);
            if (radius == 0) return RiverPlanApi.MacroWaterScope.NONE;
            // 使用当前引擎自己的配置，并纳入正片河道的高度门；缓存容量、线程数不改变地理结果。
            String rules = engine.config() + ":streams=" + dev.streamsreflowing.platform.Services.PLATFORM.generateStreams()
                    + ":maxY=" + dev.streamsreflowing.compat.streamscontrol.StreamsPrewarmConfig.current().maxRiverPlanY();
            return new RiverPlanApi.MacroWaterScope(radius, rules);
        }
        @Override public RiverPlanApi.Column column(int x, int z) {
            var plan = engine.plan(x, z);
            return new RiverPlanApi.Column(plan.inChannel(), plan.inCorridor(), plan.waterSurfaceY(),
                    plan.bedY(), plan.distance(), plan.width());
        }
        @Override public RiverPlanApi.Lake lake(int x, int z) {
            var lake = engine.lakeAt(x, z);
            return lake == null ? new RiverPlanApi.Lake(false, Integer.MIN_VALUE, Integer.MIN_VALUE)
                    : new RiverPlanApi.Lake(true, lake.level(), lake.level() - Math.max(1, lake.digDepth()));
        }
        @Override public List<RiverPlanApi.Plate> platesNear(double x, double z, double radius) {
            return Arrays.stream(engine.platesNear(x, z, radius))
                    .map(key -> new RiverPlanApi.Plate(key.cx(), key.cz())).toList();
        }
        @Override public void prewarm(RiverPlanApi.Plate plate) {
            engine.networkFor(new PlateKey(plate.x(), plate.z()));
        }
        @Override public RiverPlanApi.Startup startup() { return StreamsPrewarmService.startupStatus(level); }
    }
}
