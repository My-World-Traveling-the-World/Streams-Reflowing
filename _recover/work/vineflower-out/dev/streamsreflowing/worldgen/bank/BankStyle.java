package dev.streamsreflowing.worldgen.bank;

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

public record BankStyle() {
   private final List<ResourceLocation> biomes;
   private final List<TagKey<Biome>> tags;
   private final boolean enabled;
   private final List<BlockState> bed;
   private final List<BlockState> waterline;
   private final List<BlockState> bank;
   private final boolean bedEnabled;
   private final boolean waterlineEnabled;
   private final boolean bankEnabled;
   private final int waterlineBelow;
   private final int waterlineAbove;
   private final float underwaterNoise;
   private final float aboveWaterNoise;
   private final List<ResourceLocation> excludeBiomes;
   private final List<TagKey<Biome>> excludeTags;
   public static final ResourceKey<Registry<BankStyle>> REGISTRY_KEY = ResourceKey.createRegistryKey(
      ResourceLocation.fromNamespaceAndPath("streamsreflowing", "bank_style")
   );
   public static final int BED = 0;
   public static final int WATERLINE = 1;
   public static final int BANK = 2;
   public static final ResourceLocation CHAMELEON_ID = ResourceLocation.fromNamespaceAndPath("streamsreflowing", "chameleon");
   public static final BlockState CHAMELEON = Blocks.STRUCTURE_VOID.defaultBlockState();
   private static final Codec<BlockState> BLOCK = ResourceLocation.CODEC.comapFlatMap(var0 -> {
      if (CHAMELEON_ID.equals(var0)) {
         return DataResult.success(CHAMELEON);
      } else {
         Block var1 = BuiltInRegistries.BLOCK.getOptional(var0).orElse(null);
         return var1 == null ? DataResult.error(() -> "Unknown block: " + var0) : DataResult.success(var1.defaultBlockState());
      }
   }, var0 -> isChameleon(var0) ? CHAMELEON_ID : BuiltInRegistries.BLOCK.getKey(var0.getBlock()));
   private static final Codec<List<BlockState>> BLOCK_LIST = Codec.either(BLOCK, BLOCK.listOf())
      .xmap(
         var0 -> var0.map(var0x -> List.of(var0x), var0x -> var0x), var0 -> var0.size() == 1 ? Either.left(var0.get(0)) : Either.right((List<BlockState>)var0)
      );
   public static final Codec<BankStyle> CODEC = RecordCodecBuilder.create(
      var0 -> var0.group(
               ResourceLocation.CODEC.listOf().optionalFieldOf("biomes", List.of()).forGetter(BankStyle::biomes),
               TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("tags", List.of()).forGetter(BankStyle::tags),
               Codec.BOOL.optionalFieldOf("enabled", Boolean.valueOf(true)).forGetter(BankStyle::enabled),
               BLOCK_LIST.optionalFieldOf("bed", List.of(Blocks.GRAVEL.defaultBlockState())).forGetter(BankStyle::bed),
               BLOCK_LIST.optionalFieldOf("waterline", List.of(Blocks.COARSE_DIRT.defaultBlockState())).forGetter(BankStyle::waterline),
               BLOCK_LIST.optionalFieldOf("bank", List.of(CHAMELEON)).forGetter(BankStyle::bank),
               Codec.BOOL.optionalFieldOf("bed_enabled", Boolean.valueOf(true)).forGetter(BankStyle::bedEnabled),
               Codec.BOOL.optionalFieldOf("waterline_enabled", Boolean.valueOf(true)).forGetter(BankStyle::waterlineEnabled),
               Codec.BOOL.optionalFieldOf("bank_enabled", Boolean.valueOf(true)).forGetter(BankStyle::bankEnabled),
               Codec.INT.optionalFieldOf("waterline_below", Integer.valueOf(1)).forGetter(BankStyle::waterlineBelow),
               Codec.INT.optionalFieldOf("waterline_above", Integer.valueOf(1)).forGetter(BankStyle::waterlineAbove),
               Codec.floatRange(0.0F, 1.0F).optionalFieldOf("underwater_noise", 0.18F).forGetter(BankStyle::underwaterNoise),
               Codec.floatRange(0.0F, 1.0F).optionalFieldOf("above_water_noise", 0.18F).forGetter(BankStyle::aboveWaterNoise),
               ResourceLocation.CODEC.listOf().optionalFieldOf("exclude_biomes", List.of()).forGetter(BankStyle::excludeBiomes),
               TagKey.codec(Registries.BIOME).listOf().optionalFieldOf("exclude_tags", List.of()).forGetter(BankStyle::excludeTags)
            )
            .apply(var0, BankStyle::new)
   );

   public BankStyle(
      List<ResourceLocation> var1,
      List<TagKey<Biome>> var2,
      boolean var3,
      List<BlockState> var4,
      List<BlockState> var5,
      List<BlockState> var6,
      boolean var7,
      boolean var8,
      boolean var9,
      int var10,
      int var11,
      float var12,
      float var13,
      List<ResourceLocation> var14,
      List<TagKey<Biome>> var15
   ) {
      this.biomes = var1;
      this.tags = var2;
      this.enabled = var3;
      this.bed = var4;
      this.waterline = var5;
      this.bank = var6;
      this.bedEnabled = var7;
      this.waterlineEnabled = var8;
      this.bankEnabled = var9;
      this.waterlineBelow = var10;
      this.waterlineAbove = var11;
      this.underwaterNoise = var12;
      this.aboveWaterNoise = var13;
      this.excludeBiomes = var14;
      this.excludeTags = var15;
   }

   public static boolean isChameleon(BlockState var0) {
      return var0 != null && var0.is(Blocks.STRUCTURE_VOID);
   }

   public boolean zoneEnabled(int var1) {
      return var1 == 0 ? this.bedEnabled : (var1 == 1 ? this.waterlineEnabled : this.bankEnabled);
   }

   public BlockState zoneBlock(int var1, double var2) {
      List var4 = var1 == 0 ? this.bed : (var1 == 1 ? this.waterline : this.bank);
      if (var4.isEmpty()) {
         return null;
      } else {
         int var5 = Math.min(var4.size() - 1, (int)(var2 * (double)var4.size()));
         return (BlockState)var4.get(var5);
      }
   }

   public boolean matchesExact(ResourceKey<Biome> var1) {
      return var1 != null && this.biomes.contains(var1.location());
   }

   public boolean matchesTags(Holder<Biome> var1) {
      if (this.tags.isEmpty()) {
         return false;
      } else {
         for (TagKey var3 : this.tags) {
            if (!var1.is(var3)) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean excluded(Holder<Biome> var1) {
      ResourceKey var2 = (ResourceKey)var1.unwrapKey().orElse(null);
      if (var2 != null && this.excludeBiomes.contains(var2.location())) {
         return true;
      } else {
         for (TagKey var4 : this.excludeTags) {
            if (var1.is(var4)) {
               return true;
            }
         }

         return false;
      }
   }
}
