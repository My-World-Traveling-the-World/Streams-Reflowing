/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import dev.streamsreflowing.flow.MobFlowOverrides;
import dev.streamsreflowing.worldgen.StreamsWorldgen;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={Entity.class})
public abstract class EntityFlowMixin {
    @ModifyReturnValue(method={"isPushedByFluid"}, at={@At(value="RETURN")}, require=0, expect=0)
    private boolean streamsreflowing$waterMobsIgnoreFlow(boolean original) {
        return original && !EntityFlowMixin.streamsreflowing$holdsInCurrent((Entity)((Object)this));
    }

    @ModifyExpressionValue(method={"updateFluidHeightAndDoFluidPushing"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;isPushedByFluid()Z")}, require=0, expect=0)
    private boolean streamsreflowing$waterMobsIgnoreFlowAtPushSite(boolean original) {
        return original && !EntityFlowMixin.streamsreflowing$holdsInCurrent((Entity)((Object)this));
    }

    @Unique
    private static boolean streamsreflowing$holdsInCurrent(Entity self) {
        LivingEntity living;
        Boolean say;
        if (MobFlowOverrides.any() && (say = MobFlowOverrides.verdict(String.valueOf(EntityType.getKey(self.getType())))) != null) {
            return say;
        }
        if (!StreamsConfigFabric.waterMobsIgnoreFlow()) {
            return false;
        }
        MobCategory cat = self.getType().getCategory();
        return cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT || cat == MobCategory.UNDERGROUND_WATER_CREATURE || cat == MobCategory.AXOLOTLS || self instanceof Drowned || self instanceof Guardian || self.getType().is(EntityTypeTags.AQUATIC) || self.getType().is(StreamsWorldgen.IGNORES_CURRENT) || self instanceof LivingEntity && (living = (LivingEntity)self).canBreatheUnderwater() && !EntityFlowMixin.streamsreflowing$isUndead(self);
    }

    @Unique
    private static boolean streamsreflowing$isUndead(Entity self) {
        return self.getType().is(EntityTypeTags.UNDEAD);
    }
}

