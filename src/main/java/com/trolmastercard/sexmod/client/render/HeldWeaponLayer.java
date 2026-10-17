package com.trolmastercard.sexmod.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Draws the weapon or bow a girl has drawn on her {@code weapon} bone (1.12.2
 * {@code GirlRenderer.renderHeldItem}). The item is turned about the bone's X axis by a
 * per-girl angle: one for the bow, one for slashes, and one plus an offset for the stab.
 */
public class HeldWeaponLayer<T extends GirlEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
    public static final String BONE = "weapon";
    private static final DataTicket<ItemStackRenderState> ITEM = DataTicket.create("sexmod_held_weapon", ItemStackRenderState.class);
    private static final DataTicket<Grip> GRIP = DataTicket.create("sexmod_held_grip", Grip.class);

    /**
     * How a girl holds her weapons (1.12.2 {@code slashSwordRot}, {@code stabSwordRot},
     * {@code swordOffsetStab}, {@code holdBowRot}; degrees about X).
     */
    public record WeaponPose(float slash, float stab, Vector3f stabOffset, float bow) {
    }

    private enum Grip { SLASH, STAB, BOW }

    private final ItemModelResolver itemModelResolver;
    private final WeaponPose pose;

    public HeldWeaponLayer(EntityRendererProvider.Context context, GeoRenderer<T, Void, LivingEntityRenderState> renderer, WeaponPose pose) {
        super(renderer);
        this.itemModelResolver = context.getItemModelResolver();
        this.pose = pose;
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        ItemStack held = girl.getHeldItem();
        if (held.isEmpty()) return;
        Grip grip;
        if (held.getItem() instanceof BowItem) {
            grip = Grip.BOW;
        } else if (girl.getAction() == GirlAction.ATTACK && girl.getAttackAnimation() == 0) {
            grip = Grip.STAB;
        } else {
            grip = Grip.SLASH;
        }
        renderState.addGeckolibData(ITEM, RenderUtil.createRenderStateForItem(held, this.itemModelResolver, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, girl));
        renderState.addGeckolibData(GRIP, grip);
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> pass, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
        if (!pass.willRender() || !pass.renderState().hasGeckolibData(ITEM)) return;
        pass.model().getBone(BONE).ifPresent(bone -> consumer.accept(bone, this::renderItem));
    }

    private void renderItem(RenderPassInfo<LivingEntityRenderState> pass, GeoBone bone, SubmitNodeCollector renderTasks) {
        LivingEntityRenderState state = pass.renderState();
        ItemStackRenderState item = state.getGeckolibData(ITEM);
        Grip grip = state.getGeckolibData(GRIP);
        if (item == null || item.isEmpty() || grip == null) return;
        PoseStack poseStack = pass.poseStack();
        poseStack.pushPose();
        switch (grip) {
            case BOW -> poseStack.rotateDegrees(Axis.XP, this.pose.bow());
            case STAB -> {
                Vector3f offset = this.pose.stabOffset();
                poseStack.translate(offset.x(), offset.y(), offset.z());
                poseStack.rotateDegrees(Axis.XP, this.pose.stab());
            }
            case SLASH -> poseStack.rotateDegrees(Axis.XP, this.pose.slash());
        }
        item.submit(poseStack, renderTasks, pass.packedLight(), OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
