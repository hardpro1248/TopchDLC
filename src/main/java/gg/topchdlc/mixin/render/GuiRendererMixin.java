package gg.topchdlc.mixin.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.texture.TextureSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.ClientPipelines;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin implements MinecraftHolder {
    @Shadow protected abstract void enableScissor(ScreenRect scissorArea, RenderPass pass);

    @Inject(method = "render(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V", at = @At("HEAD"), cancellable = true)
    private void client$render(GuiRenderer.Draw draw, RenderPass pass, GpuBuffer indexBuffer, VertexFormat.IndexType indexType, CallbackInfo ci) {
        if (draw.pipeline() == ClientPipelines.HUD.getRenderPipeline()) {
            ci.cancel();

            Client.RENDERER.getCrenderSystem().render(CRenderSystem.RenderLayer.ESP);

            int mouseX = (int) mc.mouse.getScaledX(mc.getWindow());
            int mouseY = (int) mc.mouse.getScaledY(mc.getWindow());

            Client.HUD.render(mouseX, mouseY);
            if (Client.BUILDER_TRAINING != null && Client.BUILDER_TRAINING.isOpened()) {
                Client.BUILDER_TRAINING.render(mouseX, mouseY);
            }
            if (Client.GLASS_GUI != null) {
                Client.GLASS_GUI.render(mouseX, mouseY);
            }

            Client.RENDERER.getCrenderSystem().render(CRenderSystem.RenderLayer.POST);
            Client.RENDERER.getCrenderSystem().render(CRenderSystem.RenderLayer.BLUR);
            Client.RENDERER.getCrenderSystem().render(CRenderSystem.RenderLayer.HUD);
        } else {
            ci.cancel();

            pass.setPipeline(draw.pipeline());
            pass.setVertexBuffer(0, draw.vertexBuffer());

            ScreenRect screenRect = draw.scissorArea();
            if (screenRect != null) {
                this.enableScissor(screenRect, pass);
            } else {
                pass.disableScissor();
            }

            TextureSetup textures = draw.textureSetup();

            if (textures.texure0() != null && textures.sampler0() != null) {
                pass.bindTexture("Sampler0", textures.texure0(), textures.sampler0());
            }

            if (textures.texure1() != null && textures.sampler1() != null) {
                pass.bindTexture("Sampler1", textures.texure1(), textures.sampler1());
            }

            if (textures.texure2() != null && textures.sampler2() != null) {
                pass.bindTexture("Sampler2", textures.texure2(), textures.sampler2());
            }

            pass.setIndexBuffer(indexBuffer, indexType);
            pass.drawIndexed(draw.baseVertex(), 0, draw.indexCount(), 1);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void client$prepare(CallbackInfo ci) {
        Client.RENDERER.prepare();
    }
}