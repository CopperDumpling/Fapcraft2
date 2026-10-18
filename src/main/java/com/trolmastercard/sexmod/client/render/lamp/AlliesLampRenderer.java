package com.trolmastercard.sexmod.client.render.lamp;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.client.ClientHooks;
import com.trolmastercard.sexmod.item.AlliesLampItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Allie's lamp (1.12.2 {@code AlliesLampRenderer}), textured with the local player's skin
 * ({@link LampSkin}). Its arm bones show only while the local player rubs a lamp and sees it in
 * first person: the player's own arms rubbing it.
 */
public class AlliesLampRenderer extends GeoItemRenderer<AlliesLampItem> {
    private static final DataTicket<Boolean> SHOW_ARMS = DataTicket.create("sexmod_lamp_arms", Boolean.class);
    private static final List<String> ARMS = List.of("leftArm", "rightArm");

    public AlliesLampRenderer() {
        super(new GeoModel<>() {
            private final Identifier asset = Fapcraft.id("allie/lamp");

            @Override
            public Identifier getModelResource(GeoRenderState renderState) {
                return this.asset;
            }

            @Override
            public Identifier getTextureResource(GeoRenderState renderState) {
                return LampSkin.get();
            }

            @Override
            public Identifier getAnimationResource(AlliesLampItem animatable) {
                return this.asset;
            }
        });
    }

    @Override
    public void addRenderData(AlliesLampItem lamp, @Nullable RenderData renderData, GeoRenderState renderState, float partialTick) {
        boolean firstPerson = renderData != null && renderData.renderPerspective().firstPerson()
            && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        renderState.addGeckolibData(SHOW_ARMS, firstPerson && ClientHooks.isRubbingLamp());
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> pass, BoneSnapshots snapshots) {
        if (pass.renderState().getOrDefaultGeckolibData(SHOW_ARMS, false)) return;
        for (String arm : ARMS) {
            snapshots.get(arm).ifPresent(snapshot -> snapshot.skipRender(true).skipChildrenRender(true));
        }
    }
}
