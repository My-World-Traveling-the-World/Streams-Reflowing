package dev.streamsreflowing.api;

import java.util.List;
import java.util.Objects;

/**
 * 提供方拥有的只读契约，不引用Minecraft或内部引擎；named/intermediary共享同一签名。
 * world为调用方持有的世界句柄，只由提供方校验，契约层不解析游戏映射名。
 */
public final class RiverPlanApi {
    public static final int VERSION = 1;
    public enum Availability { AVAILABLE, ABSENT, FAILED }
    public enum Phase { NOT_STARTED, BUILDING, COMPLETE, DISABLED, FAILED, STOPPED }
    public record Column(boolean inChannel, boolean inCorridor, int waterSurfaceY, int bedY,
                         double distance, double width) {}
    public record Lake(boolean present, int surfaceY, int bedY) {}
    public record Plate(int x, int z) {}
    public record Startup(long session, Phase phase, String reason) {
        public Startup { Objects.requireNonNull(phase); Objects.requireNonNull(reason); }
    }
    public record Open(Availability availability, Session session, String reason) {
        public Open {
            Objects.requireNonNull(availability); Objects.requireNonNull(reason);
            if ((availability == Availability.AVAILABLE) != (session != null))
                throw new IllegalArgumentException("可用状态必须且只能携带会话");
        }
    }

    public interface Session {
        /** 供消费者去重；身份只在当前世界有效，不能序列化或跨世界复用。 */
        Object identity();
        Column column(int x, int z) throws Exception;
        Lake lake(int x, int z) throws Exception;
        List<Plate> platesNear(double x, double z, double radius) throws Exception;
        void prewarm(Plate plate) throws Exception;
        Startup startup();
    }
    public interface Provider {
        Session open(Object world);
        Startup startup(Object world);
    }

    private static volatile Provider provider;
    private RiverPlanApi() {}
    public static boolean isInstalled() { return provider != null; }

    /** 仅河流入口安装，消费者无需依赖道路后端或河流内部类型。 */
    public static synchronized void register(Provider implementation) {
        Objects.requireNonNull(implementation);
        if (provider != null && provider != implementation)
            throw new IllegalStateException("河流计划提供方已安装");
        provider = implementation;
    }

    public static Open open(Object world) {
        Provider current = provider;
        if (current == null) return new Open(Availability.ABSENT, null, "河流计划提供方未安装");
        try {
            Session session = Objects.requireNonNull(current.open(world), "河流提供方返回空会话");
            return new Open(Availability.AVAILABLE, session, "");
        } catch (RuntimeException | LinkageError failure) {
            return new Open(Availability.FAILED, null, describe(failure));
        }
    }

    /** 非阻塞状态查询；失败不伪造READY，也不隐式触发板块构建。 */
    public static Startup startup(Object world) {
        Provider current = provider;
        if (current == null) return new Startup(0, Phase.NOT_STARTED, "河流计划提供方未安装");
        try { return Objects.requireNonNull(current.startup(world)); }
        catch (RuntimeException | LinkageError failure) {
            return new Startup(0, Phase.FAILED, describe(failure));
        }
    }

    public static String describe(Throwable failure) {
        String text = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
        return text.substring(0, Math.min(512, text.length()));
    }
}
