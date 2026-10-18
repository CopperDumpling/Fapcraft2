package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.Fapcraft;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments: per-player / per-entity state (ownership, player-as-girl model, ...). */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Fapcraft.MODID);

    /**
     * The player is rubbing an Allies lamp (1.12.2 {@code sexmodAllieInUse}). Synced to that player
     * only, for the lamp's rub animation and the hidden HUD. Not saved: it ends on logout, as in 1.12.
     */
    public static final Supplier<AttachmentType<Boolean>> LAMP_RUBBING = ATTACHMENTS.register("lamp_rubbing",
        () -> AttachmentType.builder(() -> false).sync((holder, to) -> holder == to, ByteBufCodecs.BOOL).build());
    /** Server: ticks of rubbing so far (1.12.2 {@code sexmodAllieInUseTicks}). */
    public static final Supplier<AttachmentType<Integer>> LAMP_RUB_TICKS = ATTACHMENTS.register("lamp_rub_ticks",
        () -> AttachmentType.builder(() -> 0).build());

    private ModAttachments() {
    }
}
