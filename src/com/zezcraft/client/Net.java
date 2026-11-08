package com.zezcraft.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class Net {
    static final int VER = 1;

    interface W { void w(DataOutputStream o) throws IOException; }

    static void send(W w) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(b);
            w.w(o);
            ClientPlayNetworking.send(new P(b.toByteArray()));
        } catch (Throwable ignored) { }
    }

    static void hello() { send(o -> { o.writeByte(1); o.writeInt(VER); }); }

    static void key(int c, boolean d, int ms) { send(o -> { o.writeByte(2); o.writeInt(c); o.writeBoolean(d); o.writeInt(ms); }); }

    static void action(String a, boolean d) { send(o -> { o.writeByte(3); o.writeUTF(a); o.writeBoolean(d); }); }

    static void recv(byte[] d) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(d));
            int op = in.readUnsignedByte();
            switch (op) {
                case 1 -> { in.readInt(); St.server = true; }
                case 2 -> {
                    String m = in.readUTF();
                    St.Bind b = St.binds.computeIfAbsent(m, k -> new St.Bind());
                    b.flags = in.readUnsignedByte();
                    b.own = in.readInt();
                    b.aim = in.readByte();
                    b.aimMs = in.readInt();
                    b.run = in.readByte();
                    b.runMs = in.readInt();
                }
                case 3 -> St.binds.clear();
                case 4 -> {
                    String m = in.readUTF();
                    St.Bind b = St.binds.get(m);
                    int idx = in.readByte();
                    boolean lin = in.readBoolean();
                    int n = in.readInt();
                    St.Tl t = new St.Tl();
                    t.t0 = System.currentTimeMillis() - in.readInt();
                    t.lin = lin;
                    t.ms = new int[n];
                    t.v = new float[n];
                    for (int i = 0; i < n; i++) { t.ms[i] = in.readInt(); t.v[i] = in.readFloat(); }
                    t.end = in.readFloat();
                    if (b != null && n > 0) b.tl.put(idx, t);
                }
                case 5 -> {
                    St.all = in.readBoolean();
                    St.sub.clear();
                    int n = in.readInt();
                    for (int i = 0; i < n; i++) St.sub.add(in.readInt());
                }
                case 6 -> {
                    String id = in.readUTF();
                    int anchor = in.readByte();
                    int ttl = in.readInt();
                    String text = in.readUTF();
                    if (text.isEmpty()) St.hud.remove(id);
                    else St.hud.put(id, new St.Hud(anchor, ttl <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + ttl, text));
                }
                case 7 -> {
                    St.Bind b = St.binds.get(in.readUTF());
                    int idx = in.readByte();
                    if (b != null) { if (idx < 0) b.tl.clear(); else b.tl.remove(idx); }
                }
                case 8 -> { String id = in.readUTF(); String js = in.readUTF(); net.minecraft.client.Minecraft.getInstance().execute(() -> com.zezcraft.client.ui.Ui.open(id, js)); }
                case 9 -> { String id = in.readUTF(), el = in.readUTF(), k = in.readUTF(), v = in.readUTF(); com.zezcraft.client.ui.Ui.patch(id, el, k, v); }
                case 10 -> { String id = in.readUTF(); net.minecraft.client.Minecraft.getInstance().execute(() -> com.zezcraft.client.ui.Ui.close(id)); }
                default -> { }
            }
        } catch (Exception ignored) { }
    }

    static void click(String ui, String act) { send(o -> { o.writeByte(4); o.writeUTF(ui); o.writeUTF(act); }); }

    static void closed(String ui) { send(o -> { o.writeByte(5); o.writeUTF(ui); }); }
}
