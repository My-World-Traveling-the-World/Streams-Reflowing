/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.bank;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public record BankFeature(List<ResourceLocation> features, List<ResourceLocation> biomes, List<TagKey<Biome>> tags, List<ResourceLocation> excludeBiomes, List<TagKey<Biome>> excludeTags, int chance) {
    public static final ResourceKey<Registry<BankFeature>> REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("streamsreflowing", "bank_feature"));
    public static final Codec<BankFeature> CODEC = RecordCodecBuilder.create(instance -> instance.group(((MapCodec)ResourceLocation.CODEC.listOf().fieldOf("features")).forGetter(BankFeature::features), ResourceLocation.CODEC.listOf().optionalFieldOf("biomes", List.of()).forGetter(BankFeature::biomes), TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("tags", List.of()).forGetter(BankFeature::tags), ResourceLocation.CODEC.listOf().optionalFieldOf("exclude_biomes", List.of()).forGetter(BankFeature::excludeBiomes), TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("exclude_tags", List.of()).forGetter(BankFeature::excludeTags), Codec.intRange(0, 100).optionalFieldOf("chance", 100).forGetter(BankFeature::chance)).apply((Applicative<BankFeature, ?>)instance, BankFeature::new));

    public boolean matches(Holder<Biome> holder, ResourceKey<Biome> resourceKey) {
        if (resourceKey != null && this.excludeBiomes.contains(resourceKey.location())) {
            return false;
        }
        for (TagKey<Biome> tagKey : this.excludeTags) {
            if (!holder.is(tagKey)) continue;
            return false;
        }
        if (this.biomes.isEmpty() && this.tags.isEmpty()) {
            return true;
        }
        if (resourceKey != null && this.biomes.contains(resourceKey.location())) {
            return true;
        }
        if (this.tags.isEmpty()) {
            return false;
        }
        for (TagKey<Biome> tagKey : this.tags) {
            if (holder.is(tagKey)) continue;
            return false;
        }
        return true;
    }
}

