package com.trolmastercard.sexmod.client.mixin;

import com.trolmastercard.sexmod.client.scene.BoyCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Puts the first-person camera on a girl's {@code boyCam} bone while she carries the player ({@link BoyCamera}). */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void sexmod$boyCam(float partialTicks, CallbackInfo ci) {
        Vec3 position = BoyCamera.position();
        if (position != null) setPosition(position);
    }
}
