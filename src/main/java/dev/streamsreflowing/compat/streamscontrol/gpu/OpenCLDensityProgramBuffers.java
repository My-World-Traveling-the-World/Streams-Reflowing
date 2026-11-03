/* 鏂囦欢鑱岃矗锛氭寔鏈?density graph 闈欐€佽澶囩紦鍐插尯骞舵寜 kernel 鍙傛暟椤哄簭鎻愪緵璁块棶銆?*/
package dev.streamsreflowing.compat.streamscontrol.gpu;

import dev.streamsreflowing.compat.streamscontrol.gpu.bridge.OpenCLBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * 鍗曚釜 density program 鍦ㄤ竴涓?OpenCL runtime 涓婄殑甯搁┗缂撳啿鍖恒€? */
final class OpenCLDensityProgramBuffers implements AutoCloseable {
    private final List<OpenCLBridge.DeviceBuffer> arguments;
    private final OpenCLBridge.DeviceBuffer interpolatedNodes;

    private OpenCLDensityProgramBuffers(List<OpenCLBridge.DeviceBuffer> arguments,
                                        OpenCLBridge.DeviceBuffer interpolatedNodes) {
        this.arguments = List.copyOf(arguments);
        this.interpolatedNodes = interpolatedNodes;
    }

    static OpenCLDensityProgramBuffers upload(OpenCLRuntime runtime, OpenCLDensityProgramPayload payload) {
        ArrayList<OpenCLBridge.DeviceBuffer> buffers = new ArrayList<>(14);
        OpenCLBridge.DeviceBuffer interpolatedNodes = null;
        try {
            buffers.add(runtime.upload(payload.nodeInts()));
            buffers.add(runtime.upload(payload.nodeValues()));
            buffers.add(runtime.upload(payload.normalInts()));
            buffers.add(runtime.upload(payload.normalValues()));
            buffers.add(runtime.upload(payload.perlinInts()));
            buffers.add(runtime.upload(payload.perlinValues()));
            buffers.add(runtime.upload(payload.amplitudes()));
            buffers.add(runtime.upload(payload.improvedIndices()));
            buffers.add(runtime.upload(payload.improvedValues()));
            buffers.add(runtime.upload(payload.permutations()));
            buffers.add(runtime.upload(payload.splineInts()));
            buffers.add(runtime.upload(payload.splineLocations()));
            buffers.add(runtime.upload(payload.splineValueNodes()));
            buffers.add(runtime.upload(payload.splineDerivatives()));
            interpolatedNodes = runtime.upload(payload.interpolatedNodes());
            return new OpenCLDensityProgramBuffers(buffers, interpolatedNodes);
        } catch (Throwable failure) {
            buffers.forEach(OpenCLBridge.DeviceBuffer::close);
            if (interpolatedNodes != null) {
                interpolatedNodes.close();
            }
            throw failure;
        }
    }

    List<OpenCLBridge.DeviceBuffer> arguments() {
        return arguments;
    }

    OpenCLBridge.DeviceBuffer interpolatedNodes() {
        return interpolatedNodes;
    }

    @Override
    public void close() {
        arguments.forEach(OpenCLBridge.DeviceBuffer::close);
        interpolatedNodes.close();
    }
}

