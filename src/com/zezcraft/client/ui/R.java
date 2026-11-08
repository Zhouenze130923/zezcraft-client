package com.zezcraft.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class R {
    static final Identifier BTN = Identifier.withDefaultNamespace("widget/button");
    static final Identifier BTN_H = Identifier.withDefaultNamespace("widget/button_highlighted");
    static final Identifier BTN_D = Identifier.withDefaultNamespace("widget/button_disabled");
    static final Identifier TIP_BG = Identifier.withDefaultNamespace("tooltip/background");
    static final Identifier TIP_FR = Identifier.withDefaultNamespace("tooltip/frame");

    static float clamp(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    static float ease(String e, float k) {
        return switch (e) {
            case "back" -> { float c = 1.70158f, c3 = c + 1; yield 1 + c3 * (float) Math.pow(k - 1, 3) + c * (float) Math.pow(k - 1, 2); }
            case "linear" -> k;
            case "elastic" -> k == 0 || k == 1 ? k : (float) (Math.pow(2, -10 * k) * Math.sin((k * 10 - .75) * (2 * Math.PI / 3)) + 1);
            default -> 1 - (float) Math.pow(1 - k, 3);
        };
    }

    static int alpha(int c, float a) {
        int al = Math.round(((c >>> 24) & 0xFF) * clamp(a));
        return (al << 24) | (c & 0xFFFFFF);
    }

    static int mixc(int a, int b, float k) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int x = (a >>> s) & 0xFF, y = (b >>> s) & 0xFF;
            r |= (Math.round(x + (y - x) * k) & 0xFF) << s;
        }
        return r;
    }

    static List<String> wrap(Font f, String txt, int bw) {
        List<String> out = new ArrayList<>();
        for (String l : txt.split("\n", -1)) {
            if (bw > 0 && f.width(l) > bw) for (var s : f.split(Component.literal(l), bw)) {
                StringBuilder sb = new StringBuilder();
                s.accept((i, st, cp) -> { sb.appendCodePoint(cp); return true; });
                out.add(sb.toString());
            } else out.add(l);
        }
        return out;
    }

    static void measure(E k) {
        Font f = Minecraft.getInstance().font;
        if (k.type.equals("text") && k.h0 == 0) {
            int bw = k.w <= 0 ? 0 : Math.round(k.w / k.scale);
            List<String> ls = wrap(f, k.text, bw);
            k.h = Math.round(ls.size() * 10 * k.scale);
        } else if (k.fit) {
            int mw = 0, mh = 0;
            for (E c : k.kids) {
                if (!c.visible) continue;
                measure(c);
                int cw = c.w;
                if (c.type.equals("text")) {
                    int bw = c.w <= 0 ? 0 : Math.round(c.w / c.scale), tw = 0;
                    for (String l : wrap(f, c.text, bw)) tw = Math.max(tw, f.width(l));
                    cw = Math.round(tw * c.scale);
                }
                mw = Math.max(mw, c.x + cw);
                mh = Math.max(mh, c.y + c.h);
            }
            k.w = k.w0 > 0 ? Math.min(k.w0, mw + k.pad) : mw + k.pad;
            k.h = mh + k.pad;
        }
    }

    public static void layout(E e, int px, int py, int pw, int ph) {
        if (e.flow) {
            int cy = e.pad;
            for (E k : e.kids) {
                if (!k.visible) continue;
                measure(k);
                k.y = cy;
                cy += k.h + e.gap;
            }
        }
        int ox = switch (e.anchor.length() > 1 ? e.anchor.charAt(1) : 'l') { case 'c' -> (pw - e.w) / 2; case 'r' -> pw - e.w; default -> 0; };
        int oy = switch (e.anchor.charAt(0)) { case 'c' -> (ph - e.h) / 2; case 'b' -> ph - e.h; default -> 0; };
        if (e.anchor.equals("c")) { ox = (pw - e.w) / 2; oy = (ph - e.h) / 2; }
        e.ax = px + ox + e.x;
        e.ay = py + oy + e.y;
        int off = e.scroll ? Math.round(e.sy) : 0;
        for (E k : e.kids) layout(k, e.ax, e.ay - off, e.w, e.h);
    }

    public static E scroller(E e, int mx, int my) {
        if (!e.visible) return null;
        for (int i = e.kids.size() - 1; i >= 0; i--) { E f = scroller(e.kids.get(i), mx, my); if (f != null) return f; }
        return e.scroll && mx >= e.ax && my >= e.ay && mx < e.ax + e.w && my < e.ay + e.h ? e : null;
    }

    static int contentH(E e) {
        int m = 0;
        for (E k : e.kids) if (k.visible) m = Math.max(m, k.y + k.h);
        return m;
    }

    public static void wheel(E e, double d) {
        int max = Math.max(0, contentH(e) - e.h + 4);
        e.syT = Math.max(0, Math.min(max, e.syT - (float) d * 24));
    }

    public static E hit(E e, int mx, int my) {
        if (!e.visible) return null;
        if (e.scroll && !(mx >= e.ax && my >= e.ay && mx < e.ax + e.w && my < e.ay + e.h)) return null;
        for (int i = e.kids.size() - 1; i >= 0; i--) {
            E f = hit(e.kids.get(i), mx, my);
            if (f != null) return f;
        }
        boolean in = mx >= e.ax && my >= e.ay && mx < e.ax + e.w && my < e.ay + e.h;
        return in && (e.type.equals("button") || e.type.equals("input") || e.modal || !e.tip.isEmpty()) ? e : null;
    }

    public static void draw(GuiGraphicsExtractor g, E e, long el, int mx, int my, float a, float dt, List<String> tip) {
        if (!e.visible) return;
        float k = e.anim.isEmpty() ? 1 : clamp((el - e.delay) / (float) Math.max(1, e.dur));
        if (k <= 0 && !e.anim.isEmpty()) return;
        float q = ease(e.anim.equals("pop") ? "back" : e.ease, k);
        float al = a * (e.anim.isEmpty() ? 1 : Math.min(1, k * 1.6f));
        float dx = 0, dy = 0, sc = 1;
        switch (e.anim) {
            case "up" -> dy = (1 - q) * 14;
            case "down" -> dy = -(1 - q) * 14;
            case "left" -> dx = (1 - q) * 24;
            case "right" -> dx = -(1 - q) * 24;
            case "scale", "pop" -> sc = .82f + .18f * q;
            default -> { }
        }
        boolean over = mx >= e.ax && my >= e.ay && mx < e.ax + e.w && my < e.ay + e.h;
        if (e.type.equals("button") && !e.disabled) {
            e.hover = clamp(e.hover + (over ? dt : -dt) / 120f);
            if (over && !e.wasOver) Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.9f, .12f));
            e.wasOver = over;
        }
        if (e.flash) {
            long fe = System.currentTimeMillis() - e.flashAt;
            if (fe > 280) return;
            al *= 1 - fe / 280f;
            sc *= 1.25f - .25f * ease("out", Math.min(1, fe / 120f));
        }
        if (e.life > 0) {
            long lt = System.currentTimeMillis() - e.popAt;
            if (lt > e.life) return;
            float fi = clamp(lt / 160f), fo = clamp((e.life - lt) / 380f);
            al *= Math.min(fi, fo);
            dy += (1 - ease("back", fi)) * 10 - (1 - fo) * 6;
        }
        if (e.pulse) al *= .55f + .45f * (float) (Math.sin(System.currentTimeMillis() / 260.0) * .5 + .5);
        float hs = e.style.equals("ghost") || e.style.equals("card") ? 1 : 1 + .04f * ease("out", e.hover);
        sc *= hs;
        var ps = g.pose();
        ps.pushMatrix();
        float cx = e.ax + e.w / 2f, cy = e.ay + e.h / 2f;
        ps.translate(cx + dx, cy + dy - (e.style.equals("ghost") ? 0 : e.style.equals("card") ? 3 * ease("out", e.hover) : e.hover));
        ps.scale(sc, sc);
        ps.translate(-cx, -cy);
        Font f = Minecraft.getInstance().font;
        switch (e.type) {
            case "panel" -> panel(g, e, al);
            case "text" -> text(g, f, e, al);
            case "image" -> image(g, e, al);
            case "button" -> button(g, f, e, al);
            case "bar" -> bar(g, e, al, dt);
            case "badge" -> badge(g, f, e, al);
            case "pips" -> pips(g, e, al);
            case "hitmark" -> hitmark(g, e, al);
            case "item" -> item(g, e);
            case "face" -> face(g, e, al);
            case "input" -> input(g, f, e, al);
            default -> { }
        }
        if (over && !e.tip.isEmpty()) { tip.clear(); tip.addAll(e.tip); }
        if (e.scroll) {
            if (e.stuck) { e.syT = e.sy = Math.max(0, contentH(e) - e.h + 4); e.stuck = false; }
            e.sy += (e.syT - e.sy) * clamp(dt / 90f);
            g.enableScissor(e.ax, e.ay, e.ax + e.w, e.ay + e.h);
            boolean in = mx >= e.ax && my >= e.ay && mx < e.ax + e.w && my < e.ay + e.h;
            for (E c : e.kids) draw(g, c, el, in ? mx : -9999, in ? my : -9999, al, dt, tip);
            g.disableScissor();
            int ch = contentH(e);
            if (ch > e.h) {
                int th = Math.max(10, e.h * e.h / ch), ty = e.ay + Math.round((e.h - th) * (e.sy / Math.max(1, ch - e.h + 4)));
                g.fill(e.ax + e.w - 2, e.ay, e.ax + e.w, e.ay + e.h, alpha(0x30000000, al));
                g.fill(e.ax + e.w - 2, ty, e.ax + e.w, ty + th, alpha(0xA0FFFFFF, al));
            }
        } else {
            int m = -1;
            for (int i = 0; i < e.kids.size(); i++) if (e.kids.get(i).modal && e.kids.get(i).visible) m = i;
            for (int i = 0; i < e.kids.size(); i++) draw(g, e.kids.get(i), el, i < m ? -9999 : mx, i < m ? -9999 : my, al, dt, tip);
        }
        ps.popMatrix();
    }

    static void panel(GuiGraphicsExtractor g, E e, float a) {
        int x = e.ax, y = e.ay, w = e.w, h = e.h;
        switch (e.style) {
            case "none" -> { }
            case "dark" -> {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, TIP_BG, x - 9, y - 9, w + 18, h + 18, alpha(0xFFFFFFFF, a));
                g.blitSprite(RenderPipelines.GUI_TEXTURED, TIP_FR, x - 9, y - 9, w + 18, h + 18, alpha(0xFFFFFFFF, a));
            }
            case "flat" -> {
                g.fill(x, y, x + w, y + h, alpha(e.bg == 0 ? 0xC0101014 : e.bg, a));
                if (e.border != 0) {
                    int b = alpha(e.border, a);
                    g.fill(x, y, x + w, y + 1, b); g.fill(x, y + h - 1, x + w, y + h, b);
                    g.fill(x, y, x + 1, y + h, b); g.fill(x + w - 1, y, x + w, y + h, b);
                }
            }
            case "slot" -> {
                g.fill(x, y, x + w, y + h, alpha(0xFF8B8B8B, a));
                g.fill(x, y, x + w - 1, y + 1, alpha(0xFF373737, a)); g.fill(x, y, x + 1, y + h - 1, alpha(0xFF373737, a));
                g.fill(x + 1, y + h - 1, x + w, y + h, alpha(0xFFFFFFFF, a)); g.fill(x + w - 1, y + 1, x + w, y + h, alpha(0xFFFFFFFF, a));
            }
            default -> {
                int bg = alpha(e.bg == 0 ? 0xFFC6C6C6 : e.bg, a), blk = alpha(0xFF000000, a), hi = alpha(0xFFFFFFFF, a), lo = alpha(0xFF555555, a);
                g.fill(x + 1, y, x + w - 1, y + 1, blk); g.fill(x + 1, y + h - 1, x + w - 1, y + h, blk);
                g.fill(x, y + 1, x + 1, y + h - 1, blk); g.fill(x + w - 1, y + 1, x + w, y + h - 1, blk);
                g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
                g.fill(x + 1, y + 1, x + w - 3, y + 3, hi); g.fill(x + 1, y + 1, x + 3, y + h - 3, hi);
                g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, lo); g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, lo);
                g.fill(x + 2, y + 2, x + 3, y + 3, bg); g.fill(x + w - 3, y + h - 3, x + w - 2, y + h - 2, bg);
            }
        }
    }

    static void text(GuiGraphicsExtractor g, Font f, E e, float a) {
        String[] lines = e.text.split("\n");
        var ps = g.pose();
        ps.pushMatrix();
        ps.translate(e.ax, e.ay);
        ps.scale(e.scale, e.scale);
        int bw = e.w <= 0 ? 0 : Math.round(e.w / e.scale);
        int ly = 0;
        List<String> out = wrap(f, e.text, bw);
        for (String l : out) {
            int tx = switch (e.align) { case "center" -> (bw - f.width(l)) / 2; case "right" -> bw - f.width(l); default -> 0; };
            g.text(f, l, tx, ly, alpha(e.color, a), e.shadow);
            ly += 10;
        }
        ps.popMatrix();
    }

    static void image(GuiGraphicsExtractor g, E e, float a) {
        if (e.tex.isEmpty()) return;
        int fr = e.frames > 1 ? (int) ((System.currentTimeMillis() / e.ft) % e.frames) : 0;
        g.blit(RenderPipelines.GUI_TEXTURED, Identifier.parse(e.tex), e.ax, e.ay, e.u, e.v + fr * e.vh, e.w, e.h, e.uw, e.vh, e.tw, e.th, alpha(e.color, a));
    }

    static void pips(GuiGraphicsExtractor g, E e, float a) {
        int n = Math.max(1, e.count), filled = Math.round(e.value);
        if (n > 40) { e.shown = e.value / n; int fw = Math.round(e.w * Math.min(1, e.value / n)); g.fill(e.ax, e.ay, e.ax + e.w, e.ay + e.h, alpha(0x40FFFFFF, a)); g.fill(e.ax, e.ay, e.ax + fw, e.ay + e.h, alpha(e.color, a)); return; }
        float step = e.w / (float) n;
        int pw = Math.max(1, Math.round(step) - 1);
        int warn = filled <= n / 4 ? (filled == 0 ? 0xFFFF5555 : 0xFFFFAA33) : e.color;
        for (int i = 0; i < n; i++) {
            int x = e.ax + Math.round(i * step);
            g.fill(x, e.ay, x + pw, e.ay + e.h, alpha(i < filled ? warn : 0x40FFFFFF, a));
            if (i < filled) g.fill(x, e.ay, x + pw, e.ay + 1, alpha(0x80FFFFFF, a));
        }
    }

    static void hitmark(GuiGraphicsExtractor g, E e, float a) {
        int cx = e.ax + e.w / 2, cy = e.ay + e.h / 2, c = alpha(e.color, a);
        for (int i = 2; i <= e.w / 2; i++) {
            g.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, c); g.fill(cx - i, cy + i, cx - i + 1, cy + i + 1, c);
            g.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, c); g.fill(cx - i, cy - i, cx - i + 1, cy - i + 1, c);
        }
    }

    static void badge(GuiGraphicsExtractor g, Font f, E e, float a) {
        int tw = f.width(e.text), w = tw + 8, h = 11;
        int x = e.align.equals("center") ? e.ax + (e.w - w) / 2 : e.align.equals("right") ? e.ax + e.w - w : e.ax, y = e.ay;
        int bg = alpha(e.bg == 0 ? 0xC0101014 : e.bg, a);
        g.fill(x + 1, y, x + w - 1, y + h, bg); g.fill(x, y + 1, x + w, y + h - 1, bg);
        if (e.border != 0) { int b = alpha(e.border, a); g.fill(x + 1, y + h - 1, x + w - 1, y + h, b); }
        g.text(f, e.text, x + 4, y + 2, alpha(e.color, a), true);
    }

    static void card(GuiGraphicsExtractor g, Font f, E e, float a) {
        int x = e.ax, y = e.ay, w = e.w, h = e.h;
        float k = ease("out", e.hover);
        int sh = alpha(0x90000000, a * (.35f + .65f * k));
        g.fill(x + 2, y + h, x + w + 1, y + h + 2 + Math.round(2 * k), sh);
        g.fill(x + w, y + 2, x + w + 1 + Math.round(k), y + h, sh);
        image(g, e, a);
        if (k > 0) {
            int gc = alpha(e.glow, a * k);
            g.fill(x - 1, y - 1, x + w + 1, y, gc); g.fill(x - 1, y + h, x + w + 1, y + h + 1, gc);
            g.fill(x - 1, y, x, y + h, gc); g.fill(x + w, y, x + w + 1, y + h, gc);
            g.enableScissor(x, y, x + w, y + h);
            long t = System.currentTimeMillis() % 1400;
            int sx = x - h + (int) ((w + 2 * h) * (t / 1400f));
            for (int i = 0; i < 6; i++) {
                int c = alpha(0xFFFFFFFF, a * k * (i == 2 || i == 3 ? .22f : .1f));
                for (int yy = 0; yy < h; yy += 2) g.fill(sx + i + yy / 2, y + yy, sx + i + yy / 2 + 1, y + yy + 2, c);
            }
            g.disableScissor();
        }
        if (!e.text.isEmpty()) g.text(f, e.text, x + (w - f.width(e.text)) / 2, y + h - 11, alpha(e.color, a), true);
    }

    static void button(GuiGraphicsExtractor g, Font f, E e, float a) {
        int x = e.ax, y = e.ay;
        if (e.style.equals("card")) { card(g, f, e, a); return; }
        if (e.style.equals("ghost")) {
            int c0 = e.bg == 0 ? 0x60FFFFFF : e.bg;
            if (e.hover > 0) {
                int c1 = alpha(c0, a * e.hover);
                g.fill(x, y, x + e.w, y + e.h, alpha(c1, .35f));
                g.fill(x, y, x + e.w, y + 1, c1); g.fill(x, y + e.h - 1, x + e.w, y + e.h, c1);
                g.fill(x, y, x + 1, y + e.h, c1); g.fill(x + e.w - 1, y, x + e.w, y + e.h, c1);
            }
            if (!e.text.isEmpty()) g.text(f, e.text, x + (e.w - f.width(e.text)) / 2, y + (e.h - 8) / 2, alpha(e.color, a), true);
            return;
        }
        if (e.style.equals("flat")) {
            int base = e.bg == 0 ? 0xCC202028 : e.bg;
            float hv = e.sel ? Math.max(.7f, e.hover) : e.hover;
            g.fill(x, y, x + e.w, y + e.h, alpha(mixc(base, 0xFF3A3A48, hv), a));
            int ac = e.border == 0 ? 0xFF6FD06F : e.border;
            g.fill(x, y + e.h - 1, x + Math.round(e.w * (.15f + .85f * (e.sel ? 1 : e.hover))), y + e.h, alpha(ac, a));
            if (e.sel) g.fill(x, y, x + 1, y + e.h - 1, alpha(ac, a));
        } else {
            Identifier s = e.disabled ? BTN_D : e.hover > .5f ? BTN_H : BTN;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, s, x, y, e.w, e.h, alpha(0xFFFFFFFF, a));
        }
        int c = e.disabled ? 0xFFA0A0A0 : mixc(e.color, 0xFFFFFFA0, e.hover * .5f);
        if (!e.text.isEmpty()) g.text(f, e.text, x + (e.w - f.width(e.text)) / 2, y + (e.h - 8) / 2, alpha(c, a), true);
    }

    static void bar(GuiGraphicsExtractor g, E e, float a, float dt) {
        e.shown += (e.value - e.shown) * clamp(dt / 120f);
        int x = e.ax, y = e.ay;
        g.fill(x, y, x + e.w, y + e.h, alpha(e.bg == 0 ? 0xFF1A1A1A : e.bg, a));
        int fw = Math.round((e.w - 2) * clamp(e.shown));
        g.fill(x + 1, y + 1, x + 1 + fw, y + e.h - 1, alpha(e.color, a));
        g.fill(x + 1, y + 1, x + 1 + fw, y + 2, alpha(0x55FFFFFF, a));
    }

    static void item(GuiGraphicsExtractor g, E e) {
        if (e.item.isEmpty()) return;
        if (e.stack == null) {
            var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.parse(e.item));
            var st = new net.minecraft.world.item.ItemStack(it);
            if (e.glint) st.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            e.stack = st;
        }
        int sz = e.w <= 0 ? 16 : e.w;
        var ps = g.pose();
        ps.pushMatrix();
        ps.translate(e.ax, e.ay);
        ps.scale(sz / 16f, sz / 16f);
        g.item((net.minecraft.world.item.ItemStack) e.stack, 0, 0);
        ps.popMatrix();
    }

    static void face(GuiGraphicsExtractor g, E e, float a) {
        var mc = Minecraft.getInstance();
        net.minecraft.world.entity.player.PlayerSkin skin = null;
        if (mc.getConnection() != null) {
            var pi = mc.getConnection().getPlayerInfoIgnoreCase(e.text);
            if (pi != null) skin = pi.getSkin();
        }
        if (skin == null) skin = net.minecraft.client.resources.DefaultPlayerSkin.get(java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + e.text).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        int sz = e.w <= 0 ? 8 : e.w;
        if (e.border != 0) { int b = alpha(e.border, a); g.fill(e.ax - 1, e.ay - 1, e.ax + sz + 1, e.ay + sz + 1, b); }
        net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(g, skin, e.ax, e.ay, sz, alpha(e.disabled ? 0xFF707070 : 0xFFFFFFFF, a));
    }

    static void input(GuiGraphicsExtractor g, Font f, E e, float a) {
        int x = e.ax, y = e.ay, w = e.w, h = e.h <= 0 ? 16 : e.h;
        g.fill(x, y, x + w, y + h, alpha(e.focus ? 0xFFE0E0E0 : 0xFF808080, a));
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, alpha(e.bg == 0 ? 0xFF000000 : e.bg, a));
        String s = e.val;
        boolean ph = s.isEmpty();
        String shown = ph ? e.text : s;
        int room = w - 10;
        while (!ph && f.width(shown) > room && shown.length() > 0) shown = shown.substring(1);
        g.enableScissor(x + 2, y, x + w - 2, y + h);
        int ty = y + (h - 8) / 2;
        g.text(f, shown, x + 4, ty, alpha(ph ? 0xFF6A6A6A : e.color, a), !ph);
        if (e.focus && (System.currentTimeMillis() / 450) % 2 == 0) {
            int cx = x + 4 + (ph ? 0 : f.width(shown));
            g.fill(cx, ty - 1, cx + 1, ty + 9, alpha(0xFFD0D0D0, a));
        }
        g.disableScissor();
        if (e.max > 0 && e.focus && !ph) {
            String c = s.codePointCount(0, s.length()) + "/" + e.max;
            g.text(f, c, x + w - 3 - f.width(c), y + h + 2, alpha(0xFF8A8A8A, a * .8f), false);
        }
    }
}
