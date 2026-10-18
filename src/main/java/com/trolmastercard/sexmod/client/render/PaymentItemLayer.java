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
import com.trolmastercard.sexmod.entity.SceneOption;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * While a girl is being paid, draws the price items fanned out on her {@code itemRenderer}
 * bone, one item per unit of the price (1.12.2 {@code GirlRenderer.renderTradeOverlay}).
 */
public class PaymentItemLayer<T extends GirlEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
    public static final String BONE = "itemRenderer";
    private static final DataTicket<ItemStackRenderState> ITEM = DataTicket.create("sexmod_payment_item", ItemStackRenderState.class);
    private static final DataTicket<Integer> COUNT = DataTicket.create("sexmod_payment_count", Integer.class);
    private static final DataTicket<Float> SCALE = DataTicket.create("sexmod_payment_scale", Float.class);
    private final ItemModelResolver itemModelResolver;

    public PaymentItemLayer(EntityRendererProvider.Context context, GeoRenderer<T, Void, LivingEntityRenderState> renderer) {
        super(renderer);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        SceneOption option = girl.currentOption();
        if (girl.getAction() != GirlAction.PAYMENT || option == null || option.price().isEmpty()) return;
        ItemStack single = option.price().copyWithCount(1);
        renderState.addGeckolibData(ITEM, RenderUtil.createRenderStateForItem(single, this.itemModelResolver, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, girl));
        renderState.addGeckolibData(COUNT, option.priceCount());
        renderState.addGeckolibData(SCALE, girl.paymentItemScale());
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> pass, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
        if (!pass.willRender() || !pass.renderState().hasGeckolibData(ITEM)) return;
        pass.model().getBone(BONE).ifPresent(bone -> consumer.accept(bone, this::renderItems));
    }

    private void renderItems(RenderPassInfo<LivingEntityRenderState> pass, GeoBone bone, SubmitNodeCollector renderTasks) {
        LivingEntityRenderState state = pass.renderState();
        ItemStackRenderState item = state.getGeckolibData(ITEM);
        if (item == null || item.isEmpty()) return;
        int count = Math.min(state.getOrDefaultGeckolibData(COUNT, 1), 3);
        PoseStack poseStack = pass.poseStack();
        float scale = state.getOrDefaultGeckolibData(SCALE, 1.0F);
        for (int i = 0; i < count; i++) {
            poseStack.pushPose();
            poseStack.rotateDegrees(Axis.ZP, 2.5F);
            // the second and third items fan out to either side of the first
            if (i == 1) {
                poseStack.rotateDegrees(Axis.ZP, -15.0F);
                poseStack.translate(0.0F, 0.0F, -0.025F);
            } else if (i == 2) {
                poseStack.rotateDegrees(Axis.ZP, 15.0F);
                poseStack.translate(0.0F, 0.0F, 0.025F);
            }
            poseStack.scale(scale, scale, scale);
            item.submit(poseStack, renderTasks, pass.packedLight(), OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
