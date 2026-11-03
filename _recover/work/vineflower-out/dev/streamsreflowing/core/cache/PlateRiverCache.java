package dev.streamsreflowing.core.cache;

import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class PlateRiverCache {
   private static final int a = 128;
   private static final Set<PlateRiverCache> b = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
   private static volatile boolean c;
   private final ConcurrentHashMap<PlateKey, CompletableFuture<RiverNetwork>> d = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<PlateKey> e = new ConcurrentLinkedQueue<>();
   private final ConcurrentHashMap<PlateKey, CompletableFuture<Void>> f = new ConcurrentHashMap<>();
   private static final long g = 300000000000L;

   public PlateRiverCache() {
      b.add(this);
   }

   public static void releaseAllWaiters() {
      c = true;
      synchronized (b) {
         for (PlateRiverCache var2 : b) {
            for (Entry var4 : var2.f.entrySet()) {
               ((CompletableFuture)var4.getValue()).complete(null);
            }

            var2.f.clear();
         }
      }
   }

   public static void reopen() {
      c = false;
   }

   public RiverNetwork get(PlateKey var1, Supplier<RiverNetwork> var2, Supplier<RiverNetwork> var3) {
      CompletableFuture var4 = this.d.get(var1);
      if (var4 != null) {
         return a(var4, var3);
      } else {
         CompletableFuture var5 = new CompletableFuture();
         CompletableFuture var6 = this.d.putIfAbsent(var1, var5);
         if (var6 != null) {
            return a(var6, var3);
         } else {
            RiverNetwork var7;
            try {
               var7 = (RiverNetwork)var2.get();
            } catch (Throwable var10) {
               this.d.remove(var1, var5);
               var5.completeExceptionally(var10);
               CompletableFuture var9 = this.f.remove(var1);
               if (var9 != null) {
                  var9.complete(null);
               }

               throw var10;
            }

            var5.complete(var7);
            CompletableFuture var8 = this.f.remove(var1);
            if (var8 != null) {
               var8.complete(null);
            }

            this.e.offer(var1);

            while (this.d.size() > 128) {
               PlateKey var11 = this.e.poll();
               if (var11 == null) {
                  break;
               }

               if (!var11.equals(var1)) {
                  this.d.remove(var11);
               }
            }

            return var7;
         }
      }
   }

   private static RiverNetwork a(CompletableFuture<RiverNetwork> var0, Supplier<RiverNetwork> var1) {
      if (var0.isDone()) {
         return (RiverNetwork)var0.join();
      } else {
         boolean var2 = Thread.currentThread().getName().startsWith("streamsreflowing-");
         LowResGrid.noteUrgentWaiter();

         RiverNetwork var15;
         try {
            if (var2 || var1 == null) {
               return (RiverNetwork)var0.join();
            }

            try {
               return (RiverNetwork)var0.get(300000000000L, TimeUnit.NANOSECONDS);
            } catch (TimeoutException var10) {
               return (RiverNetwork)var1.get();
            } catch (InterruptedException var11) {
               Thread.currentThread().interrupt();
               var15 = (RiverNetwork)var1.get();
            } catch (ExecutionException var12) {
               Throwable var4 = var12.getCause();
               throw new CompletionException(var4);
            }
         } finally {
            LowResGrid.urgentWaiterDone();
         }

         return var15;
      }
   }

   public CompletableFuture<Void> whenReady(PlateKey var1) {
      if (c) {
         return CompletableFuture.completedFuture(null);
      } else {
         CompletableFuture var2 = this.d.get(var1);
         if (var2 != null) {
            return var2.handle((var0, var1x) -> null);
         } else {
            CompletableFuture var3 = this.f.computeIfAbsent(var1, var0 -> new CompletableFuture<>());
            CompletableFuture var4 = this.d.get(var1);
            if (var4 != null && var4.isDone()) {
               this.f.remove(var1);
               var3.complete(null);
            }

            return var3;
         }
      }
   }

   public boolean contains(PlateKey var1) {
      return this.d.containsKey(var1);
   }

   public boolean isReady(PlateKey var1) {
      CompletableFuture var2 = this.d.get(var1);
      return var2 != null && var2.isDone() && !var2.isCompletedExceptionally() && !var2.isCancelled();
   }

   public int size() {
      return this.d.size();
   }

   public void clear() {
      this.d.clear();
      this.e.clear();
   }
}
