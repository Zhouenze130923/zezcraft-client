package com.zezcraft.client.ui;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class Ui {
    public static final class Doc {
        public final String id, mode, bg;
        public final E root;
        final String zoom;
        float z = 1;
        long t0 = System.currentTimeMillis();
        long last = t0;

        Doc(String id, String json) {
            var o = JsonParser.parseString(json).getAsJsonObject();
            this.id = id;
            this.mode = o.has("mode") ? o.get("mode").getAsString() : "screen";
            this.bg = o.has("backdrop") ? o.get("backdrop").getAsString() : "blur";
            this.zoom = o.has("zoom") ? o.get("zoom").getAsString() : "1";
            if (!o.has("anchor")) o.addProperty("anchor", "c");
            this.root = E.of(o);
        }

        void draw(GuiGraphicsExtractor g, int sw, int sh, int mx, int my, List<String> tip) {
            long now = System.currentTimeMillis();
            float dt = Math.min(100, now - last);
            last = now;
            if (zoom.equals("auto")) {
                float f = Math.min(sh * .86f / Math.max(1, root.h), sw * .9f / Math.max(1, root.w));
                z = f >= 1 ? (float) Math.floor(f * 2) / 2 : f;
            } else z = Float.parseFloat(zoom);
            int vw = Math.round(sw / z), vh = Math.round(sh / z);
            R.layout(root, 0, 0, vw, vh);
            var ps = g.pose();
            ps.pushMatrix();
            ps.scale(z, z);
            R.draw(g, root, now - t0, mx < 0 ? mx : Math.round(mx / z), my < 0 ? my : Math.round(my / z), 1, dt, tip);
            ps.popMatrix();
        }
    }

    public static final Map<String, Doc> hud = new LinkedHashMap<>();
    public static BiConsumer<String, String> onClick = (a, b) -> { };
    public static java.util.function.Consumer<String> onClose = a -> { };

    public static void open(String id, String json) {
        Doc d = new Doc(id, json);
        if (d.mode.equals("hud")) { hud.put(id, d); return; }
        var cur = Minecraft.getInstance().screen;
        if (cur instanceof Scr z && z.d.id.equals(id)) { z.swap(d); return; }
        Scr n = new Scr(d);
        d.root.each(e -> { if (e.focus && e.type.equals("input")) n.focus = e; });
        Minecraft.getInstance().setScreen(n);
    }

    public static void close(String id) {
        hud.remove(id);
        var s = Minecraft.getInstance().screen;
        if (s instanceof Scr z && z.d.id.equals(id)) { z.silent = true; z.onClose(); }
    }

    public static void patch(String id, String elem, String k, String v) {
        Doc d = hud.get(id);
        var s = Minecraft.getInstance().screen;
        if (d == null && s instanceof Scr z && z.d.id.equals(id)) d = z.d;
        if (d == null) return;
        E e = d.root.find(elem);
        if (e != null) e.set(k, v);
    }

    public static void drawHud(GuiGraphicsExtractor g) {
        if (hud.isEmpty()) return;
        List<String> tip = new ArrayList<>();
        for (Doc d : hud.values()) d.draw(g, g.guiWidth(), g.guiHeight(), -1, -1, tip);
    }

    static final class Scr extends Screen {
        Doc d;
        boolean silent;
        E focus;

        void swap(Doc n) {
            Doc o = d;
            n.root.anim = "";
            n.z = o.z;
            n.root.each(e -> {
                if (e.id.isEmpty()) return;
                E p = o.root.find(e.id);
                if (p == null) return;
                if (e.type.equals("input")) { e.val = p.val; e.focus = p.focus; }
                if (e.scroll) {
                    int max = Math.max(0, R.contentH(p) - p.h + 4);
                    if (e.bottom && p.syT >= max - 2) e.stuck = true;
                    else { e.stuck = false; e.sy = p.sy; e.syT = p.syT; }
                }
                e.hover = p.hover; e.wasOver = p.wasOver;
            });
            E was = focus;
            focus = null;
            n.root.each(e -> { if (e.focus && e.type.equals("input")) focus = e; });
            d = n;
            if ((was == null) != (focus == null)) { ime = null; minecraft.onTextInputFocusChange(this, focus != null); }
        }

        String fill(String a) {
            if (a.indexOf('{') < 0) return a;
            StringBuilder out = new StringBuilder();
            int i = 0;
            while (i < a.length()) {
                int b = a.indexOf('{', i), c = b < 0 ? -1 : a.indexOf('}', b);
                if (b < 0 || c < 0) { out.append(a, i, a.length()); break; }
                out.append(a, i, b);
                E in = d.root.find(a.substring(b + 1, c));
                out.append(in == null ? "" : in.val);
                i = c + 1;
            }
            return out.toString();
        }

        void clearUsed(String a) {
            d.root.each(e -> { if (e.type.equals("input") && e.clear && a.contains("{" + e.id + "}")) e.val = ""; });
        }

        void fire(E e) {
            String raw = e.action.isEmpty() ? e.id : e.action;
            if (raw.isEmpty()) return;
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            onClick.accept(d.id, fill(raw));
            clearUsed(raw);
            if (raw.equals("close")) onClose();
        }

        net.minecraft.client.gui.components.IMEPreeditOverlay ime;

        void setFocus(E e) {
            if (focus != null) focus.focus = false;
            focus = e;
            if (e != null) e.focus = true;
            ime = null;
            if (minecraft != null) minecraft.onTextInputFocusChange(this, e != null);
        }

        @Override
        protected void init() {
            super.init();
            if (focus != null) minecraft.onTextInputFocusChange(this, true);
        }

        @Override
        public boolean preeditUpdated(net.minecraft.client.input.PreeditEvent ev) {
            if (focus == null) return false;
            ime = ev == null || ev.fullText().isEmpty() ? null : new net.minecraft.client.gui.components.IMEPreeditOverlay(ev, font, 9);
            return true;
        }

        @Override
        public boolean charTyped(net.minecraft.client.input.CharacterEvent ev) {
            if (focus != null && ev.isAllowedChatCharacter()) {
                if (focus.max <= 0 || focus.val.codePointCount(0, focus.val.length()) < focus.max) focus.val += ev.codepointAsString();
                return true;
            }
            return super.charTyped(ev);
        }

        @Override
        public boolean keyPressed(KeyEvent ev) {
            if (focus != null) {
                int k = ev.key();
                if (ime != null && (k == 259 || k == 257 || k == 335)) return true;
                if (k == 259) {
                    String v = focus.val;
                    if (ev.hasControlDown()) focus.val = "";
                    else if (!v.isEmpty()) focus.val = v.substring(0, v.offsetByCodePoints(v.length(), -1));
                    return true;
                }
                if (k == 257 || k == 335) { if (!focus.action.isEmpty()) fire(focus); return true; }
                if (k == 86 && ev.hasControlDown()) {
                    String c = minecraft.keyboardHandler.getClipboard().replace("\n", " ").replace("\r", "");
                    for (int i = 0; i < c.length(); ) {
                        int cp = c.codePointAt(i); i += Character.charCount(cp);
                        if (focus.max > 0 && focus.val.codePointCount(0, focus.val.length()) >= focus.max) break;
                        if (cp >= 32 && cp != 167 && cp != 127) focus.val += new String(Character.toChars(cp));
                    }
                    return true;
                }
                if (k == 256) { setFocus(null); return true; }
                if (k != 258) return true;
            }
            return super.keyPressed(ev);
        }
        final List<String> tip = new ArrayList<>();

        Scr(Doc d) { super(Component.literal(d.id)); this.d = d; }

        @Override public boolean isPauseScreen() { return false; }

        @Override
        public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
            if (d.bg.equals("blur")) super.extractBackground(g, mx, my, pt);
            else if (d.bg.equals("dim")) g.fillGradient(0, 0, width, height, 0x90000000, 0xB0000000);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
            super.extractRenderState(g, mx, my, pt);
            tip.clear();
            d.draw(g, width, height, mx, my, tip);
            if (ime != null && focus != null) {
                ime.updateInputPosition(Math.round(focus.cx * d.z), Math.round(focus.cy * d.z));
                ime.extractRenderState(g, mx, my, pt);
            }
            if (!tip.isEmpty()) {
                List<Component> l = new ArrayList<>();
                for (String s : tip) l.add(Component.literal(s));
                g.setComponentTooltipForNextFrame(font, l, mx, my, null);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent ev, boolean dbl) {
            E e = R.hit(d.root, (int) Math.round(ev.x() / d.z), (int) Math.round(ev.y() / d.z));
            if (e != null && e.type.equals("input")) { setFocus(e); return true; }
            if (e != null && e.type.equals("button") && !e.disabled) { fire(e); return true; }
            if (e == null) setFocus(null);
            return super.mouseClicked(ev, dbl);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double dx, double dy) {
            E sc = R.scroller(d.root, (int) Math.round(mx / d.z), (int) Math.round(my / d.z));
            if (sc != null) { R.wheel(sc, dy); return true; }
            return super.mouseScrolled(mx, my, dx, dy);
        }

        @Override
        public void removed() {
            if (focus != null) minecraft.onTextInputFocusChange(this, false);
            if (!silent) onClose.accept(d.id);
        }
    }
}
