package dev.streamsreflowing.fabric.mixin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** 直接解析真实class而不初始化客户端，开发命名空间也必须能绑定三个颜色解析器。 */
class WaterTintBindingTest {
    private static final String MIXIN = "dev/streamsreflowing/fabric/mixin/WaterTintMixin";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";

    @Test void eachShadowResolvesToActualNamedMinecraftField() throws IOException {
        ClassNode mixin = read(MIXIN), target = read("net/minecraft/client/renderer/BiomeColors");
        var shadows = mixin.fields.stream().filter(f -> has(f, SHADOW)).toList();
        assertEquals(3, shadows.size(), "不能因漏扫注解而空集合通过");
        for (FieldNode shadow : shadows) {
            assertTrue(target.fields.stream().anyMatch(f -> f.name.equals(shadow.name)
                    && f.desc.equals(shadow.desc) && (f.access & Opcodes.ACC_STATIC) != 0),
                    "真实开发目标不存在Shadow: " + shadow.name + shadow.desc);
            assertTrue(has(shadow, "Lorg/spongepowered/asm/mixin/Final;"));
            assertTrue(has(shadow, "Lorg/spongepowered/asm/mixin/Mutable;"));
        }
    }

    @Test void eachColorKindWrapsAndWritesBackItsOwnResolverExactlyOnce() throws IOException {
        ClassNode mixin = read(MIXIN);
        var method = mixin.methods.stream().filter(m -> m.name.equals("streamsreflowing$wrapResolvers"))
                .findFirst().orElseThrow();
        Map<String, String> expected = Map.of("WATER_COLOR_RESOLVER", "WATER",
                "GRASS_COLOR_RESOLVER", "GRASS", "FOLIAGE_COLOR_RESOLVER", "FOLIAGE");
        var observed = new ArrayList<String>();
        String field = null, kind = null;
        int calls = 0;
        for (var instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode f) {
                if (f.getOpcode() == Opcodes.GETSTATIC && f.owner.equals(MIXIN)) {
                    assertNull(field, "不能在上一种颜色写回前读取另一解析器");
                    field = f.name;
                } else if (f.owner.equals("dev/streamsreflowing/client/StreamTint$Kind")) {
                    kind = f.name;
                } else if (f.getOpcode() == Opcodes.PUTSTATIC && f.owner.equals(MIXIN)) {
                    assertEquals(field, f.name);
                    assertTrue(expected.containsKey(field), "错误的解析器字段: " + field);
                    assertEquals(expected.get(field), kind, "颜色类别不能串线");
                    observed.add(field);
                    field = kind = null;
                }
            } else if (instruction instanceof MethodInsnNode m
                    && m.owner.equals("dev/streamsreflowing/client/StreamTint") && m.name.equals("wrap")) {
                calls++;
            }
        }
        assertEquals(3, calls);
        assertEquals(List.of("WATER_COLOR_RESOLVER", "GRASS_COLOR_RESOLVER", "FOLIAGE_COLOR_RESOLVER"), observed);
        assertNull(field);
    }

    private static boolean has(FieldNode field, String descriptor) {
        return contains(field.visibleAnnotations, descriptor) || contains(field.invisibleAnnotations, descriptor);
    }

    private static boolean contains(List<AnnotationNode> annotations, String descriptor) {
        return annotations != null && annotations.stream().anyMatch(a -> a.desc.equals(descriptor));
    }

    private static ClassNode read(String binaryName) throws IOException {
        try (var stream = WaterTintBindingTest.class.getClassLoader().getResourceAsStream(binaryName + ".class")) {
            assertNotNull(stream, "必须读取真实类资源: " + binaryName);
            var node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
}
