package gg.topchdlc.vse.rotation.all.impl.kitty.hook;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.SensUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Create by daun kvass
 */
public class SpookyTime extends RotationChoice implements MinecraftHolder {

    private final boolean fastFalling = true;

    public SpookyTime() {
        super("Sosiski");
    }

    @Override
    public Angle calculate(Angle current, Angle target) {
        if (mc.player == null) return target;

        LivingEntity entity = TargetsUtility.getTarget();
        if (entity == null) {
            return target;
        }

        Vec3d eyePos = mc.player.getEyePos();

        Vec3d targetBoxPoint = new Vec3d(
                MathHelper.clamp(eyePos.x, entity.getBoundingBox().minX, entity.getBoundingBox().maxX),
                MathHelper.clamp(eyePos.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY),
                MathHelper.clamp(eyePos.z, entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ)
        );

        Angle baseAngle = Angle.fromDiffs(targetBoxPoint.subtract(eyePos));

        float targetYaw = baseAngle.getYaw();
        float targetPitch = baseAngle.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetYaw - current.getYaw());
        float pitchDiff = targetPitch - current.getPitch();
        boolean isLookingAtTarget = Math.abs(yawDiff) < 20.0f && Math.abs(pitchDiff) < 20.0f;
        float speedSens = (float) (0.6f + Math.random() * 0.3f);
        float testSens = (float) (100.0f + Math.random() * 35.0f);

        float baseSpeed = (170.0f - 46) * speedSens;

        if (!isLookingAtTarget) {
            baseSpeed *= 0.87f;
        }

        if (fastFalling && mc.player.fallDistance > 0.0f) {
            targetYaw += mc.player.fallDistance * MathHelper.wrapDegrees(baseAngle.getYaw() - current.getYaw()) * 0.15f;
        }

        if (targetPitch > 0.0f) {
            targetPitch += (float) (-0.826f + Math.random() * (0.459f - (-0.826f)));
        }

        float rayCaster = isLookingAtTarget ? 2.7f : 1.0f;

        double distance = mc.player.distanceTo(entity);
        if (distance < 0.7) {
            baseSpeed *= 1.4f;
            targetPitch = 85.0f;
        }

        float durationX = Math.max(1.0f, baseSpeed - 35.0f);
        float durationY = Math.max(1.0f, ((testSens - 20) * rayCaster) - 35.0f);

        float factorX = MathHelper.clamp(1.0f / (durationX * 0.15f), 0.05f, 0.95f);
        float factorY = MathHelper.clamp(1.0f / (durationY * 0.15f), 0.05f, 0.95f);

        float nextYaw = current.getYaw() + MathHelper.wrapDegrees(targetYaw - current.getYaw()) * factorX;
        float nextPitch = current.getPitch() + (targetPitch - current.getPitch()) * factorY;

        nextPitch = MathHelper.clamp(nextPitch, -89.5f, 89.5f);

        float finalYaw = SensUtility.correctRotation(nextYaw);
        float finalPitch = SensUtility.correctRotation(nextPitch);

        return new Angle(finalYaw, finalPitch);
    }


}