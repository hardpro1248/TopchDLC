package gg.topchdlc.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventPost3D;
import gg.topchdlc.api.events.list.EventRenderEntity;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import gg.topchdlc.vse.shutki.module.modules.impl.render.ShaderFog;
import gg.topchdlc.vse.shutki.module.modules.impl.render.TargetESP;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    @Shadow @Final private DefaultFramebufferSet framebufferSet;
    @Inject(method = "renderBlockDamage", at = @At("TAIL"))
    private void renderBlockDamage(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, WorldRenderState renderStates, CallbackInfo ci) {
        if (mc.player != null && mc.world != null) {
            Client.EVENTS.post(Event3D.build(new MatrixStack(), immediate));
            TargetESP.INSTANCE.render(matrices, immediate);
        }
    }
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fogBuffer, CallbackInfo ci) {
        if (ShaderFog.INSTANCE.isEnabled()) {
            FramePass framePass = frameGraphBuilder.createPass("shader_fog_sky");
            this.framebufferSet.mainFramebuffer = framePass.transfer(this.framebufferSet.mainFramebuffer);
            framePass.setRenderer(() -> {
                RenderSystem.setShaderFog(fogBuffer);
                ShaderFog.INSTANCE.renderShader();
            });

            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void onRenderClouds(FrameGraphBuilder frameGraphBuilder, CloudRenderMode mode, Vec3d cameraPos, long l, float f, int i, float g, CallbackInfo ci) {
        if (ShaderFog.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
    @Inject(method = "pushEntityRenders", at = @At("HEAD"))
    private void renderEntities(MatrixStack matrices, WorldRenderState renderStates, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        Client.EVENTS.post(EventRenderEntity.build(immediate, matrices));
    }



    @Inject(method = "renderTargetBlockOutline", at = @At("TAIL"))
    private void renderEntitiesPost(VertexConsumerProvider.Immediate immediate, MatrixStack matrices, boolean renderBlockOutline, WorldRenderState renderStates, CallbackInfo ci) {
        if (renderBlockOutline && mc.player != null && mc.world != null) {
            Client.EVENTS.post(EventPost3D.build(matrices, immediate));
        }
    }

    @Inject(method = "hasBlindnessOrDarkness", at = @At("HEAD"), cancellable = true)
    private void hasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> info) {
        if (Removals.INSTANCE.isEnabled()) {
            boolean removeBlindness = Removals.INSTANCE.removals.get(Removals.Removal.Blindness);
            boolean removeDarkness = Removals.INSTANCE.removals.get(Removals.Removal.Darkness);
            if (removeBlindness || removeDarkness) {
                info.setReturnValue(false);
            }
        }
    }
}