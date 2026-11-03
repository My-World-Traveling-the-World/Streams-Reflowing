/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={Biome.class})
public interface BiomeClimateAccessor {
    @Accessor(value="climateSettings")
    public Biome.ClimateSettings streamsreflowing$climate();
}

