/*
 * Decompiled with CFR 0.152.
 */
package dev.streamsreflowing.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.streamsreflowing.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class BakedBandTable {
    private static final String[] a = new String[]{"tectonic", "mr_lithosphere", "mr_still_life", "terralith", "mr_epicterrain", "larion"};
    private final String b;
    private final String c;
    private final double[] d;
    private final double[] e;
    private final int[] f;
    private final double[] g;
    private final double[] h;

    private BakedBandTable(String string, String string2, double[] dArray, double[] dArray2, int[] nArray, double[] dArray3, double[] dArray4) {
        this.b = string;
        this.c = string2;
        this.d = dArray;
        this.e = dArray2;
        this.f = nArray;
        this.g = dArray3;
        this.h = dArray4;
    }

    public static BakedBandTable lookup(Predicate<String> predicate, UnaryOperator<String> unaryOperator) {
        TreeSet<String> treeSet = new TreeSet<String>();
        for (String n : a) {
            if (!predicate.test(n)) continue;
            treeSet.add(n);
        }
        String string = treeSet.isEmpty() ? "vanilla" : String.join((CharSequence)"+", treeSet);
        String string2 = "/streamsreflowing/fastcal/";
        BakedBandTable bakedBandTable = null;
        int n = -1;
        for (String string3 : new String[]{"", ".v2", ".v3", ".v4", ".v5"}) {
            Loaded loaded = BakedBandTable.a(string2 + string + string3 + ".json", string, unaryOperator);
            if (loaded == null || loaded.spec < n) continue;
            bakedBandTable = loaded.table;
            n = loaded.spec;
        }
        return bakedBandTable;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Loaded a(String string, String string2, UnaryOperator<String> unaryOperator) {
        try (InputStream inputStream = BakedBandTable.class.getResourceAsStream(string);){
            Object object;
            int n;
            int n2;
            Object object2;
            Object object3;
            if (inputStream == null) {
                Loaded loaded = null;
                return loaded;
            }
            JsonObject jsonObject = JsonParser.parseReader((Reader)new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
            int n3 = 0;
            if (jsonObject.has("requires")) {
                object3 = jsonObject.getAsJsonObject("requires").entrySet().iterator();
                while (object3.hasNext()) {
                    Map.Entry entry = (Map.Entry)object3.next();
                    object2 = (String)unaryOperator.apply((String)entry.getKey());
                    if (object2 == null || !BakedBandTable.a((String)object2, ((JsonElement)entry.getValue()).getAsString())) {
                        Loaded loaded = null;
                        return loaded;
                    }
                    ++n3;
                }
            }
            if ((n2 = ((JsonArray)(object3 = jsonObject.getAsJsonArray("bands"))).size()) == 0) {
                object2 = null;
                return object2;
            }
            object2 = new double[n2];
            double[] dArray = new double[n2];
            int[] nArray = new int[n2];
            double[] dArray2 = new double[n2];
            double[] dArray3 = new double[n2];
            for (n = 0; n < n2; ++n) {
                object = ((JsonArray)object3).get(n).getAsJsonObject();
                object2[n] = ((JsonObject)object).get("center").getAsDouble();
                dArray[n] = ((JsonObject)object).get("bias").getAsDouble();
                nArray[n] = ((JsonObject)object).get("n").getAsInt();
                dArray2[n] = ((JsonObject)object).get("p025").getAsDouble();
                dArray3[n] = ((JsonObject)object).get("p975").getAsDouble();
            }
            for (n = 1; n < n2; ++n) {
                if (!(object2[n] <= object2[n - 1])) continue;
                object = null;
                return object;
            }
            String string3 = jsonObject.has("verdict") ? jsonObject.get("verdict").getAsString() : "ON";
            object = new Loaded(new BakedBandTable(string2, string3, (double[])object2, dArray, nArray, dArray2, dArray3), n3);
            return object;
        }
        catch (IOException | RuntimeException exception) {
            if (!Boolean.getBoolean("streamsreflowing.caldiag")) return null;
            Constants.LOG.warn("baked fastcal table '{}' unreadable -> per-world calibration", (Object)string, (Object)exception);
            return null;
        }
    }

    static boolean a(String string, String string2) {
        String string3 = ">=";
        String string4 = string2.trim();
        for (String string5 : new String[]{">=", "<=", "==", ">", "<"}) {
            if (!string4.startsWith(string5)) continue;
            string3 = string5;
            string4 = string4.substring(string5.length()).trim();
            break;
        }
        int n = BakedBandTable.b(string, string4);
        return switch (string3) {
            case ">=" -> {
                if (n >= 0) {
                    yield true;
                }
                yield false;
            }
            case "<=" -> {
                if (n <= 0) {
                    yield true;
                }
                yield false;
            }
            case ">" -> {
                if (n > 0) {
                    yield true;
                }
                yield false;
            }
            case "<" -> {
                if (n < 0) {
                    yield true;
                }
                yield false;
            }
            default -> n == 0;
        };
    }

    private static int b(String string, String string2) {
        int[] nArray = BakedBandTable.a(string);
        int[] nArray2 = BakedBandTable.a(string2);
        for (int i = 0; i < Math.max(nArray.length, nArray2.length); ++i) {
            int n;
            int n2 = i < nArray.length ? nArray[i] : 0;
            int n3 = n = i < nArray2.length ? nArray2[i] : 0;
            if (n2 == n) continue;
            return Integer.compare(n2, n);
        }
        return 0;
    }

    private static int[] a(String string) {
        String string2 = string.split("\\+")[0].trim();
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        String[] objectArray = string2.split("[^0-9]+");
        int n = objectArray.length;
        for (int i = 0; i < n; ++i) {
            String string3 = objectArray[i];
            if (string3.isEmpty()) continue;
            try {
                arrayList.add(Integer.parseInt(string3));
                continue;
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        int[] nArray = new int[arrayList.size()];
        for (n = 0; n < nArray.length; ++n) {
            nArray[n] = (Integer)arrayList.get(n);
        }
        return nArray;
    }

    public String name() {
        return this.b;
    }

    public String verdict() {
        return this.c;
    }

    public double[] centers() {
        return this.d;
    }

    public double[] bias() {
        return this.e;
    }

    public boolean[] safeForWindow(double d, double d2, int n) {
        boolean[] blArray = new boolean[this.d.length];
        for (int i = 0; i < this.d.length; ++i) {
            blArray[i] = this.f[i] >= n && this.h[i] <= d && this.g[i] >= -d2;
        }
        return blArray;
    }

    private record Loaded(BakedBandTable table, int spec) {
    }
}

