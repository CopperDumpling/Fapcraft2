package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The horny potion (1.12.2 {@code HornyPotion}). On a girl it waives payment. The
 * player-transformation it also triggers is Phase 6, and brewing is data-driven in 26.3
 * (see {@code data/sexmod/recipe/brewing}).
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Fapcraft.MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, Fapcraft.MODID);

    /** 1.12.2 colour 16736968 (0xFF62C8). */
    public static final DeferredHolder<MobEffect, MobEffect> HORNY =
        EFFECTS.register("horny_potion", () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xFF62C8) { });
    public static final Holder<Potion> HORNY_POTION =
        POTIONS.register("horny_potion", () -> new Potion("horny_potion", new MobEffectInstance(HORNY, 3600), new MobEffectInstance(MobEffects.NAUSEA, 200, 1)));

    private ModEffects() {
    }
}
