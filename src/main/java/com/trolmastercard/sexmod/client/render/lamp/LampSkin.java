package com.trolmastercard.sexmod.client.render.lamp;

import com.mojang.blaze3d.platform.NativeImage;
import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.client.render.PartnerSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * The lamp's texture (1.12.2 {@code AlliesLampModel.getTextureLocation}): the local player's skin,
 * so the arms rubbing it are theirs, with the lamp's colours painted into the skin's unused
 * top-left corner, where the lamp's cubes take theirs from. Render thread only.
 */
final class LampSkin {
    /** A steve skin with the lamp's colours, for when the player's skin can't be read. */
    private static final Identifier FALLBACK = Fapcraft.id("textures/entity/allie/lamp.png");
    private static final Identifier PREPARED = Fapcraft.id("lamp_skin");
    private static final int CYAN = 0xFFB9FEFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private static @Nullable Identifier preparedFrom;

    private LampSkin() {
    }

    static Identifier get() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return FALLBACK;
        Identifier source = mc.player.getSkin().body().texturePath();
        if (source.equals(preparedFrom)) return PREPARED;
        NativeImage image = PartnerSkins.readPixels(source);
        if (image == null) return FALLBACK; // a downloaded skin may still be loading
        for (int y = 0; y < 2; y++) {
            image.setPixel(0, y, CYAN);
            image.setPixel(1, y, CYAN);
            image.setPixel(2, y, WHITE);
            image.setPixel(3, y, BLACK);
        }
        mc.getTextureManager().register(PREPARED, new DynamicTexture(PREPARED::toString, image));
        preparedFrom = source;
        return PREPARED;
    }
}
