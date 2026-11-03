/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.core.cache;

import dev.streamsreflowing.core.grid.LowResGrid;
import dev.streamsreflowing.core.plate.PlateKey;
import dev.streamsreflowing.core.river.RiverNetwork;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
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
    private static final Set<PlateRiverCache> b = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap()));
    private static volatile boolean c;
    private final ConcurrentHashMap<PlateKey, CompletableFuture<RiverNetwork>> d = new ConcurrentHashMap();
    private final ConcurrentLinkedQueue<PlateKey> e = new ConcurrentLinkedQueue();
    private final ConcurrentHashMap<PlateKey, CompletableFuture<Void>> f = new ConcurrentHashMap();
    private static final long g = 300000000000L;

    public PlateRiverCache() {
        b.add(this);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void releaseAllWaiters() {
        c = true;
        Set<PlateRiverCache> set = b;
        synchronized (set) {
            for (PlateRiverCache plateRiverCache : b) {
                for (Map.Entry<PlateKey, CompletableFuture<Void>> entry : plateRiverCache.f.entrySet()) {
                    entry.getValue().complete(null);
                }
                plateRiverCache.f.clear();
            }
        }
    }

    public static void reopen() {
        c = false;
    }

    public RiverNetwork get(PlateKey plateKey, Supplier<RiverNetwork> supplier, Supplier<RiverNetwork> supplier2) {
        PlateKey plateKey2;
        RiverNetwork riverNetwork;
        CompletableFuture<RiverNetwork> completableFuture = this.d.get(plateKey);
        if (completableFuture != null) {
            return PlateRiverCache.a(completableFuture, supplier2);
        }
        CompletableFuture<RiverNetwork> completableFuture2 = new CompletableFuture<RiverNetwork>();
        CompletableFuture<RiverNetwork> completableFuture3 = this.d.putIfAbsent(plateKey, completableFuture2);
        if (completableFuture3 != null) {
            return PlateRiverCache.a(completableFuture3, supplier2);
        }
        try {
            riverNetwork = supplier.get();
        }
        catch (Throwable throwable) {
            this.d.remove(plateKey, completableFuture2);
            completableFuture2.completeExceptionally(throwable);
            CompletableFuture<Void> completableFuture4 = this.f.remove(plateKey);
            if (completableFuture4 != null) {
                completableFuture4.complete(null);
            }
            throw throwable;
        }
        completableFuture2.complete(riverNetwork);
        CompletableFuture<Void> completableFuture5 = this.f.remove(plateKey);
        if (completableFuture5 != null) {
            completableFuture5.complete(null);
        }
        this.e.offer(plateKey);
        while (this.d.size() > 128 && (plateKey2 = this.e.poll()) != null) {
            if (plateKey2.equals(plateKey)) continue;
            this.d.remove(plateKey2);
        }
        return riverNetwork;
    }

    private static RiverNetwork a(CompletableFuture<RiverNetwork> completableFuture, Supplier<RiverNetwork> supplier) {
        if (completableFuture.isDone()) {
            return completableFuture.join();
        }
        boolean bl = PlateRiverCache.a(Thread.currentThread().getName());
        LowResGrid.noteUrgentWaiter();
        try {
            if (bl || supplier == null) {
                RiverNetwork riverNetwork = completableFuture.join();
                return riverNetwork;
            }
            RiverNetwork riverNetwork = completableFuture.get(300000000000L, TimeUnit.NANOSECONDS);
            return riverNetwork;
        }
        catch (TimeoutException timeoutException) {
            RiverNetwork riverNetwork = supplier.get();
            LowResGrid.urgentWaiterDone();
            return riverNetwork;
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            RiverNetwork riverNetwork = supplier.get();
            LowResGrid.urgentWaiterDone();
            return riverNetwork;
        }
        catch (ExecutionException executionException) {
            throw new CompletionException(executionException.getCause());
        }
        finally {
            LowResGrid.urgentWaiterDone();
        }
    }

    // D01: which threads may wait for a plate without the 300 s cap.
    //
    // "streamsreflowing-*" are this mod's own sampler/prefetch/urgent/framediag workers. The two prewarm
    // pools are named "Streams-Startup-Prewarm-N" and "Streams-Runtime-Prewarm-N" (StreamsPrewarmService
    // NamedThreadFactory) - capital S and no "reflowing" - so the old all-lowercase
    // startsWith("streamsreflowing-") never matched them: every prewarm worker fell through to the timed
    // path and could sit for 300 s before recomputing the plate locally.
    //
    // Behaviour change (intentional): prewarm workers now join() instead of timing out. That cannot
    // deadlock here, because the wait-for graph is two levels deep and acyclic - the connected-network
    // cache (RiverEngine.k) is built from base networks only, and a base network (RiverEngine.j) is
    // built without waiting on any other future - so a waiting worker always waits on a builder that
    // itself waits on nobody. Threads that are not ours (server tick, render, other mods) keep the
    // timed fallback below unchanged, which is exactly what it exists for: a stuck plate must not
    // freeze them.
    private static boolean a(String string) {
        String string2 = string.toLowerCase(Locale.ROOT);
        return string2.startsWith("streamsreflowing-")
                || string2.startsWith("streams-startup-prewarm-")
                || string2.startsWith("streams-runtime-prewarm-");
    }

    public CompletableFuture<Void> whenReady(PlateKey plateKey2) {
        if (c) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<RiverNetwork> completableFuture = this.d.get(plateKey2);
        if (completableFuture != null) {
            return completableFuture.handle((riverNetwork, throwable) -> null);
        }
        CompletableFuture completableFuture2 = this.f.computeIfAbsent(plateKey2, plateKey -> new CompletableFuture());
        CompletableFuture<RiverNetwork> completableFuture3 = this.d.get(plateKey2);
        if (completableFuture3 != null && completableFuture3.isDone()) {
            this.f.remove(plateKey2);
            completableFuture2.complete(null);
        }
        return completableFuture2;
    }

    public boolean contains(PlateKey plateKey) {
        return this.d.containsKey(plateKey);
    }

    public boolean isReady(PlateKey plateKey) {
        CompletableFuture<RiverNetwork> completableFuture = this.d.get(plateKey);
        return completableFuture != null && completableFuture.isDone() && !completableFuture.isCompletedExceptionally() && !completableFuture.isCancelled();
    }

    public int size() {
        return this.d.size();
    }

    public void clear() {
        this.d.clear();
        this.e.clear();
    }
}

