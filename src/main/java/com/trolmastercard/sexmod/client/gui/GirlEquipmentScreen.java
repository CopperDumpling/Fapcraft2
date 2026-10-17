package com.trolmastercard.sexmod.client.gui;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.menu.GirlEquipmentMenu;
import com.trolmastercard.sexmod.registry.ModMenus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * The equipment screen (1.12.2 {@code GirlInventoryContainerGui2}): her weapon, bow and armour
 * slots over the player's inventory, drawn from {@code girlinventory.png} with no labels.
 */
public class GirlEquipmentScreen extends AbstractContainerScreen<GirlEquipmentMenu> {
    static final Identifier TEXTURE = Fapcraft.id("textures/gui/girlinventory.png");
    /** The panel in the texture: 176x114 from (33, 16), drawn 52 below the menu's usual top edge. */
    private static final int PANEL_U = 33;
    private static final int PANEL_V = 16;
    private static final int PANEL_HEIGHT = 114;
    private static final int PANEL_OFFSET_Y = 52;

    public GirlEquipmentScreen(GirlEquipmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos + PANEL_OFFSET_Y,
            PANEL_U, PANEL_V, this.imageWidth, PANEL_HEIGHT, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.12.2 drew no title or inventory label
    }

    @EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void onRegisterScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.GIRL_EQUIPMENT.get(), GirlEquipmentScreen::new);
        }
    }
}
