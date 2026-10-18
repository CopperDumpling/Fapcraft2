package com.trolmastercard.sexmod.client.render.lamp;

import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.renderer.GeoItemRenderer;
import org.jspecify.annotations.Nullable;

/** Hands GeckoLib the lamp's renderer, created on first use (physical client only). */
public class AlliesLampRenderProvider implements GeoRenderProvider {
    private @Nullable AlliesLampRenderer renderer;

    @Override
    public @Nullable GeoItemRenderer<?> getGeoItemRenderer() {
        if (this.renderer == null) this.renderer = new AlliesLampRenderer();
        return this.renderer;
    }
}
