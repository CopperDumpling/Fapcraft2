package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.entity.GirlAction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: the scene partner's client asks to move from {@code from} to {@code to}
 * (an animation keyframe event or a scene control). The server checks the girl's step graph.
 */
public record SceneStepPayload(int girlId, GirlAction from, GirlAction to) implements CustomPacketPayload {
    public static final Type<SceneStepPayload> TYPE = new Type<>(Fapcraft.id("scene_step"));
    private static final StreamCodec<ByteBuf, GirlAction> ACTION = ByteBufCodecs.VAR_INT.map(GirlAction::byId, GirlAction::ordinal);
    public static final StreamCodec<ByteBuf, SceneStepPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SceneStepPayload::girlId,
        ACTION, SceneStepPayload::from,
        ACTION, SceneStepPayload::to,
        SceneStepPayload::new);

    @Override
    public Type<SceneStepPayload> type() {
        return TYPE;
    }
}
