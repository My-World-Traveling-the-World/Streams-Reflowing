package dev.streamsreflowing.fabric.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class StreamsMixinPlugin implements IMixinConfigPlugin {
   @Override
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (mixinClassName.endsWith(".SableBeyondWaterMixin")) {
         try {
            return FabricLoader.getInstance().getModContainer("sable_beyond").map(c -> c.getMetadata().getVersion().getFriendlyString()).map(v -> {
               String core = v.split("[+-]")[0];
               String[] parts = core.split("\\.");
               int major = parts.length > 0 ? parseOr0(parts[0]) : 0;
               int minor = parts.length > 1 ? parseOr0(parts[1]) : 0;
               return major > 0 || minor >= 5;
            }).orElse(false);
         } catch (Throwable var4) {
            return false;
         }
      } else {
         return true;
      }
   }

   private static int parseOr0(String s) {
      try {
         return Integer.parseInt(s.trim());
      } catch (NumberFormatException var2) {
         return 0;
      }
   }

   @Override
   public void onLoad(String mixinPackage) {
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   @Override
   public List<String> getMixins() {
      return null;
   }

   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }
}
