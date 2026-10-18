package com.trolmastercard.sexmod.client.render.luna;

import com.mojang.serialization.MapCodec;
import com.trolmastercard.sexmod.entity.luna.LunaEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * {@code sexmod:luna_cast}: Luna's rod looks cast while her hook is out (1.12.2 {@code LunaRodItem}'s
 * {@code cast} override); in a player's hand it behaves as vanilla's {@code fishing_rod/cast}.
 */
public record LunaRodCast() implements ConditionalItemModelProperty {
    public static final MapCodec<LunaRodCast> MAP_CODEC = MapCodec.unit(new LunaRodCast());
    private static final FishingRodCast PLAYER_CAST = new FishingRodCast();

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext context) {
        if (owner instanceof LunaEntity luna) return luna.isRodCast();
        return PLAYER_CAST.get(stack, level, owner, seed, context);
    }

    @Override
    public MapCodec<LunaRodCast> type() {
        return MAP_CODEC;
    }
}
