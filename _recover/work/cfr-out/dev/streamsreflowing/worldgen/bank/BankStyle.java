/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.bank;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record BankStyle(List<ResourceLocation> biomes, List<TagKey<Biome>> tags, boolean enabled, List<BlockState> bed, List<BlockState> waterline, List<BlockState> bank, boolean bedEnabled, boolean waterlineEnabled, boolean bankEnabled, int waterlineBelow, int waterlineAbove, float underwaterNoise, float aboveWaterNoise, List<ResourceLocation> excludeBiomes, List<TagKey<Biome>> excludeTags) {
    public static final ResourceKey<Registry<BankStyle>> REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("streamsreflowing", "bank_style"));
    public static final int BED = 0;
    public static final int WATERLINE = 1;
    public static final int BANK = 2;
    public static final ResourceLocation CHAMELEON_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "chameleon");
    public static final BlockState CHAMELEON = Blocks.STRUCTURE_VOID.defaultBlockState();
    private static final Codec<BlockState> BLOCK = ResourceLocation.CODEC.comapFlatMap(resourceLocation -> {
        if (CHAMELEON_ID.equals(resourceLocation)) {
            return DataResult.success(CHAMELEON);
        }
        Block block = BuiltInRegistries.BLOCK.getOptional((ResourceLocation)resourceLocation).orElse(null);
        return block == null ? DataResult.error(() -> "Unknown block: " + String.valueOf(resourceLocation)) : DataResult.success(block.defaultBlockState());
    }, blockState -> BankStyle.isChameleon(blockState) ? CHAMELEON_ID : BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
    private static final Codec<List<BlockState>> BLOCK_LIST = Codec.either(BLOCK, BLOCK.listOf()).xmap(either -> either.map(blockState -> List.of(blockState), list -> list), list -> list.size() == 1 ? Either.left((BlockState)list.get(0)) : Either.right(list));
    public static final Codec<BankStyle> CODEC = RecordCodecBuilder.create(instance -> instance.group(ResourceLocation.CODEC.listOf().optionalFieldOf("biomes", List.of()).forGetter(BankStyle::biomes), TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("tags", List.of()).forGetter(BankStyle::tags), Codec.BOOL.optionalFieldOf("enabled", true).forGetter(BankStyle::enabled), BLOCK_LIST.optionalFieldOf("bed", List.of(Blocks.GRAVEL.defaultBlockState())).forGetter(BankStyle::bed), BLOCK_LIST.optionalFieldOf("waterline", List.of(Blocks.COARSE_DIRT.defaultBlockState())).forGetter(BankStyle::waterline), BLOCK_LIST.optionalFieldOf("bank", List.of(CHAMELEON)).forGetter(BankStyle::bank), Codec.BOOL.optionalFieldOf("bed_enabled", true).forGetter(BankStyle::bedEnabled), Codec.BOOL.optionalFieldOf("waterline_enabled", true).forGetter(BankStyle::waterlineEnabled), Codec.BOOL.optionalFieldOf("bank_enabled", true).forGetter(BankStyle::bankEnabled), Codec.INT.optionalFieldOf("waterline_below", 1).forGetter(BankStyle::waterlineBelow), Codec.INT.optionalFieldOf("waterline_above", 1).forGetter(BankStyle::waterlineAbove), Codec.floatRange(0.0f, 1.0f).optionalFieldOf("underwater_noise", Float.valueOf(0.18f)).forGetter(BankStyle::underwaterNoise), Codec.floatRange(0.0f, 1.0f).optionalFieldOf("above_water_noise", Float.valueOf(0.18f)).forGetter(BankStyle::aboveWaterNoise), ResourceLocation.CODEC.listOf().optionalFieldOf("exclude_biomes", List.of()).forGetter(BankStyle::excludeBiomes), TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("exclude_tags", List.of()).forGetter(BankStyle::excludeTags)).apply((Applicative<BankStyle, ?>)instance, BankStyle::new));

    public static boolean isChameleon(BlockState blockState) {
        return blockState != null && blockState.is(Blocks.STRUCTURE_VOID);
    }

    public boolean zoneEnabled(int n) {
        return n == 0 ? this.bedEnabled : (n == 1 ? this.waterlineEnabled : this.bankEnabled);
    }

    public BlockState zoneBlock(int n, double d) {
        List<BlockState> list;
        List<BlockState> list2 = n == 0 ? this.bed : (list = n == 1 ? this.waterline : this.bank);
        if (list.isEmpty()) {
            return null;
        }
        int n2 = Math.min(list.size() - 1, (int)(d * (double)list.size()));
        return list.get(n2);
    }

    public boolean matchesExact(ResourceKey<Biome> resourceKey) {
        return resourceKey != null && this.biomes.contains(resourceKey.location());
    }

    public boolean matchesTags(Holder<Biome> holder) {
        if (this.tags.isEmpty()) {
            return false;
        }
        for (TagKey<Biome> tagKey : this.tags) {
            if (holder.is(tagKey)) continue;
            return false;
        }
        return true;
    }

    public boolean excluded(Holder<Biome> holder) {
        ResourceKey resourceKey = holder.unwrapKey().orElse(null);
        if (resourceKey != null && this.excludeBiomes.contains(resourceKey.location())) {
            return true;
        }
        for (TagKey<Biome> tagKey : this.excludeTags) {
            if (!holder.is(tagKey)) continue;
            return true;
        }
        return false;
    }
}

