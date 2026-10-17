package com.trolmastercard.sexmod.client.scene;

import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.network.ChooseActionPayload;
import com.trolmastercard.sexmod.network.CompanionOrderPayload;
import com.trolmastercard.sexmod.network.CompanionOrderPayload.Order;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * A girl's interaction menu: one button per scene option, with its price next to it, and the
 * companion orders down the left (follow me or stop following, go home, set new home).
 * Picking an option sends it to the server, which checks and takes the payment.
 * (1.12.2 {@code GirlInventoryScreen}; its animated layout is Phase 3e.)
 */
public class GirlActionScreen extends Screen {
    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ORDER_WIDTH = 110;
    private final GirlEntity girl;

    public GirlActionScreen(GirlEntity girl) {
        super(girl.getName());
        this.girl = girl;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        boolean following = mc.player != null && this.girl.isMaster(mc.player);
        List<Order> orders = List.of(following ? Order.STOP_FOLLOWING : Order.FOLLOW, Order.GO_HOME, Order.SET_HOME);
        int orderTop = this.height / 2 - orders.size() * (BUTTON_HEIGHT + 4) / 2;
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            addRenderableWidget(Button.builder(Component.translatable(order.translationKey), b -> order(order))
                .bounds(10, orderTop + i * (BUTTON_HEIGHT + 4), ORDER_WIDTH, BUTTON_HEIGHT)
                .build());
        }

        List<SceneOption> options = this.girl.availableOptions();
        int top = this.height / 2 - options.size() * (BUTTON_HEIGHT + 4) / 2;
        for (int i = 0; i < options.size(); i++) {
            SceneOption option = options.get(i);
            addRenderableWidget(Button.builder(Component.translatable(option.translationKey()), b -> choose(option))
                .bounds(this.width / 2 - BUTTON_WIDTH / 2, top + i * (BUTTON_HEIGHT + 4), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        }
    }

    private void order(Order order) {
        ClientPacketDistributor.sendToServer(new CompanionOrderPayload(this.girl.getId(), order));
        onClose();
    }

    private void choose(SceneOption option) {
        ClientPacketDistributor.sendToServer(new ChooseActionPayload(this.girl.getId(), option.id()));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.girl.getName().getString(), this.width / 2, 20, 0xFFFFFFFF);
        List<SceneOption> options = this.girl.availableOptions();
        int top = this.height / 2 - options.size() * (BUTTON_HEIGHT + 4) / 2;
        for (int i = 0; i < options.size(); i++) {
            SceneOption option = options.get(i);
            if (option.price().isEmpty() || this.girl.isHorny()) continue;
            int x = this.width / 2 + BUTTON_WIDTH / 2 + 6;
            int y = top + i * (BUTTON_HEIGHT + 4) + 2;
            graphics.item(option.price(), x, y);
            graphics.itemDecorations(this.font, option.price(), x, y);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
