package gg.topchdlc.mixin.render;

import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.api.render.system.CustomVertexConsumerProvider;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilderStorage.class)
public class BufferBuilderStorageMixin {
    @Shadow
    @Final
    private VertexConsumerProvider.Immediate entityVertexConsumers;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(int maxBlockBuildersPoolSize, CallbackInfo ci) {
        ClientPipelines.customVertexConsumerProvider = new CustomVertexConsumerProvider(this.entityVertexConsumers);
    }
}
