package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.Fapcraft;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

/**
 * Client → server: one of the menu's companion buttons (1.12.2 {@code doAction} with
 * {@code followme}/{@code stopfollowme}/{@code gohome}/{@code setnewhome}, which wrote the
 * master and home straight from the client, and {@code equipment}).
 */
public record CompanionOrderPayload(int girlId, Order order) implements CustomPacketPayload {
    public enum Order {
        FOLLOW("action.names.followme"),
        STOP_FOLLOWING("action.names.stopfollowme"),
        GO_HOME("action.names.gohome"),
        SET_HOME("action.names.setnewhome"),
        EQUIPMENT("action.names.equipment");

        private static final IntFunction<Order> BY_ID = ByIdMap.continuous(Order::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Order> CODEC = ByteBufCodecs.idMapper(BY_ID, Order::ordinal);

        public final String translationKey;

        Order(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    public static final Type<CompanionOrderPayload> TYPE = new Type<>(Fapcraft.id("companion_order"));
    public static final StreamCodec<ByteBuf, CompanionOrderPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, CompanionOrderPayload::girlId,
        Order.CODEC, CompanionOrderPayload::order,
        CompanionOrderPayload::new);

    @Override
    public Type<CompanionOrderPayload> type() {
        return TYPE;
    }
}
