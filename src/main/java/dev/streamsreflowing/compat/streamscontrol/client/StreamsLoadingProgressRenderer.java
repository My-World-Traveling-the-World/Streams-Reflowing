package dev.streamsreflowing.compat.streamscontrol.client;

import dev.streamsreflowing.fabric.StreamsConfigFabric;
import java.util.Locale;
import dev.streamsreflowing.compat.streamscontrol.StreamsStartupProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Draws the river startup prewarm as a collapsible card on the level loading screen.
 *
 * <h2>Shape</h2>
 * The same frame as the wall and road cards: a theme dot and title, a river line-art band whose
 * water lights left-to-right with the overall fraction, a single-stage stepper, a live status
 * line and an ETA line. The river prewarm has one stage, so the stepper holds a single node and
 * the detail area shows the cached/build-worker/elapsed breakdown.
 *
 * <h2>Why there is no collapse</h2>
 * Matching the other two cards keeps the three startup stages reading as one family. All three
 * draw their details unconditionally: the card only exists while the world is being created,
 * when the player has nothing else to look at, and a toggle would hide the numbers that say the
 * run is alive behind one click.
 */
public final class StreamsLoadingProgressRenderer {
    private static final int PADDING = 10;
    private static final int LINE = 12;
    private static final int CARD_BACKGROUND = 0xFF10131A;
    private static final int CARD_BORDER = 0xFF404B5A;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFB7C0CC;
    private static final int TEXT_MUTED = 0xFF8D97A6;
    private static final int THEME = 0xFF4CC2E8;          // cyan
    private static final int THEME_DIM = 0xFF2A5A6E;      // unlit water
    private static final int STEP_DONE = 0xFF7FD07F;
    // Opaque on purpose (same as the wall card): the vanilla loading screen draws its own centred
    // percentage, and a translucent band let that box bleed through the river artwork - it read as
    // a second, contradictory progress number.
    private static final int BAND_BG = 0xFF101622;
    private static final int WATER_LIT = 0xFF7ADCF5;
    private static final int WATER_DEEP = 0xFF3E9CC8;     // deeper water under the crest
    private static final int WATER_SPARK = 0xFFDFF7FF;
    private static final int SAND_DIM = 0xFF2E2C24;       // bank sand (dim)
    private static final int SAND_LIT = 0xFF6A6046;       // bank sand (lit)
    private static final int BANK_DIM = 0xFF2A3428;       // 河岸（暗）
    private static final int BANK_LIT = 0xFF4A5A3C;       // 河岸（亮）
    private static final int REED_DIM = 0xFF2E3A2C;       // 芦苇（暗）
    private static final int REED_LIT = 0xFF6A8A52;       // 芦苇（亮）
    private static final int WEED = 0xFF3E6A50;           // 水草
    private static final int STONE = 0xFF5A5A54;          // 卵石
    private static final int RIPPLE = 0xFF9FE4F8;         // 涟漪
    private static final int FISH = 0xFFDFF7FF;           // 游鱼
    private static final int BIRD = 0xFFCFD8DC;           // 水鸟

    private StreamsLoadingProgressRenderer() {
    }

    public static void render(GuiGraphics graphics) {
        if (!StreamsConfigFabric.loadingNotice()) return;
        StreamsStartupProgress.Snapshot snapshot = StreamsStartupProgress.snapshot();
        if (!snapshot.active() || snapshot.totalPlates() <= 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return;
        Font font = minecraft.font;

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        // 卡片占据屏幕宽度 9/10；线稿带固定 120px（调好的显示比例），详情区一直显示，不再折叠。
        int width = Math.max(280, (screenWidth * 9) / 10);
        int innerWidth = width - PADDING * 2;
        int bandHeight = 120;

        int detailLines = 1;
        int height = PADDING
                + LINE                  // title row
                + bandHeight            // river band
                + 6
                + LINE                  // stepper
                + 4
                + LINE                  // status
                + LINE                  // eta
                + detailLines * LINE + 4
                + PADDING;

        int left = (screenWidth - width) / 2;
        // The river card anchors centred, lifted off the bottom edge by about an eighth of the
        // screen height so it does not hug the corner.
        int top = Math.max(20, screenHeight - height - Math.max(40, (screenHeight * 12) / 100));
        int right = left + width;
        int bottom = top + height;
        int x = left + PADDING;
        int y = top + PADDING;

        graphics.fill(left, top, right, bottom, CARD_BACKGROUND);
        graphics.fill(left, top, right, top + 1, CARD_BORDER);
        graphics.fill(left, bottom - 1, right, bottom, CARD_BORDER);
        graphics.fill(left, top, left + 1, bottom, CARD_BORDER);
        graphics.fill(right - 1, top, right, bottom, CARD_BORDER);

        // Title row: theme dot, title, overall percent.
        drawDot(graphics, x + 3, y + 4, 3, THEME);
        Component title = Component.translatable("plaincraft.streams.loading_progress.title");
        graphics.drawString(font, title, x + 10, y, TEXT_PRIMARY, false);
        String pct = snapshot.percent() + "%";
        graphics.drawString(font, pct, right - PADDING - font.width(pct), y, THEME, false);

        y += LINE;
        drawBand(graphics, x, y, innerWidth, bandHeight, snapshot);

        y += bandHeight + 6;
        drawStepper(graphics, x, y, innerWidth, snapshot);

        y += LINE + 4;
        Component status = Component.translatable("plaincraft.streams.loading_progress.summary",
                snapshot.completedPlates(), snapshot.totalPlates(), snapshot.percent());
        drawFitted(graphics, font, status, x, y, innerWidth, TEXT_SECONDARY);

        y += LINE;
        drawFitted(graphics, font, etaLine(snapshot), x, y, innerWidth, TEXT_MUTED);

        y += LINE + 4;
        String elapsedSeconds = String.format(Locale.ROOT, "%.1f", snapshot.elapsedMillis() / 1000.0D);
        Component detail = Component.translatable("plaincraft.streams.loading_progress.detail",
                snapshot.alreadyReadyPlates(), snapshot.buildWorkers(), elapsedSeconds);
        drawFitted(graphics, font, detail, x, y, innerWidth, TEXT_MUTED);
    }

    // ---------------------------------------------------------------------------------------------
    // River band: a meandering river whose water lights left-to-right with the fraction, with a
    // phase-animated wave so the water reads as flowing.
    // ---------------------------------------------------------------------------------------------

    private static void drawBand(GuiGraphics graphics, int x, int y, int width, int height,
                                 StreamsStartupProgress.Snapshot snapshot) {
        graphics.fill(x, y, x + width, y + height, BAND_BG);
        int litWidth = Math.round(width * snapshot.percent() / 100.0F);
        // Sandy banks with pebbles and reeds first so the water draws over their inner edge: dark
        // in full, lit on the watered prefix.
        drawBanks(graphics, x, y, width, height, 0, width, false);
        drawBanks(graphics, x, y, width, height, 0, litWidth, true);
        // Water weeds under the lit water, then the river itself.
        drawWeeds(graphics, x, y, width, height, 0, litWidth);
        // Dim river in full, then the lit prefix in bright water with a deeper body.
        drawRiver(graphics, x, y, width, height, 0, width, THEME_DIM, false, false);
        drawRiver(graphics, x, y, width, height, 0, litWidth, WATER_LIT, true, true);
        // Ripples, fish and a water bird ride the lit water only.
        drawRipples(graphics, x, y, width, height, 0, litWidth);
        drawFish(graphics, x, y, width, height, 0, litWidth);
        drawBird(graphics, x, y, width, height, litWidth);
    }

    /**
     * Draws the river over the x-range [{@code from}, {@code to}] of the band. The river is a
     * meandering water body with a deeper core and a lighter crest; the wave phase animates from
     * the wall-clock so the lit water appears to flow.
     */
    private static void drawRiver(GuiGraphics graphics, int x, int y, int width, int height,
                                  int from, int to, int color, boolean animate, boolean depth) {
        if (to <= from) return;
        long phase = animate ? (System.currentTimeMillis() / 160L) : 0L;
        int deep = depth ? WATER_DEEP : color;
        for (int i = Math.max(0, from); i < Math.min(width, to); i++) {
            int centre = y + height / 2 + riverBend(i, width);
            // Water body: a bright top layer with a deeper core beneath it.
            graphics.fill(x + i, centre - 4, x + i + 1, centre - 1, color);
            graphics.fill(x + i, centre - 1, x + i + 1, centre + 4, deep);
            // Wave crest: a lighter pixel riding the water, drifting with the phase.
            if (animate && (((i + (int) (phase % 6L)) % 6) == 0)) {
                graphics.fill(x + i, centre - 6, x + i + 1, centre - 4, WATER_SPARK);
            }
        }
    }

    /** The two sandy banks hugging the river's meander, with pebbles and reeds at the waterline.
     *  Dark on the dry prefix, lit where the water has reached. */
    private static void drawBanks(GuiGraphics graphics, int x, int y, int width, int height,
                                  int from, int to, boolean lit) {
        if (to <= from) return;
        int sand = lit ? SAND_LIT : SAND_DIM;
        int bank = lit ? BANK_LIT : BANK_DIM;
        int reed = lit ? REED_LIT : REED_DIM;
        for (int i = Math.max(0, from); i < Math.min(width, to); i++) {
            int centre = y + height / 2 + riverBend(i, width);
            // Sand strips just outside the water, then a grass line beyond them.
            graphics.fill(x + i, centre - 7, x + i + 1, centre - 4, sand);
            graphics.fill(x + i, centre + 4, x + i + 1, centre + 7, sand);
            graphics.fill(x + i, centre - 10, x + i + 1, centre - 7, bank);
            graphics.fill(x + i, centre + 7, x + i + 1, centre + 10, bank);
            // Pebbles on the sand.
            if ((i % 13) == 3) {
                graphics.fill(x + i, centre - 7, x + i + 3, centre - 4, STONE);
            }
            if ((i % 19) == 9) {
                graphics.fill(x + i, centre + 4, x + i + 3, centre + 7, STONE);
            }
            // Reeds: sparse tufts above the top bank and below the bottom bank.
            if ((i % 11) == 0) {
                int h = (3 + (i % 4));
                graphics.fill(x + i, centre - 10 - h, x + i + 1, centre - 10, reed);
            }
            if ((i % 17) == 6) {
                int h = (3 + (i % 3));
                graphics.fill(x + i, centre + 10, x + i + 1, centre + 10 + h, reed);
            }
        }
    }

    /** Water weeds swaying under the lit water, drawn before the water so they show through. */
    private static void drawWeeds(GuiGraphics graphics, int x, int y, int width, int height,
                                  int from, int to) {
        if (to <= from) return;
        long phase = System.currentTimeMillis() / 400L;
        for (int i = Math.max(0, from); i < Math.min(width, to); i++) {
            if ((i % 15) == 5) {
                int centre = y + height / 2 + riverBend(i, width);
                int sway = (int) (phase % 3L) - 1;
                // A short weed stalk leaning with the phase, rising from the river bed.
                graphics.fill(x + i + sway, centre + 1, x + i + sway + 2, centre + 5, WEED);
                graphics.fill(x + i, centre + 4, x + i + 1, centre + 7, WEED);
            }
        }
    }

    /** Expanding ripple rings on the lit water, drifting downstream with the phase. */
    private static void drawRipples(GuiGraphics graphics, int x, int y, int width, int height,
                                    int from, int to) {
        if (to <= from) return;
        long phase = System.currentTimeMillis() / 240L;
        for (int i = Math.max(0, from); i < Math.min(width, to); i++) {
            // A ripple centre every so often; the ring grows and fades with the phase.
            if (((i + (int) (phase % 9L)) % 27) == 0) {
                int centre = y + height / 2 + riverBend(i, width);
                int r = (2 + (int) (phase % 4L));
                // A broken ring: top and bottom arcs only, so it reads as a water ring.
                graphics.fill(x + i - r, centre - r - 2, x + i + r + 1, centre - r, RIPPLE);
                graphics.fill(x + i - r, centre + r + 2, x + i + r + 1, centre + r + 4, RIPPLE);
            }
        }
    }

    /** Small fish gliding along the lit water, their tail flick alternating with the phase. */
    private static void drawFish(GuiGraphics graphics, int x, int y, int width, int height,
                                 int from, int to) {
        if (to <= from) return;
        long phase = System.currentTimeMillis() / 320L;
        // A couple of fish at fixed offsets along the river, drifting slowly downstream.
        for (int base = 20; base < Math.min(width, to); base += 60) {
            int i = base + (int) (phase % 12L);
            if (i < from || i >= Math.min(width, to)) continue;
            int centre = y + height / 2 + riverBend(i, width);
            // Body: three pixels; tail flicks up or down with the phase.
            graphics.fill(x + i, centre - 1, x + i + 3, centre + 1, FISH);
            boolean tailUp = (phase % 2L) == 0L;
            graphics.fill(x + i - 2, centre + (tailUp ? -2 : 1), x + i, centre + (tailUp ? 0 : 3), FISH);
        }
    }

    /** A water bird flying across the band above the river, wings flapping with the phase. */
    private static void drawBird(GuiGraphics graphics, int x, int y, int width, int height,
                                 int litWidth) {
        if (litWidth <= 0) return;
        long phase = System.currentTimeMillis() / 500L;
        // The bird glides slowly across the whole band, above the water.
        int i = (int) ((phase * 2L) % Math.max(1, width));
        int by = y + 10 + (int) (4 * Math.sin(phase % 6L));
        // Body and two wings that flap up/down.
        graphics.fill(x + i, by, x + i + 5, by + 2, BIRD);
        boolean wingsUp = (phase % 2L) == 0L;
        if (wingsUp) {
            graphics.fill(x + i, by - 3, x + i + 2, by, BIRD);
            graphics.fill(x + i + 3, by - 3, x + i + 5, by, BIRD);
        } else {
            graphics.fill(x + i, by + 2, x + i + 2, by + 5, BIRD);
            graphics.fill(x + i + 3, by + 2, x + i + 5, by + 5, BIRD);
        }
    }

    private static int riverBend(int i, int width) {
        double t = i / (double) Math.max(1, width);
        return (int) Math.round(9 * Math.sin(t * Math.PI * 2.0) + 4 * Math.sin(t * Math.PI * 5.0));
    }

    // ---------------------------------------------------------------------------------------------
    // Stepper: the river prewarm is a single stage, so one node - pulsing while active, checked
    // when the plates finish.
    // ---------------------------------------------------------------------------------------------

    private static void drawStepper(GuiGraphics graphics, int x, int y, int width,
                                    StreamsStartupProgress.Snapshot snapshot) {
        int lineY = y + 5;
        graphics.fill(x, lineY, x + width, lineY + 1, 0xFF5A6472);
        int litX = x + Math.round(width * snapshot.percent() / 100.0F);
        graphics.fill(x, lineY, Math.min(x + width, litX), lineY + 1, THEME);
        int cx = x + width / 2;
        if (snapshot.percent() >= 100) {
            drawCheck(graphics, cx, lineY, STEP_DONE);
        } else {
            drawPulse(graphics, cx, lineY, THEME);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Text + small drawing helpers.
    // ---------------------------------------------------------------------------------------------

    private static Component etaLine(StreamsStartupProgress.Snapshot snapshot) {
        // Elapsed only: an early-stage remaining estimate is unreliable and reads as a hang.
        return Component.translatable("plaincraft.streams.loading_progress.elapsed_only",
                formatDuration(snapshot.elapsedMillis()));
    }

    private static void drawCheck(GuiGraphics graphics, int cx, int cy, int color) {
        drawDot(graphics, cx, cy, 4, color);
        graphics.fill(cx - 1, cy, cx, cy + 2, 0xFF0E2A14);
        graphics.fill(cx, cy + 1, cx + 2, cy - 1, 0xFF0E2A14);
    }

    private static void drawPulse(GuiGraphics graphics, int cx, int cy, int color) {
        long t = System.currentTimeMillis() / 240L;
        int ring = (t % 2L) == 0L ? color : WATER_SPARK;
        drawRing(graphics, cx, cy, 4, ring);
        drawDot(graphics, cx, cy, 2, color);
    }

    private static void drawDot(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        graphics.fill(cx - radius, cy - radius + 1, cx + radius, cy + radius - 1, color);
        graphics.fill(cx - radius + 1, cy - radius, cx + radius - 1, cy + radius, color);
    }

    private static void drawRing(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        graphics.fill(cx - radius, cy - radius + 1, cx - radius + 1, cy + radius - 1, color);
        graphics.fill(cx + radius - 1, cy - radius + 1, cx + radius, cy + radius - 1, color);
        graphics.fill(cx - radius + 1, cy - radius, cx + radius - 1, cy - radius + 1, color);
        graphics.fill(cx - radius + 1, cy + radius - 1, cx + radius - 1, cy + radius, color);
    }

    private static void drawFitted(GuiGraphics graphics, Font font, Component text,
                                   int x, int y, int maxWidth, int color) {
        String raw = text.getString();
        if (font.width(raw) <= maxWidth) {
            graphics.drawString(font, raw, x, y, color, false);
            return;
        }
        String clipped = font.plainSubstrByWidth(raw, Math.max(0, maxWidth - font.width("...")));
        graphics.drawString(font, clipped + "...", x, y, color, false);
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        if (seconds >= 60L) {
            return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
        }
        return seconds + "s";
    }
}
