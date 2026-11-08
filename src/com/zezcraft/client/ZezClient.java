package com.zezcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ZezClient implements ClientModInitializer {
    static final Map<String, KeyMapping> acts = new LinkedHashMap<>();
    static final Map<String, Boolean> actDown = new java.util.HashMap<>();

    public void onInitializeClient() {
        PayloadTypeRegistry.clientboundPlay().register(P.TYPE, P.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(P.TYPE, P.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(P.TYPE, (p, ctx) -> Net.recv(p.d()));
        ClientPlayConnectionEvents.JOIN.register((h, s, mc) -> { St.reset(); Input.held.clear(); Net.hello(); });
        net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents.REGISTER.register((h, snd, mc, ids) -> {
            if (ids.contains(P.TYPE.id())) { St.binds.clear(); St.server = false; tries = 0; Net.hello(); }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> { St.reset(); com.zezcraft.client.ui.Ui.hud.clear(); });
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("zezclient", "main"));
        acts.put("reload", KeyMappingHelper.registerKeyMapping(new KeyMapping("key.zezclient.reload", InputConstants.KEY_R, cat)));
        acts.put("inspect", KeyMappingHelper.registerKeyMapping(new KeyMapping("key.zezclient.inspect", InputConstants.KEY_V, cat)));
        acts.put("mode", KeyMappingHelper.registerKeyMapping(new KeyMapping("key.zezclient.mode", InputConstants.KEY_B, cat)));
        acts.put("skill", KeyMappingHelper.registerKeyMapping(new KeyMapping("key.zezclient.skill", InputConstants.KEY_G, cat)));
        ClientTickEvents.END_CLIENT_TICK.register(ZezClient::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("zezclient", "hud"), ZezClient::hud);
        com.zezcraft.client.ui.Ui.onClick = Net::click;
        com.zezcraft.client.ui.Ui.onClose = Net::closed;
    }

    static int tries, clock;

    static void tick(Minecraft mc) {
        if (mc.player == null) return;
        if (!St.server && tries < 12 && ++clock % 100 == 0) { tries++; Net.hello(); }
        boolean free = mc.screen == null;
        Input.poll(free);
        if (!St.server) return;
        for (var e : acts.entrySet()) {
            boolean d = free && e.getValue().isDown();
            boolean was = actDown.getOrDefault(e.getKey(), false);
            if (d != was) { actDown.put(e.getKey(), d); Net.action(e.getKey(), d); }
        }
    }

    static void hud(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        com.zezcraft.client.ui.Ui.drawHud(g);
        if (St.hud.isEmpty()) return;
        long now = System.currentTimeMillis();
        St.hud.values().removeIf(h -> h.until() < now);
        int w = g.guiWidth(), h = g.guiHeight();
        int[] row = new int[4];
        for (St.Hud e : St.hud.values()) {
            String[] lines = e.text().split("\n");
            for (String l : lines) {
                int tw = mc.font.width(l);
                int a = e.anchor();
                int x, y;
                switch (a) {
                    case 1 -> { x = (w - tw) / 2; y = h - 59 - 10 * row[1]; }
                    case 2 -> { x = 4; y = 4 + 10 * row[2]; }
                    case 3 -> { x = (w - tw) / 2; y = h / 2 + 12 + 10 * row[3]; }
                    default -> { x = w - tw - 6; y = h - 22 - 10 * row[0]; a = 0; }
                }
                row[a]++;
                if (a == 0) g.fill(x - 3, y - 2, x + tw + 3, y + 9, 0x66000000);
                g.text(mc.font, l, x, y, 0xFFFFFFFF, true);
            }
        }
    }
}
