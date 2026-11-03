/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import dev.streamsreflowing.platform.Services;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class StreamBiomeBlacklist {
    public static final TagKey<Biome> NO_STREAMS = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("streamsreflowing", "no_streams"));
    private static volatile String a;
    private static volatile Set<ResourceLocation> b;
    private static volatile Set<String> c;
    private static volatile List<TagKey<Biome>> d;

    private StreamBiomeBlacklist() {
    }

    public static boolean isBlocked(Holder<Biome> holder) {
        if (holder.is(NO_STREAMS)) {
            return true;
        }
        StreamBiomeBlacklist.a();
        List<TagKey<Biome>> list = d;
        for (int i = 0; i < list.size(); ++i) {
            if (!holder.is(list.get(i))) continue;
            return true;
        }
        Set<ResourceLocation> set = b;
        Set<String> set2 = c;
        if (set.isEmpty() && set2.isEmpty()) {
            return false;
        }
        ResourceKey resourceKey = holder.unwrapKey().orElse(null);
        return resourceKey != null && (set.contains(resourceKey.location()) || set2.contains(resourceKey.location().getNamespace()));
    }

    private static void a() {
        String string = Services.PLATFORM.streamBiomeBlacklist();
        if (string == null) {
            string = "";
        }
        if (string.equals(a)) {
            return;
        }
        HashSet<ResourceLocation> hashSet = new HashSet<ResourceLocation>();
        HashSet<String> hashSet2 = new HashSet<String>();
        ArrayList<TagKey<Biome>> arrayList = new ArrayList<TagKey<Biome>>();
        for (String string2 : string.split(",")) {
            ResourceLocation resourceLocation;
            String string3 = string2.trim();
            if (string3.isEmpty()) continue;
            if (string3.startsWith("#")) {
                resourceLocation = ResourceLocation.tryParse(string3.substring(1));
                if (resourceLocation == null) continue;
                arrayList.add(TagKey.create(Registries.BIOME, resourceLocation));
                continue;
            }
            if (string3.endsWith(":*")) {
                hashSet2.add(string3.substring(0, string3.length() - 2));
                continue;
            }
            if (string3.indexOf(58) < 0) {
                hashSet2.add(string3);
                continue;
            }
            resourceLocation = ResourceLocation.tryParse(string3);
            if (resourceLocation == null) continue;
            hashSet.add(resourceLocation);
        }
        b = hashSet;
        c = hashSet2;
        d = arrayList;
        a = string;
    }

    static {
        b = Set.of();
        c = Set.of();
        d = List.of();
    }
}

