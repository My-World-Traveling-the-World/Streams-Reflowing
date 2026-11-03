/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen.bank;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.streamsreflowing.Constants;
import dev.streamsreflowing.platform.Services;
import dev.streamsreflowing.worldgen.bank.BankFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class BankFeatureLoader {
    private static volatile List<BankFeature> a;
    private static boolean b;
    private static final String c = "{\n  \"features\": [\"minecraft:patch_pumpkin\"],\n  \"tags\": [\"minecraft:is_overworld\"],\n  \"exclude_tags\": [\"minecraft:is_frozen_ocean\"],\n  \"exclude_biomes\": [\"minecraft:snowy_plains\", \"minecraft:snowy_taiga\", \"minecraft:ice_spikes\"],\n  \"chance\": 8\n}\n";
    private static final String d = "# Streams Reflowing -- custom bank features\n\nThis pass plants YOUR chosen features on the land around water. The mod ships it EMPTY: it is always on but\ndoes nothing until you add an entry here (or a datapack adds one). Every matching entry runs (there is no\noverride contest like bank styles have).\n\n> **Full guide** (every field, more examples, datapacks, and bank styles too): see `GUIDE.md` in this\n> folder. This file is the quick reference.\n\n## How to add one\n1. Look in `examples/example.json` for the shape. 2. Copy it UP into this folder (or write your own) and\nedit it. 3. Relaunch. (Files in `examples/` are reference-only and are NOT loaded.)\n\n## Where features place\nOn the bank around water, following your **bank placement** setting (`allWaterBanks`): along ALL water\n(rivers/lakes/oceans + our streams) when that is on, or only our own streams when it is set to streams-only.\nEach listed feature is run AT the bank column, on sturdy ground, exactly as written -- trees and big\nfeatures included (you chose them).\n\n## Fields\n- `features` : list of PLACED-feature ids to plant (e.g. `\"minecraft:patch_pumpkin\"`). Required.\n- `biomes` : exact biome ids to include.   `tags` : biome tags the biome must ALL have to include.\n  (If both are empty, the entry matches EVERYWHERE.)\n- `exclude_biomes` / `exclude_tags` : biomes/tags to drop AFTER the include -- so \"all overworld but not\n  frozen\" is `tags: [\"minecraft:is_overworld\"]` + `exclude_tags: [\"minecraft:is_frozen_ocean\", ...]`\n  (plus any snowy biomes/tags you want out).\n- `chance` : 0-100, the percent chance per matching near-water column. Default 100.\n\n## Shipping entries with a mod / datapack\nPut `<name>.json` files at `data/<namespace>/streamsreflowing/bank_feature/` in any datapack or mod jar --\nthey load automatically and are merged with this folder.\n";

    private BankFeatureLoader() {
    }

    public static void seed() {
        BankFeatureLoader.a(BankFeatureLoader.a());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static List<BankFeature> configEntries() {
        List<BankFeature> list = a;
        if (list != null) {
            return list;
        }
        Class<BankFeatureLoader> clazz = BankFeatureLoader.class;
        synchronized (BankFeatureLoader.class) {
            if (a != null) {
                // ** MonitorExit[var1_1] (shouldn't be in output)
                return a;
            }
            a = BankFeatureLoader.b();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return a;
        }
    }

    private static Path a() {
        return Services.PLATFORM.configDir().resolve("streamsreflowing").resolve("bank_features");
    }

    private static List<BankFeature> b() {
        Path path2 = BankFeatureLoader.a();
        BankFeatureLoader.a(path2);
        ArrayList<BankFeature> arrayList = new ArrayList<BankFeature>();
        try (Stream<Path> stream = Files.list(path2);){
            stream.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().forEach(path -> {
                try {
                    JsonElement jsonElement = JsonParser.parseString((String)Files.readString(path));
                    DataResult dataResult = BankFeature.CODEC.parse(JsonOps.INSTANCE, jsonElement);
                    if (dataResult.result().isPresent()) {
                        arrayList.add((BankFeature)dataResult.result().get());
                    } else {
                        Constants.LOG.warn("Custom bank feature {} failed to parse: {}", (Object)path.getFileName(), (Object)dataResult.error().orElse("unknown error"));
                    }
                }
                catch (Exception exception) {
                    Constants.LOG.warn("Failed to read custom bank feature {}", path, (Object)exception);
                }
            });
        }
        catch (IOException iOException) {
            Constants.LOG.warn("Failed to list custom bank features in {}", (Object)path2, (Object)iOException);
        }
        if (!arrayList.isEmpty()) {
            Constants.LOG.info("Loaded {} custom bank feature(s) from {}", (Object)arrayList.size(), (Object)path2);
        }
        return arrayList;
    }

    private static synchronized void a(Path path) {
        if (b) {
            return;
        }
        b = true;
        try {
            Files.createDirectories(path, new FileAttribute[0]);
            BankFeatureLoader.a(path.resolve("README.md"), d);
            BankFeatureLoader.a(path.resolve("GUIDE.md"), "# Streams Reflowing \u2014 Bank Styles and Bank Features Guide\n\nThis guide tells you how to change the land around your streams. It is written in\nsimplified technical English. The sentences are short. Each instruction is one step.\n\nStreams Reflowing has two systems for the banks:\n\n- **Bank style** \u2014 the blocks of a channel. It sets the block at the streambed, at the\n  waterline, and on the bank above the water.\n- **Bank feature** \u2014 the plants on a bank. It puts features (for example reeds, bushes, or\n  trees) on the land next to the water.\n\nYou edit both systems with JSON files. The JSON is the same for the config folder and for a\ndatapack.\n\n---\n\n## 1. Before you start\n\nObey these three rules for both systems.\n\n1. The mod reads the files one time, at game start.\n2. A change applies to new chunks only. The mod does not change land that exists already.\n3. After you edit a file, load the change again:\n   - Config folder: stop the game. Then start the game again.\n   - Datapack: run the `/reload` command, or make a new world.\n\n---\n\n## 2. Two ways to add a file\n\nYou can put a bank style or a bank feature in two places. Use the place that is correct for\nyour task.\n\n| Place | Folder | Use it for |\n|---|---|---|\n| Config folder | `config/streamsreflowing/` on one instance | A quick change on your own game. It wins over a datapack. |\n| Datapack | `data/<namespace>/streamsreflowing/` in a datapack or a mod | A change that you send with a modpack. |\n\n`<namespace>` is a name that you choose. Use only lowercase letters, numbers, and the\n`_` sign.\n\n---\n\n## 3. Bank styles\n\nA bank style skins the carved channel in three zones.\n\n```\n        bank         (the cut slope above the water)\n   ~~~~~~~~~~~~~~     (the waterline, a strip at the water surface)\n        bed          (the floor below the water)\n```\n\n### 3.1 Where the file goes\n\n- **Config folder:** `config/streamsreflowing/bank_styles/`\n  At the first game start, the mod makes this folder. It puts a `README.md` file and an\n  `examples/` folder in it. The `examples/` folder has a copy of each default style. The\n  top folder is empty at the start.\n\n  To change a style, do these steps:\n  1. Copy a file from the `examples/` folder to the top folder.\n  2. Edit the copy in the top folder.\n\n  The mod loads the top-level files only. It does not load the `examples/` folder.\n\n- **Datapack:** `data/<namespace>/streamsreflowing/bank_style/<name>.json`\n  To replace a default style, use the same file name as the default.\n\n### 3.2 The fields\n\nEach field is optional, but you must give at least one block or set `enabled` to `false`.\nGive each block as a block id. The mod uses the default state of the block.\n\n| Field | Type | Default | Function |\n|---|---|---|---|\n| `biomes` | list of ids | none | Use this style in these exact biomes. |\n| `tags` | list of tags | none | Use this style in a biome that has ALL of these tags. |\n| `exclude_biomes` | list of ids | none | Never use this style in these exact biomes. |\n| `exclude_tags` | list of tags | none | Never use this style in a biome that has ANY of these tags. |\n| `enabled` | true or false | true | Set `false` to leave the carved land raw here. |\n| `bed` | block or list | gravel | The block below the water. |\n| `waterline` | block or list | coarse_dirt | The block at the water surface. |\n| `bank` | block or list | grass_block | The block on the slope above the water. |\n| `bed_enabled` | true or false | true | Set `false` to keep the natural bed block. |\n| `waterline_enabled` | true or false | true | Set `false` to keep the natural waterline block. |\n| `bank_enabled` | true or false | true | Set `false` to keep the natural bank block. |\n| `waterline_below` | number | 1 | How many blocks the waterline goes down into the bed. |\n| `waterline_above` | number | 1 | How many blocks the waterline goes up into the bank. |\n| `underwater_noise` | 0.0 to 1.0 | 0.18 | How much the lower zones mix at the edge. 0 = a sharp line. |\n| `above_water_noise` | 0.0 to 1.0 | 0.18 | How much the upper zones mix at the edge. 0 = a sharp line. |\n\n### 3.3 More than one block in a zone\n\nYou can put a list of blocks in `bed`, `waterline`, or `bank`. The mod divides the zone into\nequal shares, one share for each block in the list.\n\n- Example: `[\"minecraft:gravel\", \"minecraft:gravel\", \"minecraft:clay\"]` gives two shares of\n  gravel and one share of clay.\n- To leave a share natural, put `minecraft:air` in the list. The mod does not change that\n  share. Example: `[\"minecraft:mud\", \"minecraft:air\", \"minecraft:air\"]` skins one third with\n  mud. It leaves two thirds natural.\n\n### 3.4 Which style the mod uses\n\nFor each column of a stream, the mod uses one style only. It selects the style by how\nspecific the style is. It does not use a priority number.\n\n1. An exact biome match (`biomes`) wins over all tag matches.\n2. A tag match (`tags`) is good only if the biome has EVERY tag in the list. If more than one\n   tag style matches, the style with the most tags wins.\n3. A style with no `biomes` and no `tags` is the default for all biomes.\n4. `exclude_biomes` and `exclude_tags` are a block list. The mod does not use a style for a\n   biome on its exclude list. The mod then uses the next style.\n5. A config-folder style wins over a datapack style at the same level.\n\nTo write \"all overworld biomes but not frozen biomes\", do this:\n\n```json\n{ \"tags\": [\"minecraft:is_overworld\"], \"exclude_tags\": [\"minecraft:is_frozen\"] }\n```\n\n### 3.5 Bank style examples\n\n**Sandy banks in a hot, dry biome:**\n```json\n{\n  \"tags\": [\"minecraft:is_badlands\", \"minecraft:is_hot\"],\n  \"bed\": \"minecraft:sand\",\n  \"waterline\": \"minecraft:sand\",\n  \"bank\": \"minecraft:sandstone\"\n}\n```\n\n**Stone mountain streams, but not when frozen. Skin the bed and the waterline. Keep the bank\nnatural:**\n```json\n{\n  \"tags\": [\"minecraft:is_mountain\"],\n  \"exclude_tags\": [\"minecraft:is_frozen\"],\n  \"bed\": \"minecraft:stone\",\n  \"waterline\": \"minecraft:gravel\",\n  \"bank_enabled\": false\n}\n```\n\n**Leave the swamp streams raw:**\n```json\n{ \"biomes\": [\"minecraft:swamp\"], \"enabled\": false }\n```\n\n---\n\n## 4. Bank features\n\nA bank feature puts plants on the land next to the water. The mod ships no bank features. The\nfeature pass does nothing until you add a file.\n\nThe mod runs EVERY bank feature that matches a column. There is no selection contest.\n\n### 4.1 Where the file goes\n\n- **Config folder:** `config/streamsreflowing/bank_features/`\n  At the first game start, the mod puts a `README.md` file and an `examples/example.json`\n  file in this folder. To add a feature, copy the example to the top folder. Then edit it.\n  The mod loads the top-level files only.\n\n- **Datapack:** `data/<namespace>/streamsreflowing/bank_feature/<name>.json`\n\n### 4.2 The fields\n\n| Field | Type | Default | Function |\n|---|---|---|---|\n| `features` | list of ids | none (required) | The placed features to put on the bank. |\n| `biomes` | list of ids | none | Run this feature in these exact biomes. |\n| `tags` | list of tags | none | Run this feature in a biome that has ALL of these tags. |\n| `exclude_biomes` | list of ids | none | Never run this feature in these exact biomes. |\n| `exclude_tags` | list of tags | none | Never run this feature in a biome that has ANY of these tags. |\n| `chance` | 0 to 100 | 100 | The chance, in percent, for each column next to water. |\n\nYou must give the `features` field. If you give no `biomes` and no `tags`, the feature runs\neverywhere.\n\n### 4.3 Which columns get the feature\n\nFor a column next to water, the mod runs a feature when both of these are true:\n\n1. The biome is not excluded. The biome is not in `exclude_biomes`. The biome has none of the\n   `exclude_tags`. An exclude always wins.\n2. The biome is included. One of these is true:\n   - `biomes` and `tags` are both empty, or\n   - the biome is in `biomes`, or\n   - the biome has every tag in `tags`.\n\nThen the mod uses `chance` for each column.\n\n**Warning:** The mod puts each feature on the bank as it is. The mod checks for solid ground\nbelow only, so nothing floats. A tree feature makes trees at the water. Use small features\nfor the bank, unless you want trees at the water.\n\n### 4.4 Bank feature examples\n\n**Reeds and bushes on overworld banks, but not in frozen biomes:**\n```json\n{\n  \"features\": [\"minecraft:patch_sugar_cane\", \"minecraft:patch_berry_bush\"],\n  \"tags\": [\"minecraft:is_overworld\"],\n  \"exclude_tags\": [\"minecraft:is_frozen\"],\n  \"chance\": 30\n}\n```\n\n**Palm trees in one modded biome only:**\n```json\n{\n  \"features\": [\"yourmod:palm_tree\"],\n  \"biomes\": [\"yourmod:tropical_beach\"],\n  \"chance\": 15\n}\n```\n\n---\n\n## 5. Config toggles\n\nThese options are in the main config file:\n\n- NeoForge and Forge: `config/streamsreflowing-common.toml`\n- Fabric: `config/streamsreflowing.properties`\n\n| Option | Values | Default | Function |\n|---|---|---|---|\n| `customBanks` | true or false | true | The main switch for bank styles. Set `false` to show raw carved land. |\n| `allWaterBanks` | -1, 0, 1, 2 | -1 | The range of the bank style and the bank feature pass. |\n\nThe `allWaterBanks` values are:\n\n- `0` \u2014 streams only.\n- `1` \u2014 streams, lakes, and ponds.\n- `2` \u2014 streams, lakes, ponds, and ocean shores.\n- `-1` \u2014 the mod selects the value from the quality preset.\n\nBank features have no main switch. They do nothing until you add a file.\n\n---\n\n## 6. How to make a datapack\n\nDo these steps to make a datapack for your bank files.\n\n1. Make a folder for the datapack. Use any name.\n2. Make a `pack.mcmeta` file in that folder. Put this text in it:\n\n```json\n{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"My custom stream banks\"\n  }\n}\n```\n\n3. Set the correct `pack_format` number for your Minecraft version:\n\n| Minecraft version | `pack_format` |\n|---|---|\n| 1.20.1 | 15 |\n| 1.21 / 1.21.1 | 48 |\n\n   The number is different for each Minecraft version, and it changes often. For a version\n   that is not in the table, search online for its data pack format number.\n\n   **Note:** Minecraft shows a warning for a wrong `pack_format`, but it loads the datapack in\n   most cases. The bank files still work.\n\n4. Make the folders for your files:\n   - Bank styles: `data/<namespace>/streamsreflowing/bank_style/`\n   - Bank features: `data/<namespace>/streamsreflowing/bank_feature/`\n5. Put your JSON files in the correct folder.\n6. Put the datapack folder in a world:\n   - Put it in the `datapacks/` folder of the world save. Then run `/reload`, or make a new\n     world.\n7. To use the datapack in all worlds, use a global-datapack mod.\n\n---\n\n## 7. Quick reference\n\n| Task | Action |\n|---|---|\n| Change the bank blocks in one biome | Make a bank style with `biomes` or `tags` and `bed`/`waterline`/`bank`. |\n| Leave one biome's stream raw | Make a bank style: `{ \"biomes\": [...], \"enabled\": false }`. |\n| \"All overworld but not frozen\" | Use `\"tags\": [\"...is_overworld\"]` and `\"exclude_tags\": [\"...is_frozen\"]`. |\n| Keep the bank natural, skin the bed | Add `\"bank_enabled\": false`. |\n| Put plants on the banks | Make a bank feature with a `features` list. |\n| Send it with a modpack | Put the JSON in `data/<namespace>/streamsreflowing/bank_style` or `bank_feature`. |\n\n");
            Path path2 = path.resolve("examples");
            Files.createDirectories(path2, new FileAttribute[0]);
            BankFeatureLoader.a(path2.resolve("example.json"), c);
        }
        catch (IOException iOException) {
            Constants.LOG.warn("Failed to seed custom bank features config in {}", (Object)path, (Object)iOException);
        }
    }

    private static void a(Path path, String string) throws IOException {
        if (!Files.exists(path, new LinkOption[0])) {
            Files.writeString(path, (CharSequence)string, new OpenOption[0]);
        }
    }
}

