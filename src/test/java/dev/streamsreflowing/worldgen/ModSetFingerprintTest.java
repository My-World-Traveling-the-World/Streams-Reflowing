package dev.streamsreflowing.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 模组集指纹的稳定性：与顺序无关；任何一条 id/版本变化都必须改变指纹。 */
class ModSetFingerprintTest {
    @Test
    void sameModSetIsOrderIndependent() {
        long a = ModSetFingerprint.of(List.of("minecraft@1.21.1", "streamsreflowing@2.12.8-fix6",
                "fabric-api@0.116.17+1.21.1"));
        long b = ModSetFingerprint.of(List.of("fabric-api@0.116.17+1.21.1",
                "streamsreflowing@2.12.8-fix6", "minecraft@1.21.1"));
        assertEquals(a, b);
    }

    @Test
    void anyVersionChangeChangesTheFingerprint() {
        long base = ModSetFingerprint.of(List.of("minecraft@1.21.1", "streamsreflowing@2.12.8-fix6"));
        long versionBump = ModSetFingerprint.of(List.of("minecraft@1.21.1",
                "streamsreflowing@2.12.8-fix7"));
        long addedMod = ModSetFingerprint.of(List.of("minecraft@1.21.1",
                "streamsreflowing@2.12.8-fix6", "roadweaver@2.3.1"));
        assertNotEquals(base, versionBump);
        assertNotEquals(base, addedMod);
    }

    @Test
    void emptyModSetIsStable() {
        assertEquals(ModSetFingerprint.of(List.of()), ModSetFingerprint.of(List.of()));
    }
}
