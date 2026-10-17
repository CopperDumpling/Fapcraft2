package com.trolmastercard.sexmod.client.render.slime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.entity.slime.WildSlimeEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * The wild slime (1.12.2 {@code WildSlimeRenderer}): its tailed body on the vanilla slime texture,
 * squished like a vanilla slime, with the vanilla tiny slime's face drawn over it
 * ({@code WildSlimeFaceLayer}).
 */
public class WildSlimeRenderer extends AbstractCubeMobRenderer<WildSlimeEntity, SlimeRenderState, WildSlimeModel> {
    public WildSlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new WildSlimeModel(context.bakeLayer(WildSlimeModel.LAYER)));
        addLayer(new FaceLayer(this, new SlimeModel(context.bakeLayer(ModelLayers.SLIME))));
    }

    @Override
    protected void scale(SlimeRenderState state, PoseStack poseStack) {
        downscaleSlightly(poseStack);
        super.scale(state, poseStack);
    }

    @Override
    public Identifier getTextureLocation(SlimeRenderState state) {
        return SlimeRenderer.SLIME_LOCATION;
    }

    @Override
    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }

    /** The vanilla tiny slime's body, eyes and mouth over the tailed body. */
    private static final class FaceLayer extends RenderLayer<SlimeRenderState, WildSlimeModel> {
        private final SlimeModel face;

        FaceLayer(RenderLayerParent<SlimeRenderState, WildSlimeModel> parent, SlimeModel face) {
            super(parent);
            this.face = face;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, SlimeRenderState state, float yRot, float xRot) {
            if (state.isInvisible) return;
            // 1.12.2 blended it over the body
            collector.order(1).submitModel(this.face, state, poseStack, RenderTypes.entityTranslucent(SlimeRenderer.SLIME_LOCATION),
                lightCoords, LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
        }
    }
}
