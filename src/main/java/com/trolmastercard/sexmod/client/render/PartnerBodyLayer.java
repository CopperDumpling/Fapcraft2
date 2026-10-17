package com.trolmastercard.sexmod.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.entity.GirlEntity;
import java.util.function.BiConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import org.jspecify.annotations.Nullable;

/**
 * Draws the scene partner's body: the {@code steve} bone tree inside every girl model,
 * textured with the partner's skin (1.12.2 drew it in a second pass in {@code GirlRenderer}).
 * <ul>
 *   <li>Outside a scene step that involves the player, the tree is hidden.</li>
 *   <li>Slim (Alex) skins get the {@code *Alex} arm bones, wide skins the {@code *Steve} ones.</li>
 * </ul>
 */
public class PartnerBodyLayer<T extends GirlEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
    public static final String ROOT_BONE = "steve";
    private static final DataTicket<Identifier> SKIN = DataTicket.create("sexmod_partner_skin", Identifier.class);
    private static final DataTicket<Boolean> SLIM = DataTicket.create("sexmod_partner_slim", Boolean.class);

    public PartnerBodyLayer(GeoRenderer<T, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        if (girl.getAction().hasPlayer && girl.getPartner() instanceof AbstractClientPlayer partner) {
            renderState.addGeckolibData(SKIN, PartnerSkins.get(partner));
            renderState.addGeckolibData(SLIM, partner.getSkin().model() == PlayerModelType.SLIM);
        }
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        // the main pass never draws the partner's body; this layer does
        pass.addBoneUpdater((info, snapshots) -> info.model().getBone(ROOT_BONE).ifPresent(root ->
            GeoCubes.forEachBone(root, bone -> snapshots.get(bone).skipRender(true))));
    }

    @Override
    public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> pass, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
        LivingEntityRenderState state = pass.renderState();
        if (!pass.willRender() || !state.hasGeckolibData(SKIN)) return;
        boolean slim = state.getOrDefaultGeckolibData(SLIM, false);
        String hiddenArms = slim ? "Steve" : "Alex";
        pass.model().getBone(ROOT_BONE).ifPresent(root -> visit(root, hiddenArms, consumer));
    }

    private void visit(GeoBone bone, String hiddenArms, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
        // e.g. leftArmAlex / leftLowerArmSteve; the steveLeftArm parents stay visible
        if (bone.name().endsWith(hiddenArms)) return;
        if (GeoCubes.hasCubes(bone)) {
            consumer.accept(bone, this::renderBone);
        }
        for (GeoBone child : bone.children()) {
            visit(child, hiddenArms, consumer);
        }
    }

    private void renderBone(RenderPassInfo<LivingEntityRenderState> pass, GeoBone bone, SubmitNodeCollector renderTasks) {
        Identifier skin = pass.renderState().getGeckolibData(SKIN);
        if (skin == null) return;
        // back faces culled, as 1.12.2 drew it: in first person the camera sits inside the head
        RenderType renderType = RenderTypes.entityCutoutCull(skin);
        int light = pass.packedLight();
        int overlay = pass.packedOverlay();
        int color = pass.renderColor();
        // the steve bones are UV-mapped on a 64x64 skin, the model's own texture size: no rescale
        renderTasks.submitCustomGeometry(pass.poseStack(), renderType, (pose, buffer) -> {
            PoseStack poseStack = pass.poseStack();
            poseStack.pushPose();
            poseStack.last().set(pose);
            GeoCubes.renderBone(bone, poseStack, buffer, light, overlay, color, 0.0F);
            poseStack.popPose();
        });
    }
}
