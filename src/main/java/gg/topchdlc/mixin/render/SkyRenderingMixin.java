package gg.topchdlc.mixin.render;

import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.render.CustomWorld;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void onRenderStars(CallbackInfo ci) {
        if (CustomWorld.INSTANCE.isEnabled() && CustomWorld.INSTANCE.skyx.get()) {
            ci.cancel();
        }
    }
    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
    private void onRenderCelestial(CallbackInfo ci) {
        if (CustomWorld.INSTANCE.isEnabled() && CustomWorld.INSTANCE.skyx.get()) {
            ci.cancel();
        }
    }

}