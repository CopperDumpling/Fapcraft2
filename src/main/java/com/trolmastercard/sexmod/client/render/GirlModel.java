package com.trolmastercard.sexmod.client.render;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.entity.GirlEntity;
import net.minecraft.resources.Identifier;

/**
 * A girl's GeckoLib assets, by name: {@code geckolib/models/<name>/<name>dressed|nude.geo.json},
 * {@code geckolib/animations/<name>/<name>.animation.json} and
 * {@code textures/entity/<name>/<name>.png} (the 1.12.2 layout).
 */
public class GirlModel<T extends GirlEntity> extends GeoModel<T> {
    private final Identifier dressed;
    private final Identifier nude;
    private final Identifier animations;
    private final Identifier texture;

    public GirlModel(String name) {
        this.dressed = Fapcraft.id(name + "/" + name + "dressed");
        this.nude = Fapcraft.id(name + "/" + name + "nude");
        this.animations = Fapcraft.id(name + "/" + name);
        this.texture = Fapcraft.id("textures/entity/" + name + "/" + name + ".png");
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return renderState.getOrDefaultGeckolibData(GirlRenderData.DRESSED, true) ? this.dressed : this.nude;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return this.texture;
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return this.animations;
    }
}
