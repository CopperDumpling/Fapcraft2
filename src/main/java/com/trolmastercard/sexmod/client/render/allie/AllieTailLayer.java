package com.trolmastercard.sexmod.client.render.allie;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.trolmastercard.sexmod.entity.allie.AllieEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/** Every half second a portal particle drifts off one of her eight tail bones, at random (1.12.2 {@code spawnRandomParticles}). Draws nothing. */
public class AllieTailLayer extends GeoRenderLayer<AllieEntity, Void, LivingEntityRenderState> {
    private static final DataTicket<String> BONE = DataTicket.create("sexmod_allie_tail_bone", String.class);
    private static final int TAIL_BONES = 8;

    public AllieTailLayer(GeoRenderer<AllieEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(AllieEntity allie, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        if (allie.tailParticleDue()) renderState.addGeckolibData(BONE, "tail" + allie.getRandom().nextInt(TAIL_BONES));
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        String bone = pass.renderState().getGeckolibData(BONE);
        if (bone == null) return;
        pass.addBonePositionListener(bone, (worldPos, modelPos, localPos) -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (worldPos == null || level == null) return;
            RandomSource random = level.getRandom();
            level.addParticle(ParticleTypes.PORTAL, worldPos.x, worldPos.y, worldPos.z,
                random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, random.nextGaussian() * 0.01);
        });
    }
}
