package com.trolmastercard.sexmod.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.trolmastercard.sexmod.Fapcraft;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Copies of player skins prepared for the girls' built-in {@code steve} body (1.12.2
 * {@code GirlRenderer.getTintedSkinTexture}).
 * <p>
 * The body's genital cubes have zero-size UVs that sample single pixels in the skin's unused
 * top-left corner, so each copy gets 1.12's skin-tone and blush colours painted there.
 * Render thread only.
 */
public final class PartnerSkins {
    private static final int BASE_SKIN = argb(245, 199, 165);
    private static final int BLUSH = argb(245, 157, 169);
    private static final Identifier FALLBACK = Fapcraft.id("textures/player/steve.png");

    /** Source skin texture -> prepared copy. */
    private static final Map<Identifier, Identifier> PREPARED = new HashMap<>();

    private PartnerSkins() {
    }

    public static Identifier get(AbstractClientPlayer player) {
        Identifier source = player.getSkin().body().texturePath();
        Identifier prepared = PREPARED.get(source);
        if (prepared != null) return prepared;
        NativeImage image = readPixels(source);
        if (image == null) {
            // a downloaded skin may still be loading; use the bundled steve until it is
            Identifier fallback = PREPARED.get(FALLBACK);
            if (fallback != null) return fallback;
            image = readPixels(FALLBACK);
            if (image == null) return FALLBACK;
            source = FALLBACK;
        }
        prepared = Fapcraft.id("partner_skin/" + Integer.toHexString(source.hashCode()));
        paint(image);
        final NativeImage pixels = image;
        final String label = prepared.toString();
        Minecraft.getInstance().getTextureManager().register(prepared, new DynamicTexture(() -> label, pixels));
        PREPARED.put(source, prepared);
        return prepared;
    }

    private static void paint(NativeImage image) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 7; x++) {
                image.setPixel(x, y, x < 4 ? BASE_SKIN : BLUSH);
            }
        }
    }

    /** A copy of a loaded texture's pixels: a downloaded skin, or a resource texture; null if not loaded yet. */
    public static @Nullable NativeImage readPixels(Identifier texture) {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        AbstractTexture loaded = textures.getTexture(texture);
        if (loaded instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            return dynamic.getPixels().mappedCopy(pixel -> pixel);
        }
        // default skins and our fallback are plain resource textures
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture)) {
            return NativeImage.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static int argb(int r, int g, int b) {
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
