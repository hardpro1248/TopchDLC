package gg.topchdlc.mixin.client;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTickCounter.Dynamic.class)
public class MixinDynamic {

    @Shadow private float dynamicDeltaTicks;

    @Shadow private long lastTimeMillis;

    @Shadow private float tickProgress;

    @Shadow @Final private FloatUnaryOperator targetMillisPerTick;

    @Shadow @Final private float tickTime;

//    @Inject(method = "beginRenderTick(J)I", at = @At("HEAD"), cancellable = true)
//    private void beginRenderTickHook(long timeMillis, CallbackInfoReturnable<Integer> cir) {
//        if (Client.TIMER == 1.0) return;
//
//        this.dynamicDeltaTicks = (float)(timeMillis - this.lastTimeMillis) / this.targetMillisPerTick.apply(1000.0f / (Client.TIMER * 20.0f));
//        this.lastTimeMillis = timeMillis;
//        this.tickProgress = this.tickProgress + this.dynamicDeltaTicks;
//        int i = (int)this.tickProgress;
//        this.tickProgress -= i;
//        cir.setReturnValue(i);
//    }
}