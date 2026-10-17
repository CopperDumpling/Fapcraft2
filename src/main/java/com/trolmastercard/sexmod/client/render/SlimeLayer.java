package com.trolmastercard.sexmod.client.render;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.slime.SlimeEntity;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * The slime girl's extra bones (1.12.2 {@code SlimeNpcModel.setLivingAnimations}): the slime
 * puddle she lies in ({@code bedSlime}, {@code bedSlimeLayer}) shows only during doggy, and her
 * hat takes the head's pose (it isn't parented to the head). Draws nothing.
 */
public class SlimeLayer extends GeoRenderLayer<SlimeEntity, Void, LivingEntityRenderState> {
    private static final DataTicket<Boolean> PUDDLE = DataTicket.create("sexmod_slime_puddle", Boolean.class);
    private static final Set<GirlAction> PUDDLE_STEPS = EnumSet.of(
        GirlAction.STARTDOGGY, GirlAction.WAITDOGGY, GirlAction.DOGGYSTART, GirlAction.DOGGYSLOW, GirlAction.DOGGYFAST, GirlAction.DOGGYCUM);

    public SlimeLayer(GeoRenderer<SlimeEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(SlimeEntity slime, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(PUDDLE, PUDDLE_STEPS.contains(slime.getAction()));
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        boolean puddle = pass.renderState().getOrDefaultGeckolibData(PUDDLE, false);
        pass.addBoneUpdater((info, snapshots) -> {
            if (!puddle) {
                for (String name : new String[]{"bedSlime", "bedSlimeLayer"}) {
                    info.model().getBone(name).ifPresent(bone -> snapshots.get(bone).skipRender(true).skipChildrenRender(true));
                }
            }
            info.model().getBone("head").ifPresent(head -> info.model().getBone("hat").ifPresent(hat -> {
                BoneSnapshot from = snapshots.get(head);
                snapshots.get(hat)
                    .setRotation(from.getRotX(), from.getRotY(), from.getRotZ())
                    .setTranslation(from.getTranslateX(), from.getTranslateY(), from.getTranslateZ());
            }));
        });
    }
}
