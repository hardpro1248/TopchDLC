package gg.topchdlc.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.topchdlc.api.events.list.EventGetFov;
import gg.topchdlc.vse.shutki.module.modules.impl.player.NoEntityTrace;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import gg.topchdlc.vse.shutki.module.modules.impl.render.CameraCustomer;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow public abstract float getFarPlaneDistance();

    @Shadow @Final private MinecraftClient client;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Shadow
    public abstract float getSkyDarkness(float tickProgress);

    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void onUpdateCrosshairTarget(float tickProgress, CallbackInfo ci) {
        if (NoEntityTrace.INSTANCE.isEnabled() && (this.client.player.getMainHandStack().isIn(ItemTags.PICKAXES) || !NoEntityTrace.INSTANCE.ponly.get())) {
            if (this.client.player.getMainHandStack().isIn(ItemTags.SWORDS) && NoEntityTrace.INSTANCE.noSword.get()) return;
            Entity entity = this.client.getCameraEntity();
            if (entity != null) {
                double blockInteractionRange = this.client.player.getBlockInteractionRange();
                HitResult hitResult = entity.raycast(blockInteractionRange, tickProgress, false);
                this.client.crosshairTarget = hitResult;
                this.client.targetedEntity = null;
            }
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;incrementFrame()V"))
    public void renderOverlay(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        int mouseX = (int) this.client.mouse.getScaledX(this.client.getWindow());
        int mouseY = (int) this.client.mouse.getScaledY(this.client.getWindow());
        Client.GLASS_GUI.render(mouseX, mouseY);
        if (Client.AUTOBUY_CONFIG != null) {
            Client.AUTOBUY_CONFIG.render(mouseX, mouseY);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void render(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        Client.RENDERER.getCrenderSystem().postRender();
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo info) {
        if (floatingItem.getItem() == Items.TOTEM_OF_UNDYING && Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get(Removals.Removal.TotemPop)) {
            info.cancel();
        }
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("HEAD"), cancellable = true)
    public void getBasicProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        fovDegrees = Client.EVENTS.post(new EventGetFov(fovDegrees)).fov;
        Matrix4f matrix4f = new Matrix4f();
        if (Client.IS_PANIC) return;
        cir.cancel();
        float aspect = (float)this.client.getWindow().getFramebufferWidth() / (float)this.client.getWindow().getFramebufferHeight();
        if (CameraCustomer.INSTANCE.isEnabled() && CameraCustomer.INSTANCE.aspectratio.get()) {
            aspect = CameraCustomer.INSTANCE.widthSlider.get();
        }

        cir.setReturnValue(matrix4f.perspective(
                (fovDegrees * (float) (Math.PI / 180.0)),
                aspect,
                0.05F,
                this.getFarPlaneDistance())
        );
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    public void tiltViewWhenHurt(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get(Removals.Removal.HurtView)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderHand(FZLorg/joml/Matrix4f;)V"))
    private void renderWorld(RenderTickCounter renderTickCounter, CallbackInfo ci) {
        if (mc.player == null || mc.world == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        MatrixStack matrixStack = new MatrixStack();
        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        MathUtility.lastProjMat.set(mc.gameRenderer.getBasicProjectionMatrix(mc.gameRenderer.getFov(camera, mc.getRenderTickCounter().getTickProgress(true), true)));
        MathUtility.lastModMat.set(RenderSystem.getModelViewMatrix());
        MathUtility.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
        MathUtility.worldStack = matrixStack;

        RenderSystem.getModelViewStack().popMatrix();
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void topchdlc$hookHand(float tickProgress, boolean sleeping, Matrix4f positionMatrix, CallbackInfo ci) {
        if (CameraCustomer.INSTANCE.isEnabled() && CameraCustomer.INSTANCE.zoomActive && !CameraCustomer.INSTANCE.zoomHands.get()) ci.cancel();
    }
}