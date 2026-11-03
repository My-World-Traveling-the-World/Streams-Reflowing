package dev.streamsreflowing.client;

import dev.streamsreflowing.Constants;
import dev.streamsreflowing.flow.FlowTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.player.LocalPlayer;

public final class HarnessPilot {
   private static final boolean ON = "fly".equals(System.getProperty("streamsreflowing.harness"));
   private static final double SPEED = (double)Integer.getInteger("streamsreflowing.harnessspeed", 13).intValue() / 20.0;
   private static final int SETTLE_TICKS = 200;
   private static final int REPORT_TICKS = 100;
   private static int ticksInWorld;
   private static double cruiseY = Double.NaN;
   private static int wallTicks;
   private static boolean inWorldLogged;

   private HarnessPilot() {
   }

   public static boolean active() {
      return ON;
   }

   public static void tick(Minecraft mc) {
      if (ON) {
         LocalPlayer player = mc.player;
         if (player != null && mc.level != null) {
            ticksInWorld++;
            if (!inWorldLogged && mc.screen == null) {
               inWorldLogged = true;
               Constants.LOG.info("[SR-HARNESS] in-world (drop-in screen closed)");
            }

            if (ticksInWorld >= 200) {
               if (Double.isNaN(cruiseY)) {
                  if (player.getAbilities().mayfly && !player.getAbilities().flying) {
                     player.getAbilities().flying = true;
                     player.onUpdateAbilities();
                  }

                  cruiseY = player.getY() + 60.0;
                  Constants.LOG.info("[SR-HARNESS] takeoff at x={} z={} cruiseY={}", (int)player.getX(), (int)player.getZ(), (int)cruiseY);
               }

               if (player.getY() < cruiseY) {
                  player.setPos(player.getX(), Math.min(cruiseY, player.getY() + 0.375), player.getZ());
               } else if (mc.level.hasChunk(((int)Math.floor(player.getX() + SPEED) >> 4) + 1, (int)Math.floor(player.getZ()) >> 4)) {
                  player.setPos(player.getX() + SPEED, cruiseY, player.getZ());
               } else {
                  wallTicks++;
               }

               player.setDeltaMovement(0.0, 0.0, 0.0);
               player.fallDistance = 0.0F;
               if (ticksInWorld % 100 == 0) {
                  Constants.LOG
                     .info(
                        "[SR-HARNESS] t={} x={} carves={} stalls={} wall={}",
                        (ticksInWorld - 200) / 20,
                        (int)player.getX(),
                        FlowTrace.carvedChunks.get(),
                        FlowTrace.harnessStalls.get(),
                        wallTicks / 20
                     );
               }
            }
         } else {
            if (mc.screen instanceof ConfirmScreen cs) {
               for (GuiEventListener child : cs.children()) {
                  if (child instanceof Button b) {
                     String label = b.getMessage().getString();
                     if (label.equalsIgnoreCase("Proceed") || label.equalsIgnoreCase("Yes") || label.equalsIgnoreCase("Create Backup and Load")) {
                        Constants.LOG.info("[SR-HARNESS] auto-confirmed '{}'", label);
                        b.onPress();
                        break;
                     }
                  }
               }
            }

            ticksInWorld = 0;
            cruiseY = Double.NaN;
            inWorldLogged = false;
         }
      }
   }
}
