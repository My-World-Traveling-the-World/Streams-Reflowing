package dev.streamsreflowing.compat;

import dev.streamsreflowing.api.RiverPlanApi;
import java.util.List;
import java.util.function.BooleanSupplier;

/** API会话跟随提供方世界身份，卸载后的句柄不能继续查询或返回迟到的计划。 */
final class WorldBoundRiverSession implements RiverPlanApi.Session {
    private final RiverPlanApi.Session delegate;
    private final BooleanSupplier alive;
    WorldBoundRiverSession(RiverPlanApi.Session delegate, BooleanSupplier alive) {
        this.delegate = delegate;
        this.alive = alive;
    }
    private void check() {
        if (!alive.getAsBoolean()) throw new IllegalStateException("河流计划会话所属世界已卸载或引擎已替换");
    }
    @Override public Object identity() { return delegate.identity(); }
    @Override public RiverPlanApi.Column column(int x, int z) throws Exception {
        check(); var result = delegate.column(x, z); check(); return result;
    }
    @Override public RiverPlanApi.Lake lake(int x, int z) throws Exception {
        check(); var result = delegate.lake(x, z); check(); return result;
    }
    @Override public List<RiverPlanApi.Plate> platesNear(double x, double z, double radius) throws Exception {
        check(); var result = List.copyOf(delegate.platesNear(x, z, radius)); check(); return result;
    }
    @Override public void prewarm(RiverPlanApi.Plate plate) throws Exception {
        // 不打断已经进入的共享构建，但卸载后的排队请求不能再进入引擎。
        check(); delegate.prewarm(plate); check();
    }
    @Override public RiverPlanApi.Startup startup() {
        var status = delegate.startup();
        return alive.getAsBoolean() ? status
                : new RiverPlanApi.Startup(status.session(), RiverPlanApi.Phase.STOPPED, "世界已卸载或引擎已替换");
    }
}
