package com.trolmastercard.sexmod.item;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.trolmastercard.sexmod.client.ClientHooks;
import com.trolmastercard.sexmod.client.render.lamp.AlliesLampRenderProvider;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.allie.AllieEntity;
import com.trolmastercard.sexmod.registry.ModAttachments;
import com.trolmastercard.sexmod.registry.ModComponents;
import com.trolmastercard.sexmod.util.ParticleRings;
import com.trolmastercard.sexmod.util.VectorMath;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Allie's lamp (1.12.2 {@code AlliesLampItem}): three wishes.
 * <ul>
 *   <li>Right-click starts rubbing it. While the lamp is held the rub runs on its own: sparkles
 *   gather from the player's eyes down to the lamp's spot two blocks ahead from 2.5 s, and at
 *   4.75 s Allie appears there, facing the player. On sand she runs away instead.</li>
 *   <li>A lamp whose Allie is still out, or with no wishes left, does nothing. Used lamps show the
 *   wishes left.</li>
 *   <li>The rub animation shows the player's own arms (their skin) rubbing it in first person,
 *   and the HUD is hidden meanwhile.</li>
 * </ul>
 * Found in mineshaft, dungeon and woodland mansion chests ({@code data/sexmod/loot_modifiers}).
 */
public class AlliesLampItem extends Item implements GeoItem {
    public static final int WISHES = 3;
    private static final int SUMMON_TICKS = 95;
    private static final int SPARKLES_FROM_TICKS = 50;
    private static final int SPARKLES = 150;
    private static final double SPARKLE_RADIUS = 0.75;
    private static final RawAnimation RUB = RawAnimation.begin().thenPlayAndHold("animation.lamp.rub");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AlliesLampItem(Properties properties) {
        super(properties);
    }

    public static int wishesUsed(ItemStack stack) {
        return stack.getOrDefault(ModComponents.WISHES_USED.get(), 0);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (wishesUsed(stack) >= WISHES || player.getData(ModAttachments.LAMP_RUBBING) || GirlEntity.inScene(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            if (AllieEntity.hasAllieOut(serverPlayer)) return InteractionResult.PASS;
            serverPlayer.setData(ModAttachments.LAMP_RUBBING, true);
            serverPlayer.setData(ModAttachments.LAMP_RUB_TICKS, 0);
        }
        return InteractionResult.CONSUME;
    }

    /** The rub advances only while the lamp is in a hand (1.12.2: main or off hand). */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof ServerPlayer player) || slot == null || slot.getType() != EquipmentSlot.Type.HAND) return;
        if (!player.getData(ModAttachments.LAMP_RUBBING)) return;
        int ticks = player.getData(ModAttachments.LAMP_RUB_TICKS);
        player.setData(ModAttachments.LAMP_RUB_TICKS, ticks + 1);
        Vec3 spot = lampSpot(player);
        if (ticks > SPARKLES_FROM_TICKS && ticks < SUMMON_TICKS) {
            double progress = (double) (ticks - SPARKLES_FROM_TICKS) / (SUMMON_TICKS - SPARKLES_FROM_TICKS);
            progress = 1.0 - Math.pow(1.0 - progress, 4.0);
            ParticleRings.ring(level, ParticleTypes.ENCHANTED_HIT, spot.add(0.0, player.getEyeHeight() * (1.0 - progress), 0.0),
                (int) (progress * SPARKLES), progress * SPARKLE_RADIUS, progress);
        }
        if (ticks >= SUMMON_TICKS) {
            ParticleRings.ring(level, ParticleTypes.ENCHANTED_HIT, spot, SPARKLES, SPARKLE_RADIUS, 2.0);
            player.setData(ModAttachments.LAMP_RUBBING, false);
            player.setData(ModAttachments.LAMP_RUB_TICKS, 0);
            int used = wishesUsed(stack);
            stack.set(ModComponents.WISHES_USED.get(), used + 1);
            AllieEntity.summon(level, player, spot, used);
        }
    }

    /** Two blocks ahead of where the player looks (1.12.2 {@code getLampOffset}). */
    private static Vec3 lampSpot(Player player) {
        return player.position().add(VectorMath.rotateByYaw(new Vec3(0.0, 0.0, 2.0), player.getYHeadRot()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        // 1.12.2 showed it once the lamp had been used
        if (stack.has(ModComponents.WISHES_USED.get())) {
            int left = Math.max(0, WISHES - wishesUsed(stack));
            if (left < WISHES) builder.accept(Component.translatable("item.sexmod.allies_lamp.wishes_left." + left));
        }
    }

    /* ------------------------------------------------------------------ */
    /* GeckoLib                                                           */
    /* ------------------------------------------------------------------ */

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 1.12.2: every lamp shows the rub while the local player rubs one
        controllers.add(new AnimationController<AlliesLampItem>("controller", 2, test -> {
            if (ClientHooks.isRubbingLamp()) return test.setAndContinue(RUB);
            test.controller().reset();
            return PlayState.STOP;
        }));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new AlliesLampRenderProvider());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
