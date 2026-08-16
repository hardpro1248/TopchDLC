package gg.topchdlc.mixin.render;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.render.CustomWorld;
import gg.topchdlc.vse.shutki.module.modules.impl.render.FullBright;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 1))
    private float client$getValue(Double instance) {
        if (FullBright.INSTANCE.isEnabled()) {
            return FullBright.INSTANCE.currentGamma;
        }
        return instance.floatValue();
    }
    @Shadow private boolean dirty;
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (CustomWorld.INSTANCE.isEnabled()&& CustomWorld.INSTANCE.customColor.get()) {
            this.dirty = true;
        }
    }

    @ModifyVariable(method = "update", at = @At(value = "STORE"), ordinal = 0)
    private Vector3f modifyVector(Vector3f original) {
        if (CustomWorld.INSTANCE.isEnabled() && CustomWorld.INSTANCE.customColor.get()) {
            Color c = CustomWorld.INSTANCE.worldColor.get();
            return new Vector3f(
                    c.getRed() / 255f,
                    c.getGreen() / 255f,
                    c.getBlue() / 255f
            );
        }
        return original;
    }

    @ModifyVariable(method = "update", at = @At(value = "STORE"), ordinal = 8)
    private float modifyAmbient(float original) {
        if (CustomWorld.INSTANCE.isEnabled()&& CustomWorld.INSTANCE.customColor.get()) {
            return 0.6f;
        }
        return original;
    }

    @ModifyVariable(method = "update", at = @At(value = "STORE"), ordinal = 6)
    private float modifyNightVision(float original) {
        if (CustomWorld.INSTANCE.isEnabled()&& CustomWorld.INSTANCE.customColor.get()) {
            return 1.0f;
        }
        return original;
    }
    @Inject(method = "getDarkness", at = @At("HEAD"), cancellable = true)
    private void getDarknessFactor(LivingEntity entity, float factor, float tickProgress, CallbackInfoReturnable<Float> info) {
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get(Removals.Removal.Darkness)) info.setReturnValue(0.0f);
    }
}
