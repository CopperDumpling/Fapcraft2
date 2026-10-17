package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.Fapcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: an animation event asks to move the partner to one of the scene's named poses. */
public record ScenePosePayload(int girlId, String pose) implements CustomPacketPayload {
    public static final Type<ScenePosePayload> TYPE = new Type<>(Fapcraft.id("scene_pose"));
    public static final StreamCodec<ByteBuf, ScenePosePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ScenePosePayload::girlId,
        ByteBufCodecs.stringUtf8(64), ScenePosePayload::pose,
        ScenePosePayload::new);

    @Override
    public Type<ScenePosePayload> type() {
        return TYPE;
    }
}
