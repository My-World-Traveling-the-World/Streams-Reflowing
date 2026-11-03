/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.flow;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public final class RTFFlowUnderlay {
    private static final int MAX = 16384;
    private static final ConcurrentHashMap<Long, byte[]> CLIENT = new ConcurrentHashMap();
    private static final ConcurrentHashMap<Long, byte[]> SERVER = new ConcurrentHashMap();

    private RTFFlowUnderlay() {
    }

    private static long key(int cx, int cz) {
        return (long)cx << 32 ^ (long)cz & 0xFFFFFFFFL;
    }

    private static ConcurrentHashMap<Long, byte[]> side(boolean client) {
        return client ? CLIENT : SERVER;
    }

    public static byte[] convert(byte[] raw) {
        if (raw == null || raw.length != 256) {
            return null;
        }
        byte[] out = null;
        for (int i = 0; i < 256; ++i) {
            int b = raw[i] & 0xFF;
            if (b >>> 5 == 0) {
                if (out == null) continue;
                out[i] = -1;
                continue;
            }
            if (out == null) {
                out = new byte[256];
                Arrays.fill(out, 0, i, (byte)-1);
            }
            out[i] = (byte)Math.round((double)(b & 0x1F) * 255.0 / 32.0);
        }
        return out;
    }

    public static boolean has(boolean client, int cx, int cz) {
        return RTFFlowUnderlay.side(client).containsKey(RTFFlowUnderlay.key(cx, cz));
    }

    public static void put(boolean client, int cx, int cz, byte[] grid) {
        ConcurrentHashMap<Long, byte[]> map = RTFFlowUnderlay.side(client);
        map.put(RTFFlowUnderlay.key(cx, cz), grid);
        if (map.size() > 16384) {
            Iterator it = ((ConcurrentHashMap.KeySetView)map.keySet()).iterator();
            while (map.size() > 12288 && it.hasNext()) {
                it.next();
                it.remove();
            }
        }
    }

    public static void remove(boolean client, int cx, int cz) {
        RTFFlowUnderlay.side(client).remove(RTFFlowUnderlay.key(cx, cz));
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public static void clearServer() {
        SERVER.clear();
    }

    public static byte angleAt(boolean client, int x, int z) {
        byte[] g = RTFFlowUnderlay.side(client).get(RTFFlowUnderlay.key(x >> 4, z >> 4));
        return g == null ? (byte)-1 : g[(x & 0xF) + ((z & 0xF) << 4)];
    }

    public static boolean empty() {
        return CLIENT.isEmpty() && SERVER.isEmpty();
    }
}

