package com.trolmastercard.sexmod.client;

import com.trolmastercard.sexmod.client.gui.BeeDialogueScreen;
import com.trolmastercard.sexmod.client.scene.GirlActionScreen;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.bee.BeeEntity;
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

    /** 1.12.2: "{@code <Jenny> I am busy at the moment~}", shown only to the clicking player. */
    public static void showBusy(GirlEntity girl) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("<").append(girl.getName()).append("> ")
                .append(Component.translatable(girl.dialoguePrefix() + "busy")));
        }
    }
}
