package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.render.state.WeatherRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create by daun kvass
 */
@Mixin(WeatherRendering.class)
public class WeatherEffectMixin {

    @Inject(method = "renderPrecipitation", at = @At("HEAD"), cancellable = true)
    private void renderWeather(VertexConsumerProvider vertexConsumers, Vec3d pos, WeatherRenderState state, CallbackInfo ci) {
        if (Chlen()) ci.cancel();
    }

    @Inject(method = "addParticlesAndSound", at = @At("HEAD"), cancellable = true)
    private void WeatherSound(ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode, int weatherRadius, CallbackInfo ci) {
        if (Chlen()) ci.cancel();
    }

    @Unique
    private boolean Chlen() {
        return Removals.INSTANCE != null
                && Removals.INSTANCE.isEnabled()
                && Removals.INSTANCE.particle.get(Removals.Particle.RainP);
    }
}