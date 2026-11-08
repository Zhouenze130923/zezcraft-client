package com.zezcraft.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record P(byte[] d) implements CustomPacketPayload {
    public static final Type<P> TYPE = new Type<>(Identifier.fromNamespaceAndPath("zez", "c"));
    public static final StreamCodec<FriendlyByteBuf, P> CODEC = StreamCodec.of((b, p) -> b.writeBytes(p.d), b -> {
        byte[] a = new byte[b.readableBytes()];
        b.readBytes(a);
        return new P(a);
    });

    public Type<P> type() { return TYPE; }
}
