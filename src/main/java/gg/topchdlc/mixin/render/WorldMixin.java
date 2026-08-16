package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.CustomWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    @Inject(method = "getRainGradient", cancellable = true, at = @At("HEAD"))
    private void hookGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        float gradient = CustomWorld.INSTANCE.getRainGradient();
        if (gradient != -1f) cir.setReturnValue(gradient);
    }

    @Inject(method = "getThunderGradient", cancellable = true, at = @At("HEAD"))
    private void hookGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        float gradient = CustomWorld.INSTANCE.getThunderGradient();
        if (gradient != -1f) cir.setReturnValue(gradient);
    }
}
