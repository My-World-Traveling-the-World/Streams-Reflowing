package dev.streamsreflowing.compat.streamscontrol;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 静态接线守卫只保证入口未旁路结果门，不冒充 Minecraft Mixin 运行时验证。 */
class RiverStartupWiringTest {
    private static final Path MAIN = Path.of("src/main/java/dev/streamsreflowing");

    @Test
    void bothLifecycleHooksUsePublisherGate() throws Exception {
        String source = Files.readString(MAIN.resolve("fabric/mixin/StreamsServerLifecycleMixin.java"));
        assertEquals(2, source.split(java.util.regex.Pattern.quote(
                "StreamsPrewarmService.publishStartupReadiness(level);"), -1).length - 1);
        assertFalse(source.contains("RiverReadinessBroadcast.publishReady("));
        assertTrue(source.contains("StreamsPrewarmService.stop((MinecraftServer) (Object) this);"));
        assertTrue(source.contains("RiverReadinessBroadcast.reset();"));
    }

    @Test
    void serviceRecordsAllFailurePathsAndStopsWorldGate() throws Exception {
        String source = Files.readString(MAIN.resolve("compat/streamscontrol/StreamsPrewarmService.java"));
        assertTrue(source.contains("if (!state.startup.begin()) return;"));
        assertTrue(source.contains("state.startup.disabled();"));
        assertTrue(source.contains("throw new IllegalStateException(\"Streams startup engine unavailable\");"));
        assertTrue(source.contains("state.startup.failed();"));
        assertTrue(source.contains("state.startup.completeWhen(combined.whenComplete("));
        assertTrue(source.contains("state.startup.publishIfReady(() -> {"));
        assertTrue(source.contains("state.startup.stop();"));
        assertFalse(source.contains("startupFuture"));
    }
}
