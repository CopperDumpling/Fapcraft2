package com.trolmastercard.sexmod.client.gui;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.entity.bee.BeeEntity;
import com.trolmastercard.sexmod.network.CompanionOrderPayload;
import com.trolmastercard.sexmod.network.CompanionOrderPayload.Order;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The bee's dialogue (1.12.2 {@code BeeDialogueScreen}), once she carries a chest: her chest at
 * the top (click to open it), "follow me" / "stop following" and "go home" either side of it, and
 * a house below that sets her new home where she is. The buttons slide in over a quarter second.
 * Her reactions are local chat lines, as in 1.12.2.
 */
public class BeeDialogueScreen extends Screen {
    private static final Identifier TEXTURE = Fapcraft.id("textures/gui/girlinventory.png");
    private static final float SLIDE_MILLIS = 250.0F;

    private final BeeEntity bee;
    private final boolean following;
    private long openedAt;
    private @Nullable Button followButton;
    private @Nullable Button homeButton;
    private @Nullable Button setHomeButton;

    public BeeDialogueScreen(BeeEntity bee) {
        super(bee.getName());
        this.bee = bee;
        this.following = bee.hasMaster();
    }

    @Override
    protected void init() {
        this.openedAt = Util.getMillis();
        Component follow = Component.translatable(this.following ? Order.STOP_FOLLOWING.translationKey : Order.FOLLOW.translationKey);
        this.followButton = addRenderableWidget(Button.builder(follow, b -> {
            if (this.following) {
                order(Order.STOP_FOLLOWING, "bee.dialogue.sad");
            } else {
                order(Order.FOLLOW, "bee.dialogue.exited");
            }
        }).bounds(0, 30, 0, 20).build());
        this.homeButton = addRenderableWidget(Button.builder(Component.translatable(Order.GO_HOME.translationKey), b -> order(Order.GO_HOME, null))
            .bounds(this.width / 2 + 19, 30, 0, 20).build());
        this.setHomeButton = addRenderableWidget(Button.builder(Component.empty(), b -> order(Order.SET_HOME, "bee.dialogue.home"))
            .bounds(this.width / 2 - 10, 0, 20, 20).build());
    }

    private void order(Order order, @Nullable String reaction) {
        ClientPacketDistributor.sendToServer(new CompanionOrderPayload(this.bee.getId(), order));
        if (reaction != null && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.translatable(reaction));
        }
        onClose();
    }

    private float progress() {
        return Mth.clamp((Util.getMillis() - this.openedAt) / SLIDE_MILLIS, 0.0F, 1.0F);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float progress = progress();
        int center = this.width / 2;
        int width = (int) (progress * 100.0F);
        int drop = (int) (15.0F - progress * 15.0F);
        if (this.followButton != null) {
            this.followButton.setX(center - 119 + (100 - width));
            this.followButton.setWidth(width);
        }
        if (this.homeButton != null) this.homeButton.setWidth(width);
        if (this.setHomeButton != null) this.setHomeButton.setY(59 - drop);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, center - 7, 61 - drop, 32, 0, 15, 15, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, center - 20, 20, this.bee.hasChest() ? 0 : 40, 130, 40, 40, 256, 256);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int center = this.width / 2;
        if (this.bee.hasChest() && event.x() >= center - 20 && event.x() <= center + 20 && event.y() >= 20 && event.y() <= 60) {
            order(Order.OPEN_CHEST, null);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
