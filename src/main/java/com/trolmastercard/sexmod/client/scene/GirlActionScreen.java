package com.trolmastercard.sexmod.client.scene;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.GirlSlot;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.network.ChooseActionPayload;
import com.trolmastercard.sexmod.network.CompanionOrderPayload;
import com.trolmastercard.sexmod.network.CompanionOrderPayload.Order;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * A girl's interaction menu, in the 1.12.2 {@code GirlInventoryScreen} layout:
 * <ul>
 *   <li>her scene options slide in at the bottom right, stacked upwards, each with its price to
 *   the left of it;</li>
 *   <li>her equipment slides in on the left, then a column of icon buttons for the companion
 *   orders (follow me or stop following, go home, set new home, equipment); each opens out to
 *   show its label while the mouse is over it.</li>
 * </ul>
 * Picking an option or order sends it to the server, which checks it and takes any payment.
 */
public class GirlActionScreen extends Screen {
    private static final Identifier TEXTURE = Fapcraft.id("textures/gui/girlinventory.png");
    /** Each slide-in stage takes 5 ticks (1.12.2 added the frame's elapsed ticks / 5). */
    private static final float STAGE_MILLIS = 250.0F;
    /** Width an order button opens by per 1/60 s while hovered or closes by otherwise (1.12.2: 7 per frame). */
    private static final float OPEN_SPEED = 7.0F * 60.0F / 1000.0F;

    private static final int OPTION_WIDTH = 100;
    private static final int ORDER_X = 36;
    private static final int ORDER_TOP = 70;
    private static final int ROW_SPACING = 30;
    /** Where her equipment items sit, from the top (weapon, bow, helmet, chest plate, pants, shoes). */
    private static final int[] EQUIPMENT_Y = {68, 87, 109, 127, 146, 166};
    private static final List<GirlSlot> EQUIPMENT = List.of(GirlSlot.WEAPON, GirlSlot.BOW, GirlSlot.HELMET, GirlSlot.CHEST_PLATE, GirlSlot.PANTS, GirlSlot.SHOES);

    private final GirlEntity girl;
    private final List<SceneOption> options;
    private final List<Button> optionButtons = new ArrayList<>();
    private final List<OrderButton> orderButtons = new ArrayList<>();
    private long openedAt;
    private long lastFrame;

    /** An order's icon (u in the texture), how far it opens and how many spaces pad its label clear of the icon. */
    private enum OrderStyle {
        FOLLOW(Order.FOLLOW, 64, 50, 4),
        STOP_FOLLOWING(Order.STOP_FOLLOWING, 80, 90, 4),
        GO_HOME(Order.GO_HOME, 47, 50, 5),
        SET_HOME(Order.SET_HOME, 32, 80, 5),
        EQUIPMENT(Order.EQUIPMENT, 96, 60, 4);

        final Order order;
        final int iconU;
        final int maxOpen;
        final int padding;

        OrderStyle(Order order, int iconU, int maxOpen, int padding) {
            this.order = order;
            this.iconU = iconU;
            this.maxOpen = maxOpen;
            this.padding = padding;
        }
    }

    private static final class OrderButton {
        final OrderStyle style;
        final Button button;
        final Component label;
        float open;

        OrderButton(OrderStyle style, Button button, Component label) {
            this.style = style;
            this.button = button;
            this.label = label;
        }
    }

    public GirlActionScreen(GirlEntity girl) {
        super(girl.getName());
        this.girl = girl;
        this.options = girl.availableOptions();
    }

    @Override
    protected void init() {
        this.openedAt = Util.getMillis();
        this.lastFrame = this.openedAt;
        this.optionButtons.clear();
        this.orderButtons.clear();
        for (SceneOption option : this.options) {
            this.optionButtons.add(addRenderableWidget(Button.builder(Component.translatable(option.translationKey()), b -> choose(option))
                .bounds(this.width, 0, OPTION_WIDTH, 20)
                .build()));
        }
        // 1.12.2 swapped "follow me" for "stop following" whenever she had a master
        OrderStyle follow = this.girl.hasMaster() ? OrderStyle.STOP_FOLLOWING : OrderStyle.FOLLOW;
        for (OrderStyle style : List.of(follow, OrderStyle.GO_HOME, OrderStyle.SET_HOME, OrderStyle.EQUIPMENT)) {
            Component label = Component.translatable(style.order.translationKey).append(" ".repeat(style.padding));
            Button button = addRenderableWidget(Button.builder(Component.empty(), b -> order(style.order))
                .bounds(ORDER_X, 0, 0, 20)
                .build());
            this.orderButtons.add(new OrderButton(style, button, label));
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
        long now = Util.getMillis();
        float frameMillis = now - this.lastFrame;
        this.lastFrame = now;
        float slideIn = Mth.clamp((now - this.openedAt) / STAGE_MILLIS, 0.0F, 1.0F);
        float openUp = Mth.clamp((now - this.openedAt - STAGE_MILLIS) / STAGE_MILLIS, 0.0F, 1.0F);
        int slide = (int) Mth.lerp(slideIn, -30.0F, 120.0F);

        layoutOptions(graphics, slide, openUp);
        // her equipment, sliding in from the left
        if (openUp > 0.0F) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, slide - 113, 60, 0, 0, 32, 130, 256, 256);
        }
        for (int i = 0; i < EQUIPMENT.size(); i++) {
            graphics.item(this.girl.getShownEquipment(EQUIPMENT.get(i)), slide - 105, EQUIPMENT_Y[i]);
        }
        layoutOrders(mouseX, mouseY, openUp, frameMillis);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (openUp > 0.0F) {
            int grow = (int) Mth.lerp(openUp, 0.0F, 23.0F);
            for (int i = 0; i < this.orderButtons.size(); i++) {
                OrderButton order = this.orderButtons.get(i);
                int y = ORDER_TOP + i * ROW_SPACING;
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, (int) order.open + ORDER_X - 19 + grow, y + 2, order.style.iconU, 0, 16, 16, 256, 256);
            }
        }
    }

    /** Scene options at the bottom right, stacked upwards, with each price in a dark box to the left. */
    private void layoutOptions(GuiGraphicsExtractor graphics, int slide, float openUp) {
        boolean free = this.girl.isHorny();
        for (int i = 0; i < this.optionButtons.size(); i++) {
            int y = this.height - 70 - i * ROW_SPACING;
            Button button = this.optionButtons.get(i);
            button.setX(this.width - slide);
            button.setY(y);
            ItemStack price = this.options.get(i).price();
            if (openUp > 0.0F && !free && !price.isEmpty()) {
                int textX = this.width - (int) Mth.lerp(openUp, 115.0F, 161.0F) + 12;
                drawPrice(graphics, price, textX, y + 6, this.width - (int) Mth.lerp(openUp, 91.0F, 137.0F), y + 2);
            }
        }
    }

    /** 1.12.2's hand-drawn tooltip box with "{@code <count>x}", and the item drawn inside it. */
    private void drawPrice(GuiGraphicsExtractor graphics, ItemStack price, int textX, int textY, int itemX, int itemY) {
        String text = price.getCount() + "x    ";
        int width = this.font.width(text);
        int height = 8;
        int x0 = textX - 3;
        int x1 = textX + width + 3;
        int y0 = textY - 3;
        int y1 = textY + height + 3;
        int background = 0xF0100010;
        graphics.fill(x0, y0 - 1, x1, y0, background);
        graphics.fill(x0, y1, x1, y1 + 1, background);
        graphics.fill(x0, y0, x1, y1, background);
        graphics.fill(x0 - 1, y0, x0, y1, background);
        graphics.fill(x1, y0, x1 + 1, y1, background);
        graphics.fillGradient(x0, y0 + 1, x0 + 1, y1 - 1, 0x505000FF, 0x5028007F);
        graphics.fillGradient(x1 - 1, y0 + 1, x1, y1 - 1, 0x505000FF, 0x5028007F);
        graphics.fill(x0, y0, x1, y0 + 1, 0x505000FF);
        graphics.fill(x0, y1 - 1, x1, y1, 0x5028007F);
        graphics.text(this.font, text, textX, textY, 0xFFFFFFFF, true);
        graphics.item(price, itemX, itemY);
    }

    /** The order buttons open while hovered and close again otherwise; the label shows once open past the icon. */
    private void layoutOrders(int mouseX, int mouseY, float openUp, float frameMillis) {
        int grow = (int) Mth.lerp(openUp, 0.0F, 23.0F);
        for (int i = 0; i < this.orderButtons.size(); i++) {
            OrderButton order = this.orderButtons.get(i);
            int y = ORDER_TOP + i * ROW_SPACING;
            if (openUp > 0.0F) {
                boolean hovered = mouseX >= ORDER_X - 1 && mouseX <= ORDER_X - 1 + 23 + order.open && mouseY >= y && mouseY <= y + 20;
                float step = OPEN_SPEED * frameMillis;
                order.open = hovered ? Math.min(order.style.maxOpen, order.open + step) : Math.max(0.0F, order.open - step);
            }
            Button button = order.button;
            button.setY(y);
            button.setWidth(grow + (int) order.open);
            button.visible = openUp > 0.0F;
            button.setMessage(order.open > 14.0F ? order.label : Component.empty());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
