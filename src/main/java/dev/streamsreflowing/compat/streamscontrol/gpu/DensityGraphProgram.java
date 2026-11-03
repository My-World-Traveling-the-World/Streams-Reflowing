/* 鏂囦欢鑱岃矗锛氫繚瀛樺彲涓婁紶鍒?OpenCL 鐨勭揣鍑戝鏍?density graph 绋嬪簭銆?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * OpenCL density graph 缂栬瘧浜х墿銆? */
public record DensityGraphProgram(
        int rootNode,
        Map<DensityGraphRoot, Integer> roots,
        List<Integer> interpolatedNodes,
        List<DensityGraphNode> nodes,
        List<Double> constants,
        OpenCLNoiseTables noiseTables,
        List<OpenCLSpline> splines
) {
    public DensityGraphProgram {
        EnumMap<DensityGraphRoot, Integer> copiedRoots = new EnumMap<>(DensityGraphRoot.class);
        if (roots != null) {
            copiedRoots.putAll(roots);
        }
        roots = Map.copyOf(copiedRoots);
        interpolatedNodes = List.copyOf(interpolatedNodes == null ? List.of() : interpolatedNodes);
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        constants = List.copyOf(constants == null ? List.of() : constants);
        noiseTables = noiseTables == null ? new OpenCLNoiseTables(List.of(), List.of(), List.of()) : noiseTables;
        splines = List.copyOf(splines == null ? List.of() : splines);
    }

    public DensityGraphProgram(int rootNode,
                               List<DensityGraphNode> nodes,
                               List<Double> constants,
                               OpenCLNoiseTables noiseTables,
                               List<OpenCLSpline> splines) {
        this(rootNode, Map.of(), List.of(), nodes, constants, noiseTables, splines);
    }

    public int root(DensityGraphRoot root) {
        Integer node = roots.get(root);
        if (node == null) {
            throw new IllegalArgumentException("Density root not compiled: " + root);
        }
        return node;
    }

    public boolean hasRoot(DensityGraphRoot root) {
        return roots.containsKey(root);
    }

    public boolean isEmpty() {
        return rootNode < 0 || nodes.isEmpty();
    }
}

