package com.zezcraft.client.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class E {
    public String type = "panel", id = "", text = "", style = "vanilla", action = "", tex = "", align = "left", anchor = "tl", anim = "", ease = "out";
    public int x, y, w, h, color = 0xFFFFFFFF, bg = 0, border = 0, tw = 16, th = 16, u, v, uw, vh, frames = 1, ft = 100, dur = 220, delay;
    public float scale = 1, value, shown;
    public boolean shadow = true, visible = true, disabled;
    public List<String> tip = new ArrayList<>();
    public final List<E> kids = new ArrayList<>();
    public float hover;
    public boolean pulse, wasOver, flash;
    public long flashAt;
    public int count;
    public boolean scroll;
    public float sy, syT;
    public int glow;
    public int ax, ay;
    public String val = "", item = "";
    public int max = 64, gap = 2, pad = 4, life;
    public boolean modal, focus, flow, fit, sel, bottom, stuck, clear, glint;
    public long popAt;
    Object stack;
    public int w0, h0;

    static int col(JsonObject o, String k, int d) {
        if (!o.has(k)) return d;
        String s = o.get(k).getAsString().replace("#", "");
        long v = Long.parseLong(s, 16);
        if (s.length() <= 6) v |= 0xFF000000L;
        return (int) v;
    }

    static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }

    static boolean bool(JsonObject o, String k) { return o.has(k) && o.get(k).getAsBoolean(); }

    public void each(java.util.function.Consumer<E> c) { c.accept(this); for (E k : kids) k.each(c); }

    static int num(JsonObject o, String k, int d) { return o.has(k) ? o.get(k).getAsInt() : d; }

    public static E of(JsonObject o) {
        E e = new E();
        e.type = str(o, "type", "panel");
        e.id = str(o, "id", "");
        e.text = str(o, "text", "").replace('&', '§');
        e.style = str(o, "style", e.type.equals("button") ? "vanilla" : "vanilla");
        e.action = str(o, "action", "");
        e.tex = str(o, "tex", "");
        e.align = str(o, "align", "left");
        e.anchor = str(o, "anchor", "tl");
        e.anim = str(o, "anim", "");
        e.ease = str(o, "ease", "out");
        e.x = num(o, "x", 0); e.y = num(o, "y", 0); e.w = num(o, "w", 0); e.h = num(o, "h", 0);
        e.color = col(o, "color", 0xFFFFFFFF); e.bg = col(o, "bg", 0); e.border = col(o, "border", 0);
        e.tw = num(o, "tw", 16); e.th = num(o, "th", 16); e.u = num(o, "u", 0); e.v = num(o, "v", 0);
        e.uw = num(o, "uw", e.w); e.vh = num(o, "vh", e.h); e.frames = Math.max(1, num(o, "frames", 1)); e.ft = Math.max(1, num(o, "ft", 100));
        e.dur = num(o, "dur", 220); e.delay = num(o, "delay", 0);
        e.scale = o.has("scale") ? o.get("scale").getAsFloat() : 1;
        e.value = o.has("value") ? o.get("value").getAsFloat() : 0;
        e.shown = e.value;
        e.shadow = !o.has("shadow") || o.get("shadow").getAsBoolean();
        e.visible = !o.has("visible") || o.get("visible").getAsBoolean();
        e.disabled = o.has("disabled") && o.get("disabled").getAsBoolean();
        e.pulse = o.has("pulse") && o.get("pulse").getAsBoolean();
        e.flash = o.has("flash") && o.get("flash").getAsBoolean();
        e.count = num(o, "count", 0);
        e.scroll = o.has("scroll") && o.get("scroll").getAsBoolean();
        e.glow = col(o, "glow", 0xFFFFE070);
        e.w0 = e.w; e.h0 = e.h;
        e.val = str(o, "val", "");
        e.item = str(o, "item", "");
        e.max = num(o, "max", 64); e.gap = num(o, "gap", 2); e.pad = num(o, "pad", 4); e.life = num(o, "life", 0);
        e.modal = bool(o, "modal");
        e.focus = bool(o, "focus"); e.flow = bool(o, "flow"); e.fit = bool(o, "fit"); e.sel = bool(o, "sel");
        e.bottom = bool(o, "bottom"); e.stuck = e.bottom; e.clear = bool(o, "clear"); e.glint = bool(o, "glint");
        if (o.has("tip")) for (var t : o.getAsJsonArray("tip")) e.tip.add(t.getAsString().replace('&', '§'));
        if (o.has("children")) {
            JsonArray a = o.getAsJsonArray("children");
            for (var c : a) e.kids.add(of(c.getAsJsonObject()));
        }
        return e;
    }

    public E find(String id) {
        if (id.equals(this.id)) return this;
        for (E k : kids) { E f = k.find(id); if (f != null) return f; }
        return null;
    }

    public void set(String k, String val) {
        switch (k) {
            case "text" -> text = val.replace('&', '§');
            case "value" -> value = Float.parseFloat(val);
            case "visible" -> visible = Boolean.parseBoolean(val);
            case "disabled" -> disabled = Boolean.parseBoolean(val);
            case "tex" -> tex = val;
            case "color" -> color = (int) Long.parseLong(val.replace("#", ""), 16) | (val.replace("#", "").length() <= 6 ? 0xFF000000 : 0);
            case "tip" -> { tip.clear(); if (!val.isEmpty()) for (String l : val.split("\n")) tip.add(l.replace('&', '\u00a7')); }
            case "flash" -> flashAt = System.currentTimeMillis();
            case "count" -> count = Integer.parseInt(val);
            case "u" -> u = Integer.parseInt(val);
            case "v" -> v = Integer.parseInt(val);
            case "pop" -> { text = val.replace('&', '\u00a7'); popAt = System.currentTimeMillis(); }
            case "sel" -> sel = Boolean.parseBoolean(val);
            case "val" -> this.val = val;
            case "item" -> { item = val; stack = null; }
            default -> { }
        }
    }
}
