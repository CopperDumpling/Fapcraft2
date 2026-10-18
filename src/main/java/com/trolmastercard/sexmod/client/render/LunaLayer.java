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
import com.trolmastercard.sexmod.entity.luna.LunaEntity;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Luna's extras (1.12.2 {@code LunaRenderer}):
 * <ul>
 *   <li>unless she is held in place for a scene, her hair hangs straight while she tilts her head:
 *   it turns back against the head's pitch, and the back and side strands shift with it;</li>
 *   <li>once her catch has reached her, she holds it in her {@code offhand} bone, shrinking as
 *   she eats it.</li>
 * </ul>
 */
public class LunaLayer extends GeoRenderLayer<LunaEntity, Void, LivingEntityRenderState> {
    private static final DataTicket<Boolean> ANCHORED = DataTicket.create("sexmod_luna_anchored", Boolean.class);
    private static final DataTicket<ItemStackRenderState> CATCH = DataTicket.create("sexmod_luna_catch", ItemStackRenderState.class);
    private static final DataTicket<Float> CATCH_SCALE = DataTicket.create("sexmod_luna_catch_scale", Float.class);
    /** Head pitch at which the hair shifts by its full amount. */
    private static final float FULL_TILT = 45.0F * Mth.DEG_TO_RAD;

    private final ItemModelResolver itemModelResolver;

    public LunaLayer(EntityRendererProvider.Context context, GeoRenderer<LunaEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public void addRenderData(LunaEntity luna, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(ANCHORED, luna.isAnchored());
        ItemStack caught = luna.getCatch();
        if (!caught.isEmpty() && luna.catchFlight() >= 1.0F) {
            renderState.addGeckolibData(CATCH, RenderUtil.createRenderStateForItem(caught, this.itemModelResolver, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, luna));
            renderState.addGeckolibData(CATCH_SCALE, luna.catchScale());
        }
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        if (pass.renderState().getOrDefaultGeckolibData(ANCHORED, false)) return;
        // 1.12.2 set these bones' whole rotation and position; a GeckoLib 5 snapshot adds rotation to the model's own
        pass.addBoneUpdater((info, snapshots) -> info.model().getBone("head").ifPresent(head -> {
            float pitch = head.baseRotX() + snapshots.get(head).getRotX();
            float tilt = pitch / FULL_TILT;
            float back = Mth.lerp(tilt, 0.0F, 0.75F);
            float side = Mth.lerp(tilt, 0.0F, 1.3F);
            info.model().getBone("backHair").ifPresent(bone -> snapshots.get(bone).setTranslateZ(back).setTranslateY(back).setRotX(-pitch - bone.baseRotX()));
            for (String name : new String[]{"sideHairR", "sideHairL"}) {
                info.model().getBone(name).ifPresent(bone -> snapshots.get(bone).setTranslateZ(-side).setTranslateY(side).setRotX(-pitch - bone.baseRotX()));
            }
            for (String name : new String[]{"frontHairL", "frontHairR"}) {
                info.model().getBone(name).ifPresent(bone -> snapshots.get(bone).setRotX(-pitch - bone.baseRotX()));
            }
        }));
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> pass, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
        if (!pass.willRender() || !pass.renderState().hasGeckolibData(CATCH)) return;
        pass.model().getBone("offhand").ifPresent(bone -> consumer.accept(bone, this::renderCatch));
    }

    private void renderCatch(RenderPassInfo<LivingEntityRenderState> pass, GeoBone bone, SubmitNodeCollector renderTasks) {
        LivingEntityRenderState state = pass.renderState();
        ItemStackRenderState item = state.getGeckolibData(CATCH);
        if (item == null || item.isEmpty()) return;
        float scale = state.getOrDefaultGeckolibData(CATCH_SCALE, 1.0F);
        PoseStack poseStack = pass.poseStack();
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.XP, 90.0F);
        poseStack.scale(scale, scale, scale);
        item.submit(poseStack, renderTasks, pass.packedLight(), OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
