package gg.topchdlc.mixin.client;

import gg.topchdlc.Client;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create by daun kvass
 */
@Mixin(RenderTickCounter.Dynamic.class)
public class RenderTickCounterMixin {

    @Shadow private float dynamicDeltaTicks;

    @Inject(method = "beginRenderTick(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;lastTimeMillis:J", shift = At.Shift.AFTER))
    private void tick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        this.dynamicDeltaTicks *= Client.TIMER;
    }
}
