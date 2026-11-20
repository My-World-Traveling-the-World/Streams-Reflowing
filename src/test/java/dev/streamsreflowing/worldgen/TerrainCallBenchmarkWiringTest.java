package dev.streamsreflowing.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** 读取真实世界入口字节码，避免只测新组件而旧恒零诊断仍在生产旁路运行。 */
class TerrainCallBenchmarkWiringTest {
    @Test void worldEntryDelegatesToMeasuredStagesAndHasNoSelfSubtractedLongTimers() throws Exception {
        var node = new ClassNode();
        try (var stream = getClass().getClassLoader().getResourceAsStream(
                "dev/streamsreflowing/worldgen/StreamsWorldgen.class")) {
            assertNotNull(stream);
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        int selfSubtracts = 0, calls = 0;
        for (var method : node.methods) {
            var real = new java.util.ArrayList<org.objectweb.asm.tree.AbstractInsnNode>();
            for (var instruction : method.instructions) {
                if (instruction.getOpcode() >= 0) real.add(instruction);
                if (instruction instanceof MethodInsnNode call
                        && call.owner.equals("dev/streamsreflowing/worldgen/TerrainCallBenchmark")
                        && call.name.equals("run")) calls++;
            }
            for (int i = 2; i < real.size(); i++) {
                if (real.get(i).getOpcode() == Opcodes.LSUB
                        && real.get(i - 2) instanceof VarInsnNode a && a.getOpcode() == Opcodes.LLOAD
                        && real.get(i - 1) instanceof VarInsnNode b && b.getOpcode() == Opcodes.LLOAD
                        && a.var == b.var) selfSubtracts++;
            }
        }
        assertEquals(0, selfSubtracts, "同一long局部变量相减恒为零，不能作为实测耗时");
        assertEquals(1, calls, "真实世界入口必须调用阶段诊断，不能只测未接线的组件");
    }
}
