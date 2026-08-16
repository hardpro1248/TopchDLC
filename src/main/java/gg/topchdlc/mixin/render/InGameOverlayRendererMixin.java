package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void injectRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        if (Removals.INSTANCE.removals.get(Removals.Removal.FireOverlay)) {
            ci.cancel();
        }
    }
    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void injectRenderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (Removals.INSTANCE.removals.get(Removals.Removal.BlockOverlay)) {
            ci.cancel();
        }
    }
    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void injectWaterOverlay(MinecraftClient client, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (Removals.INSTANCE.removals.get(Removals.Removal.Under)) {
            ci.cancel();
        }
    }
}
