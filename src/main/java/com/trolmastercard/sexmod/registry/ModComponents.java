package com.trolmastercard.sexmod.registry;

import com.mojang.serialization.Codec;
import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item data components (replace 1.12 item NBT: wand state, egg tribe, ...). */
public final class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Fapcraft.MODID);

    /** Wishes already granted by an Allies lamp (1.12.2 {@code sexmodUses}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WISHES_USED = COMPONENTS.registerComponentType(
        "wishes_used", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModComponents() {
    }
}
