package com.zezcraft.client;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

final class Input {
    static final Map<Integer, Long> held = new HashMap<>();

    static java.lang.invoke.MethodHandle mb;

    static boolean raw(int code) {
        var w = Minecraft.getInstance().getWindow();
        if (code >= 0) return com.mojang.blaze3d.platform.InputConstants.isKeyDown(w, code);
        try {
            if (mb == null) mb = java.lang.invoke.MethodHandles.publicLookup().findStatic(Class.forName("org.lwjgl.glfw.GLFW"), "glfwGetMouseButton",
                    java.lang.invoke.MethodType.methodType(int.class, long.class, int.class));
            return (int) mb.invokeExact(w.handle(), -code - 1) == 1;
        } catch (Throwable t) {
            var o = Minecraft.getInstance().options;
            return code == -1 ? o.keyAttack.isDown() : code == -2 ? o.keyUse.isDown() : code == -3 && o.keyPickItem.isDown();
        }
    }

    static boolean down(int code) {
        return held.containsKey(code);
    }

    static void poll(boolean free) {
        long now = System.currentTimeMillis();
        java.util.Set<Integer> codes = new java.util.HashSet<>(St.sub);
        codes.add(-1); codes.add(-2);
        if (St.all) {
            for (int k = 32; k <= 348; k++) codes.add(k);
            for (int b = 1; b <= 8; b++) codes.add(-b);
        }
        codes.addAll(held.keySet());
        for (int c : codes) {
            boolean d = free && raw(c);
            Long since = held.get(c);
            if (d && since == null) {
                held.put(c, now);
                if (report(c)) Net.key(c, true, 0);
            } else if (!d && since != null) {
                held.remove(c);
                if (report(c)) Net.key(c, false, (int) (now - since));
            }
        }
    }

    static boolean report(int c) {
        return St.server && (St.all || St.sub.contains(c));
    }
}
