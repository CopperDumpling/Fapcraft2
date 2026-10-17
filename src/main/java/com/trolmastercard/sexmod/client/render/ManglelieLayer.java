package com.trolmastercard.sexmod.client.render;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.trolmastercard.sexmod.entity.manglelie.ManglelieEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Manglelie's skirt (1.12.2 {@code ManglelieNpcModel.animateModel} and
 * {@code ManglelieRenderer.applyBoneTransform}), as worn outside the threesome:
 * <ul>
 *   <li>the skirt shows, with the skirted hips; the bare cheeks and sides under it are hidden;</li>
 *   <li>the skirt's strips follow her body: strips 17-34 rise with the cheek on their side
 *   (0.01 per degree it is raised), and the first piece of strips 1-10 swings with the leg on
 *   its side.</li>
 * </ul>
 * Draws nothing.
 */
public class ManglelieLayer extends GeoRenderLayer<ManglelieEntity, Void, LivingEntityRenderState> {
    private static final List<String> HIDDEN = List.of("cheekRBelowSkirt", "cheekLBelowSkirt", "sideRNoSkirt", "sideLNoSkirt");

    public ManglelieLayer(GeoRenderer<ManglelieEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        pass.addBoneUpdater((info, snapshots) -> {
            for (String name : HIDDEN) {
                info.model().getBone(name).ifPresent(bone -> snapshots.get(bone).skipRender(true).skipChildrenRender(true));
            }
            for (int strip = 17; strip < 35; strip++) {
                float cheek = (float) Math.toDegrees(rotX(info, snapshots, strip < 26 ? "cheekL" : "cheekR"));
                if (cheek < 0.0F) continue;
                for (int piece = 0; piece < 3; piece++) {
                    info.model().getBone("skirt_" + strip + "_" + piece).ifPresent(bone -> {
                        BoneSnapshot snapshot = snapshots.get(bone);
                        snapshot.setTranslateY(snapshot.getTranslateY() + cheek * 0.01F);
                    });
                }
            }
            for (int strip = 1; strip < 11; strip++) {
                float leg = rotX(info, snapshots, strip < 6 ? "legR" : "legL");
                if (leg < 0.0F) continue;
                info.model().getBone("skirt_" + strip + "_1").ifPresent(bone -> snapshots.get(bone)
                    .setRotX(leg)
                    .setTranslateY((float) Math.toRadians(Math.toDegrees(leg) * 0.03)));
            }
        });
    }

    private static float rotX(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots snapshots, String name) {
        Optional<GeoBone> bone = info.model().getBone(name);
        return bone.map(b -> snapshots.get(b).getRotX()).orElse(-1.0F);
    }
}
