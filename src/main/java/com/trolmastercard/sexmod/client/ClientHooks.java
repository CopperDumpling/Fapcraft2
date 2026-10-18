package com.trolmastercard.sexmod.client;

import com.trolmastercard.sexmod.client.gui.BeeDialogueScreen;
import com.trolmastercard.sexmod.client.scene.GirlActionScreen;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.allie.AllieEntity;
import com.trolmastercard.sexmod.entity.bee.BeeEntity;
import com.trolmastercard.sexmod.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client actions called from common code. Only invoked when {@code level.isClientSide()}, so
 * a dedicated server never loads this class.
 */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openActionMenu(GirlEntity girl) {
        Minecraft.getInstance().gui.setScreen(new GirlActionScreen(girl));
    }

    public static void openBeeDialogue(BeeEntity bee) {
        Minecraft.getInstance().gui.setScreen(new BeeDialogueScreen(bee));
    }

    /** Opens Allie's wish menu if the local player is her summoner. */
    public static void openWishMenu(AllieEntity allie) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && allie.isPartner(mc.player)) openActionMenu(allie);
    }

    /** Allie's wish menu comes back once no other screen is open; true if it did (1.12.2 {@code ac}). */
    public static boolean reopenWishMenu(AllieEntity allie) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !allie.isPartner(mc.player)) return true; // nothing to reopen
        if (mc.gui.screen() != null) return false;
        openActionMenu(allie);
        return true;
    }

    /** Whether the local player is rubbing an Allies lamp. */
    public static boolean isRubbingLamp() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getData(ModAttachments.LAMP_RUBBING);
    }

    /** 1.12.2: "{@code <Jenny> I am busy at the moment~}", shown only to the clicking player. */
    public static void showBusy(GirlEntity girl) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("<").append(girl.getName()).append("> ")
                .append(Component.translatable(girl.dialoguePrefix() + "busy")));
        }
    }
}
