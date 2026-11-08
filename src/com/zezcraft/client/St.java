package com.zezcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class St {
    public static final class Bind {
        public int flags, own, aim = -1, aimMs = 150, run = -1, runMs = 120;
        float aimV, runV;
        long last;
        final Map<Integer, Tl> tl = new HashMap<>();
    }

    static final class Tl {
        long t0;
        int[] ms;
        float[] v;
        boolean lin;
        float end;

        float at(long now) {
            long e = now - t0;
            if (e >= ms[ms.length - 1]) return Float.NaN;
            int i = 0;
            while (i + 1 < ms.length && ms[i + 1] <= e) i++;
            if (!lin || i + 1 >= ms.length) return v[i];
            float k = (e - ms[i]) / (float) Math.max(1, ms[i + 1] - ms[i]);
            return v[i] + (v[i + 1] - v[i]) * k;
        }

        boolean active(long now) { return now - t0 < ms[ms.length - 1]; }
    }

    record Hud(int anchor, long until, String text) {}

    public static final Map<String, Bind> binds = new HashMap<>();
    static final Map<String, Hud> hud = new LinkedHashMap<>();
    static boolean server, all;
    static final java.util.Set<Integer> sub = new java.util.HashSet<>();

    static void reset() {
        binds.clear(); hud.clear(); sub.clear(); all = false; server = false;
    }

    public static String model(ItemStack s) {
        if (s == null || s.isEmpty()) return null;
        Identifier id = s.get(DataComponents.ITEM_MODEL);
        return id == null ? null : id.toString();
    }

    public static Bind bind(ItemStack s) {
        String m = model(s);
        return m == null ? null : binds.get(m);
    }

    public static Bind held() {
        var p = Minecraft.getInstance().player;
        return p == null ? null : bind(p.getMainHandItem());
    }

    static float approach(float v, float t, float dt, int ms) {
        float step = ms <= 0 ? 1 : dt / ms;
        return v < t ? Math.min(t, v + step) : Math.max(t, v - step);
    }

    public static float value(Bind b, int idx, float orig) {
        long now = System.currentTimeMillis();
        if (b.last != now) {
            float dt = b.last == 0 ? 0 : Math.min(200, now - b.last);
            b.last = now;
            Minecraft mc = Minecraft.getInstance();
            boolean free = mc.screen == null;
            Tl r = b.tl.get(0);
            boolean busy = r != null && r.active(now);
            boolean aiming = free && !busy && Input.down(-2);
            b.aimV = approach(b.aimV, aiming ? 1 : 0, dt, b.aimMs);
            boolean running = mc.player != null && mc.player.isSprinting() && !aiming && !busy;
            b.runV = approach(b.runV, running ? 1 : 0, dt, b.runMs);
        }
        Tl t = b.tl.get(idx);
        if (t != null) {
            float v = t.at(now);
            if (!Float.isNaN(v)) return v;
            if (!Float.isNaN(t.end) && t.end != 0) return t.end;
        }
        if (idx == b.aim) return b.aimV < .01f ? 0 : b.aimV;
        if (idx == b.run) return b.runV < .01f ? 0 : b.runV;
        if ((b.own >> idx & 1) != 0) return 0;
        return orig;
    }
}
