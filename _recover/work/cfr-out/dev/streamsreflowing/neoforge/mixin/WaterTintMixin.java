/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.neoforge.mixin;

import dev.streamsreflowing.client.StreamTint;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BiomeColors.class})
public abstract class WaterTintMixin {
    @Shadow
    @Final
    @Mutable
    public static ColorResolver field_5666;
    @Shadow
    @Final
    @Mutable
    public static ColorResolver field_5665;
    @Shadow
    @Final
    @Mutable
    public static ColorResolver field_5664;

    @Inject(method={"<clinit>"}, at={@At(value="TAIL")}, require=0)
    private static void streamsreflowing$wrapResolvers(CallbackInfo ci) {
        field_5666 = StreamTint.wrap(field_5666, StreamTint.Kind.WATER);
        field_5665 = StreamTint.wrap(field_5665, StreamTint.Kind.GRASS);
        field_5664 = StreamTint.wrap(field_5664, StreamTint.Kind.FOLIAGE);
    }
}

