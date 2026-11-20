package dev.streamsreflowing.api;

import java.net.URLClassLoader;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 提供方契约用真注册表；独立加载器覆盖未安装，避免测试顺序掩盖ABSENT。 */
class RiverPlanApiTest {
    @Test void absentProviderInIndependentLoaderIsExplicit() throws Exception {
        try (var loader = new URLClassLoader(new java.net.URL[] {
                RiverPlanApi.class.getProtectionDomain().getCodeSource().getLocation()
        }, ClassLoader.getPlatformClassLoader())) {
            Class<?> api = loader.loadClass(RiverPlanApi.class.getName());
            Object result = api.getMethod("open", Object.class).invoke(null, new Object());
            assertEquals("ABSENT", result.getClass().getMethod("availability").invoke(result).toString());
            assertNull(result.getClass().getMethod("session").invoke(result));
        }
    }

    @Test void resultCannotLieAboutAvailability() {
        assertThrows(IllegalArgumentException.class,
                () -> new RiverPlanApi.Open(RiverPlanApi.Availability.AVAILABLE, null, ""));
        assertThrows(NullPointerException.class, () -> new RiverPlanApi.Startup(1, null, ""));
    }

    @Test void realRegistryPreservesSessionDataAndExplainsProviderFailure() throws Exception {
        Object world = new Object();
        var session = new RiverPlanApi.Session() {
            @Override public Object identity() { return world; }
            @Override public RiverPlanApi.Column column(int x, int z) {
                return new RiverPlanApi.Column(true, true, 71, 66, x - z, 5);
            }
            @Override public RiverPlanApi.Lake lake(int x, int z) { return new RiverPlanApi.Lake(true, 80, 79); }
            @Override public List<RiverPlanApi.Plate> platesNear(double x, double z, double r) {
                return List.of(new RiverPlanApi.Plate(-3, 7));
            }
            @Override public void prewarm(RiverPlanApi.Plate plate) { assertEquals(new RiverPlanApi.Plate(-3, 7), plate); }
            @Override public RiverPlanApi.Startup startup() {
                return new RiverPlanApi.Startup(7, RiverPlanApi.Phase.BUILDING, "");
            }
        };
        var provider = new RiverPlanApi.Provider() {
            @Override public RiverPlanApi.Session open(Object handle) {
                if (handle != world) throw new IllegalStateException("不是当前世界");
                return session;
            }
            @Override public RiverPlanApi.Startup startup(Object handle) {
                if (handle != world) throw new IllegalStateException("状态查询故障");
                return session.startup();
            }
        };
        RiverPlanApi.register(provider);
        assertDoesNotThrow(() -> RiverPlanApi.register(provider));
        assertThrows(IllegalStateException.class, () -> RiverPlanApi.register(new RiverPlanApi.Provider() {
            public RiverPlanApi.Session open(Object w) { return null; }
            public RiverPlanApi.Startup startup(Object w) { return null; }
        }));
        var opened = RiverPlanApi.open(world);
        assertEquals(RiverPlanApi.Availability.AVAILABLE, opened.availability());
        assertSame(world, opened.session().identity());
        assertEquals(new RiverPlanApi.Column(true, true, 71, 66, 8, 5), opened.session().column(12, 4));
        assertEquals(new RiverPlanApi.Lake(true, 80, 79), opened.session().lake(1, 2));
        var plates = opened.session().platesNear(1, 2, 64);
        assertEquals(List.of(new RiverPlanApi.Plate(-3, 7)), plates);
        assertThrows(UnsupportedOperationException.class, () -> plates.clear());
        opened.session().prewarm(plates.getFirst());
        assertEquals(new RiverPlanApi.Startup(7, RiverPlanApi.Phase.BUILDING, ""), RiverPlanApi.startup(world));
        assertEquals(RiverPlanApi.Availability.FAILED, RiverPlanApi.open(null).availability());
        assertTrue(RiverPlanApi.open(null).reason().contains("不是当前世界"));
        assertEquals(RiverPlanApi.Phase.FAILED, RiverPlanApi.startup(null).phase());
        assertTrue(RiverPlanApi.startup(null).reason().contains("状态查询故障"));
    }
}
