package dev.streamsreflowing.compat;

import dev.streamsreflowing.api.RiverPlanApi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 直接调用生产世界句柄守卫；模拟查询中卸载，不能靠成功返回空计划蒙混。 */
class WorldBoundRiverSessionTest {
    static class Delegate implements RiverPlanApi.Session {
        final List<RiverPlanApi.Plate> plates = new ArrayList<>(List.of(new RiverPlanApi.Plate(-1, 2)));
        int calls;
        Runnable duringRead = () -> {};
        public Object identity() { return this; }
        public RiverPlanApi.Column column(int x, int z) {
            calls++; duringRead.run(); return new RiverPlanApi.Column(true, true, 70, 65, 2, 5);
        }
        public RiverPlanApi.Lake lake(int x, int z) {
            calls++; return new RiverPlanApi.Lake(true, 71, 70);
        }
        public List<RiverPlanApi.Plate> platesNear(double x, double z, double r) { calls++; return plates; }
        public void prewarm(RiverPlanApi.Plate plate) { calls++; }
        public RiverPlanApi.Startup startup() { return new RiverPlanApi.Startup(7, RiverPlanApi.Phase.COMPLETE, ""); }
    }

    @Test void liveDataAndPlateSnapshotSurviveDelegateMutation() throws Exception {
        var delegate = new Delegate();
        var session = new WorldBoundRiverSession(delegate, () -> true);
        assertSame(delegate, session.identity());
        assertEquals(new RiverPlanApi.Column(true, true, 70, 65, 2, 5), session.column(1, 2));
        assertEquals(new RiverPlanApi.Lake(true, 71, 70), session.lake(1, 2));
        var snapshot = session.platesNear(1, 2, 64);
        delegate.plates.clear();
        assertEquals(List.of(new RiverPlanApi.Plate(-1, 2)), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        session.prewarm(snapshot.getFirst()); assertEquals(4, delegate.calls);
    }
    @Test void stoppedHandleRefusesAllWorkAndReportsStopped() {
        var delegate = new Delegate();
        var session = new WorldBoundRiverSession(delegate, () -> false);
        assertThrows(IllegalStateException.class, () -> session.column(1, 2));
        assertThrows(IllegalStateException.class, () -> session.lake(1, 2));
        assertThrows(IllegalStateException.class, () -> session.platesNear(1, 2, 64));
        assertThrows(IllegalStateException.class, () -> session.prewarm(new RiverPlanApi.Plate(1, 2)));
        assertEquals(0, delegate.calls);
        assertEquals(RiverPlanApi.Phase.STOPPED, session.startup().phase());
        assertEquals(7, session.startup().session());
    }
    @Test void unloadDuringQueryCannotReturnLatePlan() {
        var alive = new AtomicBoolean(true);
        var delegate = new Delegate();
        delegate.duringRead = () -> alive.set(false);
        var session = new WorldBoundRiverSession(delegate, alive::get);
        assertThrows(IllegalStateException.class, () -> session.column(1, 2));
        assertEquals(1, delegate.calls);
    }
}
