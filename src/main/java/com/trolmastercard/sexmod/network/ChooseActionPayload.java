package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.Fapcraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the player picked {@code option} from a girl's interaction menu. */
public record ChooseActionPayload(int girlId, String option) implements CustomPacketPayload {
    public static final Type<ChooseActionPayload> TYPE = new Type<>(Fapcraft.id("choose_action"));
    public static final StreamCodec<ByteBuf, ChooseActionPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ChooseActionPayload::girlId,
        ByteBufCodecs.stringUtf8(64), ChooseActionPayload::option,
        ChooseActionPayload::new);

    @Override
    public Type<ChooseActionPayload> type() {
        return TYPE;
    }
}
