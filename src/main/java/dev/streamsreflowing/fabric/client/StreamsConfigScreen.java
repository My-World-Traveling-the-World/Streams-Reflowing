/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.fabric.client;

import dev.streamsreflowing.core.config.QualityPreset;
import dev.streamsreflowing.fabric.StreamsConfigFabric;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class StreamsConfigScreen
extends Screen {
    private int rowsPerPage = 6;
    private final Screen parent;
    private final List<String[]> rows = new ArrayList<String[]>();
    private final List<String> pageOrder = new ArrayList<String>();
    private final Map<String, String> edited = new HashMap<String, String>();
    private int page;
    private String pageTitle = "";
    private final List<Object[]> pageLabels = new ArrayList<Object[]>();
    private static final int TOOLTIP_MAX_LINES = 7;
    private static final Pattern CHOICE = Pattern.compile("^(\\d+)\\s*=\\s*(.+)$");

    public StreamsConfigScreen(Screen parent) {
        super(Component.literal("Streams Reflowing"));
        this.parent = parent;
        for (String[] kv : StreamsConfigFabric.snapshotOrdered(false)) {
            this.rows.add(new String[]{kv[0], kv[1], "s", kv.length > 2 ? kv[2] : "", kv.length > 3 ? kv[3] : "GENERAL"});
        }
        for (String[] kv : StreamsConfigFabric.snapshotOrdered(true)) {
            this.rows.add(new String[]{kv[0], kv[1], "c", kv.length > 2 ? kv[2] : "", kv.length > 3 ? kv[3] : "PARTICLES & SOUND"});
        }
        for (String[] row : this.rows) {
            if (this.pageOrder.contains(row[4])) continue;
            this.pageOrder.add(row[4]);
        }
    }

    private static String condense(String comment) {
        StringBuilder out = new StringBuilder();
        String cost = null;
        String applies = null;
        int kept = 0;
        for (String line : comment.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("Perf:")) {
                String p = t.substring(5).trim();
                cost = p.contains("BIGGEST DIAL") || p.contains("MAIN SPEED DIAL") ? "very high" : (p.startsWith("major") ? "high" : (p.contains("CLIENT FPS") ? "costs frames" : (p.startsWith("moderate") ? "moderate" : (p.startsWith("IMPROVES") ? "improves performance" : (p.startsWith("light") ? "light" : "negligible")))));
                continue;
            }
            if (t.startsWith("World:")) {
                String w = t.substring(6).trim();
                applies = w.contains("CHANGES THE STREAM NETWORK") ? "after a world reload -- changes stream layout" : (w.contains("RELOADED") ? "after a world reload, in new chunks" : (w.contains("restart the game") ? "after restarting the game" : (w.contains("chunks generated from now on") ? "to newly generated chunks" : "immediately")));
                continue;
            }
            if (kept >= 7) continue;
            out.append(t).append('\n');
            ++kept;
        }
        if (cost != null) {
            out.append("\nCost: ").append(cost).append('\n');
        }
        if (applies != null) {
            out.append("Applies: ").append(applies);
        }
        return out.toString().trim();
    }

    private static List<String[]> choicesOf(String comment) {
        ArrayList<String[]> out = new ArrayList<String[]>();
        for (String line : comment.split("\n")) {
            Matcher m = CHOICE.matcher(line.trim());
            if (!m.matches()) continue;
            Object label = m.group(2).trim();
            int cut = ((String)label).indexOf(58);
            if (cut < 0) {
                cut = ((String)label).indexOf(46);
            }
            if (cut > 0) {
                label = ((String)label).substring(0, cut);
            }
            if (((String)label).length() > 22) {
                label = ((String)label).substring(0, 21).trim() + "...";
            }
            out.add(new String[]{m.group(1), ((String)label).trim()});
        }
        return out.size() >= 2 ? out : null;
    }

    private List<String[]> inCategory(String category) {
        ArrayList<String[]> out = new ArrayList<String[]>();
        for (String[] row : this.rows) {
            if (!row[4].equals(category)) continue;
            out.add(row);
        }
        return out;
    }

    private List<Object[]> pages() {
        ArrayList<Object[]> out = new ArrayList<Object[]>();
        for (String category : this.pageOrder) {
            List<String[]> items = this.inCategory(category);
            for (int i = 0; i < items.size(); i += this.rowsPerPage) {
                out.add(new Object[]{category, items.subList(i, Math.min(items.size(), i + this.rowsPerPage))});
            }
        }
        if (out.isEmpty()) {
            out.add(new Object[]{"", new ArrayList()});
        }
        return out;
    }

    private int pageCount() {
        return this.pages().size();
    }

    private String currentValue(String[] row) {
        return this.edited.getOrDefault(row[0], row[1]);
    }

    private int firstPageOf(String category) {
        List<Object[]> pages = this.pages();
        for (int i = 0; i < pages.size(); ++i) {
            if (!category.equals(pages.get(i)[0])) continue;
            return i;
        }
        return 0;
    }

    @Override
    protected void init() {
        this.pageLabels.clear();
        int x = this.width / 2 - 155;
        List<Object[]> pages = this.pages();
        this.page = Math.floorMod(this.page, pages.size());
        Object[] thisPage = pages.get(this.page);
        this.pageTitle = (String)thisPage[0];
        List<String[]> shown = (List<String[]>)thisPage[1];
        for (int i = 0; i < this.pageOrder.size(); ++i) {
            String category = this.pageOrder.get(i);
            int tabX = this.width / 2 - 156 + i % 3 * 104;
            int tabY = 22 + i / 3 * 20;
            Button tab = Button.builder(Component.literal(StreamsConfigScreen.shortTab(category)), b -> {
                this.page = this.firstPageOf(category);
                this.clearWidgets();
                this.init();
            }).bounds(tabX, tabY, 102, 18).build();
            tab.active = !category.equals(this.pageTitle);
            this.addRenderableWidget(tab);
        }
        int y = 22 + (this.pageOrder.size() + 2) / 3 * 20 + 8;
        int room = (this.height - 80 - y) / 24;
        int fitted = Math.max(1, Math.min(8, room));
        if (fitted != this.rowsPerPage) {
            this.rowsPerPage = fitted;
            pages = this.pages();
            this.page = Math.floorMod(this.page, pages.size());
            thisPage = pages.get(this.page);
            this.pageTitle = (String)thisPage[0];
            shown = (List<String[]>)thisPage[1];
        }
        for (String[] row : shown) {
            Tooltip tip;
            String key = row[0];
            String value = this.currentValue(row);
            String doc = row.length > 3 ? StreamsConfigScreen.condense(row[3]) : "";
            Tooltip tooltip = tip = doc.isEmpty() ? null : Tooltip.create(Component.literal(doc));
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                CycleButton<Boolean> b2 = CycleButton.onOffBuilder(Boolean.parseBoolean(value)).create(x, y, 310, 20, Component.literal(key), (btn, v) -> this.edited.put(key, String.valueOf(v)));
                b2.setTooltip(tip);
                this.addRenderableWidget(b2);
            } else if ("terrainAccuracyLevel".equals(key)) {
                QualityPreset current = StreamsConfigScreen.presetOf(value);
                CycleButton<QualityPreset> b3 = CycleButton.<QualityPreset>builder(p -> Component.literal(p.name())).withValues((QualityPreset[])QualityPreset.values()).withInitialValue(current).create(x, y, 310, 20, Component.literal(key), (btn, v) -> this.edited.put(key, v.name()));
                b3.setTooltip(tip);
                this.addRenderableWidget(b3);
            } else if (StreamsConfigScreen.choicesOf(row.length > 3 ? row[3] : "") != null && value.matches("\\d+")) {
                List<String[]> choices = StreamsConfigScreen.choicesOf(row[3]);
                ArrayList<String> values = new ArrayList<String>();
                HashMap<String, String> labels = new HashMap<String, String>();
                for (String[] c : choices) {
                    values.add(c[0]);
                    labels.put(c[0], c[1]);
                }
                String start = values.contains(value) ? value : (String)values.get(0);
                CycleButton<String> b4 = CycleButton.<String>builder(v -> Component.literal(labels.getOrDefault(v, (String)v))).withValues((Collection<String>)values).withInitialValue(start).create(x, y, 310, 20, Component.literal(key), (btn, v) -> this.edited.put(key, (String)v));
                b4.setTooltip(tip);
                this.addRenderableWidget(b4);
            } else {
                EditBox box = new EditBox(this.font, this.width / 2 + 5, y, 150, 20, Component.literal(key));
                box.setMaxLength(4096);
                box.setValue(value);
                box.setResponder(v -> this.edited.put(key, (String)v));
                box.setTooltip(tip);
                this.addRenderableWidget(box);
                this.pageLabels.add(new Object[]{y, key, tip});
            }
            y += 24;
        }
        int navY = this.height - 52;
        if (this.pagesInSection() > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> this.flip(-1)).bounds(this.width / 2 - 60, navY, 20, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> this.flip(1)).bounds(this.width / 2 + 40, navY, 20, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose()).bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private String sectionPage() {
        List<Object[]> pages = this.pages();
        int idx = 0;
        for (int i = 0; i <= this.page && i < pages.size(); ++i) {
            if (!this.pageTitle.equals(pages.get(i)[0])) continue;
            ++idx;
        }
        return idx + "/" + this.pagesInSection();
    }

    private int pagesInSection() {
        int n = 0;
        for (Object[] p : this.pages()) {
            if (!this.pageTitle.equals(p[0])) continue;
            ++n;
        }
        return n;
    }

    private static String shortTab(String category) {
        switch (category) {
            case "PARTICLES & SOUND": {
                return "Particles & Sound";
            }
            case "PERFORMANCE": {
                return "Performance";
            }
            case "APPEARANCE": {
                return "Appearance";
            }
            case "EXPERIMENTAL": {
                return "Experimental";
            }
            case "WATER": {
                return "Water";
            }
            case "GENERAL": {
                return "General";
            }
        }
        String s = category.toLowerCase(Locale.ROOT);
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void flip(int dir) {
        List<Object[]> pages = this.pages();
        int first = this.firstPageOf(this.pageTitle);
        int span = this.pagesInSection();
        if (span > 1) {
            this.page = first + Math.floorMod(this.page - first + dir, span);
        }
        this.clearWidgets();
        this.init();
    }

    private static QualityPreset presetOf(String value) {
        for (QualityPreset p : QualityPreset.values()) {
            if (!p.name().equalsIgnoreCase(value)) continue;
            return p;
        }
        return QualityPreset.values()[0];
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        for (Object[] label : this.pageLabels) {
            gfx.drawString(this.font, (String)label[1], this.width / 2 - 155, (Integer)label[0] + 6, 0xFFFFFF);
        }
        gfx.drawCenteredString(this.font, Component.literal("Hover an option for what it does, what it costs, and when it takes effect."), this.width / 2, this.height - 72, 0xA0A0A0);
        if (this.pagesInSection() > 1) {
            gfx.drawCenteredString(this.font, Component.literal(this.sectionPage()), this.width / 2, this.height - 46, 16765823);
        }
    }

    @Override
    public void onClose() {
        HashMap<String, String> common = new HashMap<String, String>();
        HashMap<String, String> client = new HashMap<String, String>();
        for (String[] row : this.rows) {
            String value = this.edited.get(row[0]);
            if (value == null || value.equals(row[1])) continue;
            ("c".equals(row[2]) ? client : common).put(row[0], value);
        }
        if (!common.isEmpty() || !client.isEmpty()) {
            StreamsConfigFabric.setValues(common, client);
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}

