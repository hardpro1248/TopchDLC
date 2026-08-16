package gg.topchdlc.mixin.client;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.impl.render.HandShaderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {

    @Inject(method = "limitDisplayFPS", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onLimitDisplayFPS(int fps, CallbackInfo ci) {
        if (!Client.IS_WINDOW_FOCUSED) {
            try {
                Thread.sleep(33);
            } catch (InterruptedException ignored) {}
        }
        ci.cancel();
    }

    @Inject(method = "bindDefaultUniforms(Lcom/mojang/blaze3d/systems/RenderPass;)V", at = @At("TAIL"))
    private static void bindCustomChamsUniform(RenderPass renderPass, CallbackInfo ci) {
        boolean handShaderActive = HandShaderModule.INSTANCE.isEnabled() && HandShaderModule.INSTANCE.fillItem.get();

        if (handShaderActive || HandShaderModule.isRenderingShader) {
            var chamsBuffer = ClientPipelines.getOrCreateUniformBuffer();
            if (chamsBuffer != null) {
                renderPass.setUniform("ShaderFogData", chamsBuffer);
            }
        }
    }
}