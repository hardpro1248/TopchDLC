 package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.CameraCustomer;
import gg.topchdlc.vse.shutki.module.modules.impl.player.FreeCam;
import gg.topchdlc.vse.shutki.module.modules.impl.render.FreeLook;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static gg.topchdlc.MinecraftHolder.mc;
/**
 * Create by daun kvass
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void hookCameraNoClip(float f, CallbackInfoReturnable<Float> xui) {
        float dist = CameraCustomer.INSTANCE.getCameraDistance(f);
        if (dist != f) {
            xui.setReturnValue(dist);
            xui.cancel();
            return;
        }
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get().contains(Removals.Removal.cameraNoClip)) {
            xui.setReturnValue(f);
            xui.cancel();
            return;
        }
    }
    @Inject(method = "update", at = @At("TAIL"))
    private void onCameraUpdate(CallbackInfo ci) {
        if (FreeCam.INSTANCE.isActive() && mc.player != null) {
            float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
            double interpX = MathHelper.lerp(tickDelta, FreeCam.INSTANCE.getPrevCameraX(), FreeCam.INSTANCE.cameraX);
            double interpY = MathHelper.lerp(tickDelta, FreeCam.INSTANCE.getPrevCameraY(), FreeCam.INSTANCE.cameraY);
            double interpZ = MathHelper.lerp(tickDelta, FreeCam.INSTANCE.getPrevCameraZ(), FreeCam.INSTANCE.cameraZ);
            setPos(interpX, interpY, interpZ);
        }

    }
    @ModifyArgs(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"))
    private void setRotationHook(Args args) {
        if (FreeLook.INSTANCE.isActiveLook()) {
            args.setAll(FreeLook.INSTANCE.getCameraYaw(), FreeLook.INSTANCE.getCameraPitch());
        }
    }
}

