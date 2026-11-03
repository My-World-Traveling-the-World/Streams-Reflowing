package dev.streamsreflowing.compat;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.platform.Services;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SableFlowCore {
   private static final ConcurrentHashMap<Long, AtomicLong> lastNudgeTick = new ConcurrentHashMap<>();
   private static long lastLog = -1L;
   private static final boolean DIAG = Boolean.getBoolean("streamsreflowing.sablediag");

   private static double targetSpeed() {
      return Services.PLATFORM.vesselDriftSpeed() / 20.0;
   }

   private static double driftAccel() {
      return targetSpeed() / 8.0;
   }

   private SableFlowCore() {
   }

   public static void onPrePhysics(SubLevelPhysicsSystem system) {
      if (!(targetSpeed() <= 0.0)) {
         ServerLevel level = system.getLevel();
         ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
         if (container != null) {
            long gt = level.getGameTime();
            long levelKey = (long)level.dimension().location().hashCode() << 32;
            List<ServerSubLevel> bodies = container.getAllSubLevels();
            boolean log = DIAG && gt != lastLog && !bodies.isEmpty();
            MutableBlockPos p = new MutableBlockPos();

            for (ServerSubLevel body : bodies) {
               AtomicLong slot = lastNudgeTick.computeIfAbsent(levelKey ^ (long)body.getRuntimeId() & 4294967295L, k -> new AtomicLong(Long.MIN_VALUE));
               long prev = slot.get();
               if (prev != gt && slot.compareAndSet(prev, gt)) {
                  BoundingBox3i box = body.lastNetworkedBoundingBox();
                  if (box != null) {
                     Pose3d pose = body.logicalPose();
                     double wMinX = Double.MAX_VALUE;
                     double wMinY = Double.MAX_VALUE;
                     double wMinZ = Double.MAX_VALUE;
                     double wMaxX = -Double.MAX_VALUE;
                     double wMaxY = -Double.MAX_VALUE;
                     double wMaxZ = -Double.MAX_VALUE;

                     for (int ci = 0; ci < 8; ci++) {
                        Vec3 w = pose.transformPosition(
                           new Vec3(
                              (ci & 1) == 0 ? (double)box.minX() : (double)(box.maxX() + 1),
                              (ci & 2) == 0 ? (double)box.minY() : (double)(box.maxY() + 1),
                              (ci & 4) == 0 ? (double)box.minZ() : (double)(box.maxZ() + 1)
                           )
                        );
                        wMinX = Math.min(wMinX, w.x);
                        wMinY = Math.min(wMinY, w.y);
                        wMinZ = Math.min(wMinZ, w.z);
                        wMaxX = Math.max(wMaxX, w.x);
                        wMaxY = Math.max(wMaxY, w.y);
                        wMaxZ = Math.max(wMaxZ, w.z);
                     }

                     Vector3dc anchor = pose.position();
                     double halfDiag = 0.5 * Math.sqrt((wMaxX - wMinX) * (wMaxX - wMinX) + (wMaxZ - wMinZ) * (wMaxZ - wMinZ));
                     double cxBox = (wMinX + wMaxX) * 0.5;
                     double czBox = (wMinZ + wMaxZ) * 0.5;
                     if (Math.abs(cxBox - anchor.x()) <= halfDiag + 32.0 && Math.abs(czBox - anchor.z()) <= halfDiag + 32.0) {
                        int x0 = (int)Math.floor(wMinX - 0.5);
                        int x1 = (int)Math.floor(wMaxX + 0.5);
                        int z0 = (int)Math.floor(wMinZ - 0.5);
                        int z1 = (int)Math.floor(wMaxZ + 0.5);
                        int yTop = (int)Math.floor(wMaxY);
                        int yLow = (int)Math.floor(wMinY - 0.5);
                        int stride = Math.max(1, Math.max(x1 - x0, z1 - z0) / 32);
                        double fx = 0.0;
                        double fz = 0.0;
                        int wet = 0;

                        for (int x = x0; x <= x1; x += stride) {
                           for (int z = z0; z <= z1; z += stride) {
                              if (level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
                                 for (int y = yTop; y >= yLow; y--) {
                                    FluidState fs = level.getFluidState(p.set(x, y, z));
                                    if (!fs.isEmpty() && fs.is(FluidTags.WATER)) {
                                       if (!((double)((float)y + fs.getHeight(level, p)) >= wMinY - 0.1)) {
                                          break;
                                       }

                                       Vec3 flow = fs.getFlow(level, p);
                                       if (flow.x != 0.0 || flow.z != 0.0) {
                                          fx += flow.x;
                                          fz += flow.z;
                                          wet++;
                                       }
                                       break;
                                    }

                                    if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                                       break;
                                    }
                                 }
                              }
                           }
                        }

                        if (wet != 0) {
                           double len = Math.sqrt(fx * fx + fz * fz);
                           if (!(len < 1.0E-4)) {
                              double nx = fx / len;
                              double nz = fz / len;
                              RigidBodyHandle handle = RigidBodyHandle.of(body);
                              Vector3dc v = handle.getLinearVelocity();
                              double target = targetSpeed();
                              double along = v.x() * nx + v.z() * nz;
                              if (!(along >= target)) {
                                 double add = Math.min(driftAccel(), target - along);
                                 system.wakeUpObjectsAt((int)Math.floor(anchor.x()), (int)Math.floor(anchor.y()), (int)Math.floor(anchor.z()));
                                 handle.addLinearAndAngularVelocity(new Vector3d(nx * add, 0.0, nz * add), new Vector3d());
                                 if (log) {
                                    lastLog = gt;
                                    Constants.LOG
                                       .info(
                                          "[SR-SABLE] nudged body {} at ({},{},{}) wet={} along={} +{} -> flow({},{})",
                                          body.getRuntimeId(),
                                          String.format("%.1f", anchor.x()),
                                          String.format("%.1f", anchor.y()),
                                          String.format("%.1f", anchor.z()),
                                          wet,
                                          String.format("%.3f", along),
                                          String.format("%.3f", add),
                                          String.format("%.2f", nx),
                                          String.format("%.2f", nz)
                                       );
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
