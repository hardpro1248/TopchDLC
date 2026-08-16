package gg.topchdlc.mixin.render;

import gg.topchdlc.api.render.system.ClientPipelines;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OutlineVertexConsumerProvider.class)
public class OutlineVertexConsumersMixin {
    @Inject(method = "draw", at = @At("TAIL"))
    private void draw(CallbackInfo ci) {
        ClientPipelines.customVertexConsumerProvider.draw();
    }
}
