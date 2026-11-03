/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.mixin;

import dev.streamsreflowing.fabric.fluid.StreamFluids;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings"}, remap=false)
public class WorldRenderingSettingsMixin {
    @Inject(method={"setBlockStateIds"}, at={@At(value="HEAD")}, remap=false, require=0)
    private void streamsreflowing$mapStreamToWater(Object2IntMap<BlockState> ids, CallbackInfo ci) {
        if (ids == null) {
            return;
        }
        int waterId = ids.getInt(Blocks.WATER.defaultBlockState());
        if (waterId == ids.defaultReturnValue()) {
            return;
        }
        for (BlockState state : StreamFluids.STREAM_BLOCK.getStateDefinition().getPossibleStates()) {
            ids.put(state, waterId);
        }
    }
}

