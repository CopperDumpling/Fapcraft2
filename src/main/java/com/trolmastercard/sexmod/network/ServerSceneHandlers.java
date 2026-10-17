package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.entity.BedFinder;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.menu.GirlEquipmentMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

/** Server handlers for the menu and scene payloads. Each one re-checks everything the client claims. */
final class ServerSceneHandlers {
    /** Furthest a player may be from a girl to use her menu. */
    private static final double MENU_RANGE = 8.0;
    /** Radius dialogue lines reach (1.12.2: 40 blocks). */
    private static final double CHAT_RANGE = 40.0;

    private ServerSceneHandlers() {
    }

    static void chooseAction(ChooseActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        GirlEntity girl = girl(player, payload.girlId(), MENU_RANGE);
        if (girl == null || girl.isBusy() || !girl.takesOrdersFrom(player)) return;
        SceneOption option = girl.availableOptions().stream().filter(o -> o.id().equals(payload.option())).findFirst().orElse(null);
        if (option == null) return;
        if (option.needsBed()) {
            BedFinder.Plan plan = girl.planBed();
            if (!plan.ok()) {
                girl.refuseBed(player, plan.failure());
                return;
            }
        }
        boolean free = girl.isHorny() || option.price().isEmpty() || player.hasInfiniteMaterials();
        if (!free && !takeItems(player.getInventory(), option.price())) {
            ItemStack price = option.price();
            player.sendSystemMessage(Component.translatable("sexmod.scene.cant_afford", price.getCount(), price.getHoverName()));
            return;
        }
        if (option.solo()) {
            girl.startSolo(player, option);
        } else {
            girl.startScene(player, option);
        }
    }

    static void order(CompanionOrderPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        GirlEntity girl = girl(player, payload.girlId(), MENU_RANGE);
        if (girl == null || girl.isBusy() || !girl.takesOrdersFrom(player)) return;
        switch (payload.order()) {
            case FOLLOW -> girl.setMaster(player);
            case STOP_FOLLOWING -> girl.clearMaster();
            case GO_HOME -> girl.goHome();
            case SET_HOME -> {
                girl.setHome(Vec3.atBottomCenterOf(girl.blockPosition()));
                girl.onNewHome(player);
            }
            case EQUIPMENT -> player.openMenu(
                new SimpleMenuProvider((id, inventory, p) -> new GirlEquipmentMenu(id, inventory, girl), girl.getName()),
                buf -> buf.writeVarInt(girl.getId()));
        }
    }

    static void step(SceneStepPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            GirlEntity girl = girl(player, payload.girlId(), 16.0);
            if (girl != null) girl.requestStep(player, payload.from(), payload.to());
        }
    }

    static void pose(ScenePosePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            GirlEntity girl = girl(player, payload.girlId(), 16.0);
            if (girl != null) girl.requestNamed(player, payload.pose());
        }
    }

    static void chat(SceneChatPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        GirlEntity girl = girl(player, payload.girlId(), 16.0);
        if (girl == null || !girl.isPartner(player) || !payload.key().startsWith(girl.dialoguePrefix())) return;
        Component speaker = payload.byPartner() ? player.getName() : girl.getName();
        Component line = Component.literal("<").append(speaker).append("> ").append(Component.translatable(payload.key()));
        for (ServerPlayer nearby : player.level().players()) {
            if (nearby.distanceToSqr(girl) <= CHAT_RANGE * CHAT_RANGE) nearby.sendSystemMessage(line);
        }
    }

    static void leave(LeaveScenePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        for (GirlEntity girl : player.level().getEntitiesOfClass(GirlEntity.class, player.getBoundingBox().inflate(16.0))) {
            if (girl.isPartner(player)) girl.endScene();
        }
    }

    private static @Nullable GirlEntity girl(ServerPlayer player, int id, double range) {
        return player.level().getEntity(id) instanceof GirlEntity girl && girl.distanceToSqr(player) <= range * range ? girl : null;
    }

    /** Removes {@code price} (item + count) from the inventory if it is all there. */
    private static boolean takeItems(Inventory inventory, ItemStack price) {
        int have = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, price)) have += stack.getCount();
        }
        if (have < price.getCount()) return false;
        int left = price.getCount();
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, price)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
        inventory.setChanged();
        return true;
    }
}
