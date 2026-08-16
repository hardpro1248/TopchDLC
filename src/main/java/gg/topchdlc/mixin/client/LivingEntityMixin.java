package gg.topchdlc.mixin.client;

import gg.topchdlc.vse.shutki.module.modules.impl.render.SwingAnimations;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected abstract float getJumpVelocity();

    @Shadow public float handSwingProgress;
    @Shadow public float lastHandSwingProgress;

    @Shadow public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);

    @Shadow public abstract @Nullable StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect);

    @Shadow protected abstract double getEffectiveGravity();

    @Unique
    private boolean client$local = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void client$init(EntityType<?> entityType, World world, CallbackInfo ci) {
        client$local = (Entity)(Object)this instanceof ClientPlayerEntity;
    }

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void client$getYaw(CallbackInfo ci) {
        if (Client.IS_PANIC || !client$local || !Client.ROTATION.isRotating()) return;

        float f = this.getJumpVelocity();
        if (!(f <= 1.0E-5F)) {
            Vec3d vec3d = ((Entity)(Object)this).getVelocity();
            ((Entity)(Object)this).setVelocity(vec3d.x, Math.max(f, vec3d.y), vec3d.z);
            if (((Entity)(Object)this).isSprinting()) {
                float g = Client.ROTATION.getRotate().getYaw() * (float) (Math.PI / 180.0);
                ((Entity)(Object)this).addVelocityInternal(new Vec3d(-MathHelper.sin(g) * 0.2, 0.0, MathHelper.cos(g) * 0.2));
            }

            ((Entity)(Object)this).velocityDirty = true;
        }

        ci.cancel();
    }

    @Redirect(method = { "tick", "baseTick" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float getYaw(LivingEntity livingEntity) {
        if (Client.IS_PANIC || !client$local || !Client.ROTATION.isRotating()) return livingEntity.getYaw();
        return Client.ROTATION.getRotate().getYaw();
    }
    @Redirect(method = { "turnHead" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float turnHead$(LivingEntity livingEntity) {
        if (Client.IS_PANIC || !client$local || !Client.ROTATION.isRotating()) return livingEntity.getYaw();
        return Client.ROTATION.getVisual().getYaw();
    }
    @Inject(method = { "calcGlidingVelocity" }, at = @At("HEAD"), cancellable = true)
    private void calcGlidingVelocity$(Vec3d oldVelocity, CallbackInfoReturnable<Vec3d> cir) {
        if (Client.IS_PANIC || !client$local || !Client.ROTATION.isRotating()) return;
//        cir.cancel();
        Vec3d vec3d = Client.ROTATION.getRotate().toVector();
        float f = Client.ROTATION.getRotate().getPitch() * (float) (Math.PI / 180.0);
        double d = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        double e = oldVelocity.horizontalLength();
        double g = this.getEffectiveGravity();
        double h = MathHelper.square(Math.cos(f));
        oldVelocity = oldVelocity.add(0.0, g * (-1.0 + h * 0.75), 0.0);
        if (oldVelocity.y < 0.0 && d > 0.0) {
            double i = oldVelocity.y * -0.1 * h;
            oldVelocity = oldVelocity.add(vec3d.x * i / d, i, vec3d.z * i / d);
        }

        if (f < 0.0F && d > 0.0) {
            double i = e * -MathHelper.sin(f) * 0.04;
            oldVelocity = oldVelocity.add(-vec3d.x * i / d, i * 3.2, -vec3d.z * i / d);
        }

        if (d > 0.0) {
            oldVelocity = oldVelocity.add((vec3d.x / d * e - oldVelocity.x) * 0.1, 0.0, (vec3d.z / d * e - oldVelocity.z) * 0.1);
        }

        cir.setReturnValue(oldVelocity.multiply(0.99F, 0.98F, 0.99F));
        cir.cancel();
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void getHandSwingProgress(CallbackInfoReturnable<Integer> cir) {
        if (client$local && SwingAnimations.INSTANCE.isEnabled() && SwingAnimations.INSTANCE.shouldApply()) {
            cir.setReturnValue((int)(SwingAnimations.INSTANCE.speed.getMax() - SwingAnimations.INSTANCE.speed.get()));
            cir.cancel();
        }
    }
}
