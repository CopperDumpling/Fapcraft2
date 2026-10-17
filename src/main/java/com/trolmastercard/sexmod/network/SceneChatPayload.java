package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.Fapcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client → server: an animation event asks to broadcast a dialogue line to players nearby.
 * Only a translation key under the girl's dialogue prefix is accepted.
 *
 * @param byPartner the line is spoken by the partner ({@code <name> ...}) instead of the girl
 */
public record SceneChatPayload(int girlId, String key, boolean byPartner) implements CustomPacketPayload {
    public static final Type<SceneChatPayload> TYPE = new Type<>(Fapcraft.id("scene_chat"));
    public static final StreamCodec<ByteBuf, SceneChatPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, SceneChatPayload::girlId,
        ByteBufCodecs.stringUtf8(128), SceneChatPayload::key,
        ByteBufCodecs.BOOL, SceneChatPayload::byPartner,
        SceneChatPayload::new);

    @Override
    public Type<SceneChatPayload> type() {
        return TYPE;
    }
}
