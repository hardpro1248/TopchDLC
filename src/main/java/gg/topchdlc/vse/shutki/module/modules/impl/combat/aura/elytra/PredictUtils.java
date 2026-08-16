package gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.elytra;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@UtilityClass
public class PredictUtils {
    

    public static Vec3d predict(LivingEntity entity, Vec3d pos, float ticks) {
        double horizontalSpeed = Math.hypot(entity.getX() - entity.lastRenderX, entity.getZ() - entity.lastRenderZ) * 20.0D;
        double verticalSpeed = (entity.getY() - entity.lastRenderY) * 20.0D;
        if (horizontalSpeed <= 5.0D && verticalSpeed <= 5.0D) {
            return pos;
        }
        float pitch = entity.getPitch() * 0.017453292F;
        float yaw = -entity.getYaw() * 0.017453292F;
        float cosYaw = MathHelper.cos(yaw);
        float sinYaw = MathHelper.sin(yaw);
        float cosPitch = MathHelper.cos(pitch);
        float sinPitch = MathHelper.sin(pitch);
        Vec3d oldVelocity = entity.getVelocity();
        Vec3d lookVec = new Vec3d(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
        float pitchRad = (float)(entity.getPitch() * 0.017453293D);
        double horizontalLookLength = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
        double horizontalVelocity = oldVelocity.horizontalLength();
        boolean isFalling = entity.getVelocity().y <= 0.0D;
        double gravity = isFalling && entity.hasStatusEffect(StatusEffects.SLOW_FALLING) 
            ? Math.min(entity.getFinalGravity(), 0.01D) 
            : entity.getFinalGravity();
        double cosPitchSquared = MathHelper.square(Math.cos(pitchRad));
        oldVelocity = oldVelocity.add(0.0D, gravity * (-1.0D + cosPitchSquared * 0.75D), 0.0D);
        if (oldVelocity.y < 0.0D && horizontalLookLength > 0.0D) {
            double lift = oldVelocity.y * -0.1D * cosPitchSquared;
            oldVelocity = oldVelocity.add(
                lookVec.x * lift / horizontalLookLength, 
                lift, 
                lookVec.z * lift / horizontalLookLength
            );
        }
        if (pitchRad < 0.0F && horizontalLookLength > 0.0D) {
            double dive = horizontalVelocity * (-MathHelper.sin(pitchRad)) * 0.04D;
            oldVelocity = oldVelocity.add(
                -lookVec.x * dive / horizontalLookLength, 
                dive * 3.2D, 
                -lookVec.z * dive / horizontalLookLength
            );
        }
        if (horizontalLookLength > 0.0D) {
            oldVelocity = oldVelocity.add(
                (lookVec.x / horizontalLookLength * horizontalVelocity - oldVelocity.x) * 0.1D, 
                0.0D, 
                (lookVec.z / horizontalLookLength * horizontalVelocity - oldVelocity.z) * 0.1D
            );
        }
        Vec3d totalMotion = oldVelocity.multiply(0.99D, 0.98D, 0.99D);
        return pos.add(totalMotion.multiply(ticks)).add(0.0D, 0.5D, 0.0D);
    }
    public static Vec3d predictGround(LivingEntity entity, Vec3d currentPos, float ticks) {
        double motionX = entity.getX() - entity.lastX;
        double motionZ = entity.getZ() - entity.lastZ;
        double motionY = entity.getY() - entity.lastY;

        double speedSq = motionX * motionX + motionZ * motionZ;
        if (speedSq < 0.000001) {
            return currentPos;
        }

        double predictedX = currentPos.x + (motionX * ticks);
        double predictedZ = currentPos.z + (motionZ * ticks);
        double predictedY = currentPos.y;

        if (!entity.isOnGround() || Math.abs(motionY) > 0.001) {
            predictedY += (motionY * ticks);
        }
        return new Vec3d(predictedX, predictedY, predictedZ);
    }

    public static Vec3d predictFast(LivingEntity entity, Vec3d pos, float ticks) {
        Vec3d velocity = entity.getVelocity();
        return pos.add(velocity.multiply(ticks)).add(0.0D, 0.5D, 0.0D);
    }
}