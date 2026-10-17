package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.Fapcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the player pressed the leave-scene key (1.12.2 R-Shift). */
public record LeaveScenePayload() implements CustomPacketPayload {
    public static final LeaveScenePayload INSTANCE = new LeaveScenePayload();
    public static final Type<LeaveScenePayload> TYPE = new Type<>(Fapcraft.id("leave_scene"));
    public static final StreamCodec<ByteBuf, LeaveScenePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<LeaveScenePayload> type() {
        return TYPE;
    }
}
