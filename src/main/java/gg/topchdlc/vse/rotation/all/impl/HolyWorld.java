package gg.topchdlc.vse.rotation.all.impl;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;

/**
 * Create by daun kvass
 */
public class HolyWorld extends RotationChoice implements MinecraftHolder {
    private float velYaw   = 0f;
    private float velPitch = 0f;
    private float storedYaw   = Float.NaN;
    private float storedPitch = Float.NaN;
    private float noiseTimeYaw   = MathUtility.random(0f, 100f);
    private float noiseTimePitch = MathUtility.random(0f, 100f);
    private Vec3d currentOffset = Vec3d.ZERO;
    private Vec3d targetOffset  = Vec3d.ZERO;
    private float offsetLerp    = 1.0f;
    private float recoilYaw   = 0f;
    private float recoilPitch = 0f;
    private float missDriftYaw   = 0f;
    private float missDriftPitch = 0f;
    private int hitCounter = 0;
    private int hitsToNextChange = 2;
    private Entity lastTarget = null;

    private float currentInertia = 0.72f;
    private int inertiaTicks = 0;

    public HolyWorld() {
        super("HolyWorld");
    }

    @Override
    public Angle calculate(Angle current, Angle target) {
        noiseTimeYaw   += 0.10f + MathUtility.random(0.01f, 0.03f);
        noiseTimePitch += 0.08f + MathUtility.random(0.01f, 0.03f);

        Entity targetEntity = TargetsUtility.getTarget();

        if (targetEntity != lastTarget) {
            lastTarget = targetEntity;
            hitCounter = 0;
            hitsToNextChange = MathUtility.random(2, 4);
            targetOffset = generateSmartOffset(targetEntity);
            currentOffset = targetOffset;
            offsetLerp = 1.0f;
            recoilYaw = 0f;
            recoilPitch = 0f;
        }
        if (offsetLerp < 1.0f) {
            offsetLerp += 0.12f;
            currentOffset = lerpVec3d(currentOffset, targetOffset, Math.min(1.0f, offsetLerp));
        }
        recoilYaw   = MathUtility.linear(recoilYaw,   0f, 0.22f);
        recoilPitch = MathUtility.linear(recoilPitch, 0f, 0.22f);
        missDriftYaw   = MathUtility.linear(missDriftYaw,   0f, 0.18f);
        missDriftPitch = MathUtility.linear(missDriftPitch, 0f, 0.18f);

        Angle finalTarget = target;
        if (targetEntity != null && mc.player != null) {
            Vec3d eyePos = mc.player.getEyePos();
            Vec3d targetVel = targetEntity.getVelocity();
            Vec3d lazyDrift = new Vec3d(-targetVel.x * 0.45, 0, -targetVel.z * 0.45);
            Vec3d aimPoint = targetEntity.getEntityPos()
                    .add(0, targetEntity.getHeight() * 0.5, 0)
                    .add(currentOffset)
                    .add(lazyDrift);
            finalTarget = calculateAngleToVec(eyePos, aimPoint);
        }

        Angle base = Float.isNaN(storedYaw) ? current : new Angle(storedYaw, storedPitch);
        float targetYaw   = finalTarget.getYaw() + recoilYaw + missDriftYaw;
        float targetPitch = finalTarget.getPitch() + recoilPitch + missDriftPitch;

        float rawYawDiff   = MathHelper.wrapDegrees(targetYaw - base.getYaw());
        float rawPitchDiff = targetPitch - base.getPitch();
        float dist         = (float) Math.hypot(rawYawDiff, rawPitchDiff);
        float yawNoise   = calculateNoise(noiseTimeYaw) * Math.min(dist * 0.08f, 1.0f);
        float pitchNoise = calculateNoise(noiseTimePitch) * Math.min(dist * 0.06f, 0.6f);

        if (dist < 2.0f) {
            yawNoise   += MathUtility.gaussian(-0.05f, 0.05f);
            pitchNoise += MathUtility.gaussian(-0.03f, 0.03f);
        }
        float baseSpeed = шишкинлес(dist);
        float desiredVelYaw   = (rawYawDiff + yawNoise) * baseSpeed;
        float desiredVelPitch = (rawPitchDiff + pitchNoise) * baseSpeed;
        float maxAccel = 3.8f;
        float yawAccel   = MathHelper.clamp(desiredVelYaw - velYaw, -maxAccel, maxAccel);
        float pitchAccel = MathHelper.clamp(desiredVelPitch - velPitch, -maxAccel, maxAccel);

        velYaw   += yawAccel;
        velPitch += pitchAccel;
        inertiaTicks--;
        if (inertiaTicks <= 0) {
            currentInertia = MathUtility.random(0.68f, 0.76f);
            inertiaTicks = MathUtility.random(4, 8);
        }

        velYaw   *= currentInertia;
        velPitch *= currentInertia;
        float stepYaw   = MathUtility.clamp(velYaw,   -14.0f, 18.0f);
        float stepPitch = MathUtility.clamp(velPitch, -7.5f,  7.5f);
        float nextYaw   = base.getYaw() + stepYaw;
        float nextPitch = MathHelper.clamp(base.getPitch() + stepPitch, -90f, 90f);
        Angle resultAngle = applyGCD(new Angle(nextYaw, nextPitch), base);
        storedYaw   = resultAngle.getYaw();
        storedPitch = resultAngle.getPitch();

        return resultAngle;
    }

    public void registerHit() {
        hitCounter++;
        recoilYaw   = MathUtility.random(0.5f, 1.4f) * (MathUtility.randomBoolean() ? 1 : -1);
        recoilPitch = MathUtility.random(-0.6f, 0.4f);

        if (hitCounter >= hitsToNextChange) {
            hitCounter = 0;
            hitsToNextChange = MathUtility.random(2, 4);
            if (lastTarget != null) {
                targetOffset = generateSmartOffset(lastTarget);
                offsetLerp = 0.0f;
            }
        }
    }

    public void triggerMissDrift() {
        missDriftYaw   = MathUtility.random(2.2f, 3.8f) * (MathUtility.randomBoolean() ? 1 : -1);
        missDriftPitch = MathUtility.random(-1.5f, 1.5f);
    }

    private Vec3d generateSmartOffset(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        float width  = entity.getWidth();
        float height = entity.getHeight();
        double offsetX = MathUtility.random(0.15f, 0.35f) * width * (MathUtility.randomBoolean() ? 1 : -1);
        double offsetZ = MathUtility.random(0.15f, 0.35f) * width * (MathUtility.randomBoolean() ? 1 : -1);
        double offsetY = MathUtility.random(-0.18f, 0.18f) * (height * 0.5f);

        return new Vec3d(offsetX, offsetY, offsetZ);
    }

    private float calculateNoise(float time) {
        return (float) (Math.sin(time * 0.7f) * 0.4 + Math.cos(time * 1.2f) * 0.3);
    }

    private float шишкинлес(float dist) {
        float x = MathUtility.clamp(dist / 90f, 0f, 1f);
        float bell = 4f * x * (1f - x);
        return MathUtility.clamp(bell * MathUtility.random(0.30f, 0.42f) + 0.14f, 0.14f, 0.55f);
    }

    private Angle applyGCD(Angle target, Angle current) {
        if (mc.options == null) return target;
        double sensitivity = mc.options.getMouseSensitivity().getValue();
        double f = sensitivity * 0.6 + 0.2;
        double gcd = f * f * f * 1.2;

        float deltaYaw   = target.getYaw() - current.getYaw();
        float deltaPitch = target.getPitch() - current.getPitch();

        deltaYaw   = MathUtility.step(deltaYaw, (float) gcd);
        deltaPitch = MathUtility.step(deltaPitch, (float) gcd);

        return new Angle(current.getYaw() + deltaYaw, MathHelper.clamp(current.getPitch() + deltaPitch, -90f, 90f));
    }

    private Vec3d lerpVec3d(Vec3d from, Vec3d to, float delta) {
        return new Vec3d(
                MathUtility.linear((float) from.x, (float) to.x, delta),
                MathUtility.linear((float) from.y, (float) to.y, delta),
                MathUtility.linear((float) from.z, (float) to.z, delta)
        );
    }

    private Angle calculateAngleToVec(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffY = to.y - from.y;
        double diffZ = to.z - from.z;
        double diffXZ = Math.hypot(diffX, diffZ);

        float yaw   = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float pitch = (float) MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(diffY, diffXZ)));

        return new Angle(yaw, pitch);
    }
}