package gg.topchdlc.vse.rotation.all.impl;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.rotation.builder.BuilderProfile;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;

/**
 * Create by daun kvass
 */
public class Builder extends RotationChoice implements MinecraftHolder {

    public static BuilderProfile activeProfile = BuilderProfile.defaultProfile();
    public final SliderSetting extraSpeed = sliderSetting("Extra Speed", 1.2f, 0.5f, 10.0f).increment(0.1f);

    private static float velYaw   = 0f;
    private static float velPitch = 0f;
    private static float storedYaw   = Float.NaN;
    private static float storedPitch = Float.NaN;
    private static float currentRelX = 0.0f;
    private static float currentRelY = 0.58f;
    private static float targetRelX  = 0.0f;
    private static float targetRelY  = 0.58f;
    private static int activePointIndex = 0;
    private static int hitsOnCurrentPoint = 0;
    private static int maxHitsAllowed = MathUtility.random(2, 5);
    private static int idleShiftTimer = 0;

    private static Entity lastTarget = null;
    private static float targetAcquisitionProgress = 0f;

    public Builder() {
        super("Builder");
    }

    @Override
    public Angle calculate(Angle current, Angle defaultTargetAngle) {
        idleShiftTimer++;

        LivingEntity targetEntity = TargetsUtility.getTarget();

        if (targetEntity == null || mc.player == null) {
            lastTarget = null;
            storedYaw = Float.NaN;
            storedPitch = Float.NaN;
            velYaw = 0f;
            velPitch = 0f;
            targetAcquisitionProgress = 0f;
            hitsOnCurrentPoint = 0;
            return defaultTargetAngle;
        }

        double distance = mc.player.getEyePos().distanceTo(targetEntity.getEyePos());

        if (targetEntity != lastTarget || Float.isNaN(storedYaw)) {
            lastTarget = targetEntity;
            storedYaw = current.getYaw();
            storedPitch = current.getPitch();
            velYaw = 0f;
            velPitch = 0f;
            targetAcquisitionProgress = 0f;
            hitsOnCurrentPoint = 0;
            maxHitsAllowed = MathUtility.random(2, 5);
            idleShiftTimer = 0;

            selectFavorablePoint(targetEntity, distance, current);
            currentRelX = targetRelX;
            currentRelY = targetRelY;
        }

        float yawDiffFromReal = Math.abs(MathHelper.wrapDegrees(storedYaw - current.getYaw()));
        if (yawDiffFromReal > 35.0f) {
            storedYaw = MathHelper.wrapDegrees(current.getYaw() + MathHelper.clamp(MathHelper.wrapDegrees(storedYaw - current.getYaw()), -25.0f, 25.0f));
            storedPitch = MathHelper.clamp(current.getPitch(), -88.0f, 88.0f);
            velYaw *= 0.1f;
            velPitch *= 0.1f;
        }

        if (idleShiftTimer >= 20) {
            idleShiftTimer = 0;
            forceSwapDiagonalPoint();
        }

        if (targetAcquisitionProgress < 1.0f) {
            targetAcquisitionProgress = Math.min(1.0f, targetAcquisitionProgress + 0.35f);
        }

        float smoothSpeed = activeProfile.pointSmoothness * 1.5f;
        currentRelX = MathUtility.linear(currentRelX, targetRelX, smoothSpeed);
        currentRelY = MathUtility.linear(currentRelY, targetRelY, smoothSpeed * 0.85f);

        Vec3d optimalPoint = calculateDynamicBodyPoint(targetEntity, currentRelX, currentRelY);
        Angle directAngle = calculateAngleToVec(mc.player.getEyePos(), optimalPoint);
        Angle base = new Angle(storedYaw, storedPitch);

        float rawYawDiff   = MathHelper.wrapDegrees(directAngle.getYaw() - base.getYaw());
        float rawPitchDiff = directAngle.getPitch() - base.getPitch();

        float angularDistance = (float) Math.hypot(rawYawDiff, rawPitchDiff);
        if (angularDistance > 2.0f && activeProfile.pathArcCurve != null) {
            int arcIndex = MathHelper.clamp((int) ((1.0f - MathHelper.clamp(angularDistance / 30.0f, 0f, 1f)) * 9), 0, 9);
            float arcOffset = activeProfile.pathArcCurve[arcIndex] * activeProfile.arcCurvature * 15.0f;
            rawPitchDiff += arcOffset * Math.signum(rawYawDiff);
        }
        Vec3d targetVel = targetEntity.getVelocity();
        float speedMult = calculateSmoothSpeed(angularDistance, distance, targetVel) * extraSpeed.get() * targetAcquisitionProgress;

        float ratio = activeProfile.yawPitchRatio;
        float desiredVelYaw   = rawYawDiff * speedMult * ratio;
        float desiredVelPitch = rawPitchDiff * speedMult * (1.0f / ratio);

        float dynamicInertia = activeProfile.inertia;
        if (angularDistance < 3.0f) {
            dynamicInertia *= activeProfile.kineticFriction;
        }

        velYaw   = MathUtility.linear(velYaw, desiredVelYaw, dynamicInertia);
        velPitch = MathUtility.linear(velPitch, desiredVelPitch, dynamicInertia);

        float maxStepYaw = MathUtility.clamp(activeProfile.maxSpeed * extraSpeed.get(), 12.0f, 38.0f);
        float maxStepPitch = maxStepYaw * 0.55f;

        if (distance < 1.5) {
            maxStepYaw *= 0.80f;
            maxStepPitch *= 0.70f;
        }

        float stepYaw   = MathHelper.clamp(velYaw, -maxStepYaw, maxStepYaw);
        float stepPitch = MathHelper.clamp(velPitch, -maxStepPitch, maxStepPitch);

        float nextYaw   = MathHelper.wrapDegrees(base.getYaw() + stepYaw);
        float nextPitch = MathHelper.clamp(base.getPitch() + stepPitch, -89f, 89f);

        Angle resultAngle = applyGCD(new Angle(nextYaw, nextPitch), base);

        storedYaw   = resultAngle.getYaw();
        storedPitch = resultAngle.getPitch();

        return resultAngle;
    }


    private float calculateSmoothSpeed(float angularDistance, double entityDistance, Vec3d targetVel) {
        float baseSpeed = activeProfile.avgSpeed;
        float angleProgress = MathHelper.clamp(angularDistance / 25.0f, 0.0f, 1.0f);
        float flickAcceleration = (float) Math.pow(angleProgress, 0.65) * activeProfile.dynamicRatio;

        double targetSpeedXZ = Math.hypot(targetVel.x, targetVel.z);
        float targetMotionBoost = 1.0f + (float) Math.min(targetSpeedXZ * 2.2, 0.85);

        float finalSpeed = (baseSpeed + flickAcceleration * 0.45f) * targetMotionBoost;

        if (entityDistance < 2.0) {
            finalSpeed *= 0.88f;
        }

        return MathUtility.clamp(finalSpeed, 0.12f, 0.95f);
    }

    public void registerHit() {
        hitsOnCurrentPoint++;
        idleShiftTimer = 0;

        if (hitsOnCurrentPoint >= maxHitsAllowed) {
            hitsOnCurrentPoint = 0;
            maxHitsAllowed = MathUtility.random(2, 5);

            LivingEntity target = TargetsUtility.getTarget();
            if (target != null && mc.player != null) {
                forceSwapDiagonalPoint();
            }
        }
    }

    private void forceSwapDiagonalPoint() {
        float oldX = targetRelX;
        float oldY = targetRelY;

        for (int attempt = 0; attempt < 10; attempt++) {
            activePointIndex = (activePointIndex + MathUtility.random(1, 4)) % 15;
            float newX = MathUtility.clamp(activeProfile.relativePointX[activePointIndex], -0.09f, 0.09f);
            float newY = MathUtility.clamp(activeProfile.relativePointY[activePointIndex], 0.54f, 0.62f);
            float dx = newX - oldX;
            float dy = newY - oldY;
            if (Math.abs(dx) >= 0.020f && Math.abs(dy) >= 0.015f) {
                targetRelX = newX;
                targetRelY = newY;
                return;
            }
        }

        float dirX = MathUtility.randomBoolean() ? 1f : -1f;
        float dirY = MathUtility.randomBoolean() ? 1f : -1f;

        targetRelX = MathUtility.clamp(oldX + dirX * MathUtility.random(0.03f, 0.06f), -0.08f, 0.08f);
        targetRelY = MathUtility.clamp(oldY + dirY * MathUtility.random(0.02f, 0.04f), 0.54f, 0.62f);
    }

    private void selectFavorablePoint(LivingEntity target, double distance, Angle currentView) {
        Box box = target.getBoundingBox();
        Vec3d eyes = mc.player.getEyePos();
        Vec3d viewVec = currentView.toVector();

        Vec3d rayPoint = eyes.add(viewVec.multiply(distance));
        Vec3d clamped = MathUtility.clampToBox(rayPoint, box);

        double boxWidthX = Math.max(0.001, box.maxX - box.minX);
        double boxHeightY = Math.max(0.001, box.maxY - box.minY);

        float relX = (float) (((clamped.x - box.minX) / boxWidthX) - 0.5);
        float relY = (float) ((clamped.y - box.minY) / boxHeightY);
        targetRelX = MathUtility.clamp(relX, -0.06f, 0.06f);
        targetRelY = MathUtility.clamp(relY, 0.54f, 0.62f);
    }

    private static Vec3d calculateDynamicBodyPoint(LivingEntity target, float relX, float relY) {
        Box box = target.getBoundingBox();
        float clampedRelY = MathUtility.clamp(relY, 0.52f, 0.64f);
        float clampedRelX = MathUtility.clamp(relX, -0.09f, 0.09f);

        double targetX = box.minX + (box.maxX - box.minX) * (0.5 + clampedRelX);
        double targetY = box.minY + (box.maxY - box.minY) * clampedRelY;
        double targetZ = box.minZ + (box.maxZ - box.minZ) * (0.5 + clampedRelX * 0.5);

        Vec3d targetVel = target.getVelocity();
        double leadWeight = activeProfile.predictionWeight * Math.min(Math.hypot(targetVel.x, targetVel.z) * 1.3, 0.35);

        return new Vec3d(
                targetX + targetVel.x * leadWeight,
                targetY,
                targetZ + targetVel.z * leadWeight
        );
    }

    public static Vec3d getPointForTarget(Entity target) {
        Entity t = target != null ? target : TargetsUtility.getTarget();
        if (t == null || !(t instanceof LivingEntity living)) {
            return mc.player != null ? mc.player.getEyePos() : Vec3d.ZERO;
        }
        return calculateDynamicBodyPoint(living, currentRelX, currentRelY);
    }

    private Angle applyGCD(Angle target, Angle current) {
        if (mc.options == null) return target;
        double sensitivity = mc.options.getMouseSensitivity().getValue();
        double f = sensitivity * 0.6 + 0.2;
        double gcd = f * f * f * 1.2;

        float deltaYaw   = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float deltaPitch = target.getPitch() - current.getPitch();

        deltaYaw   = MathUtility.step(deltaYaw, (float) gcd);
        deltaPitch = MathUtility.step(deltaPitch, (float) gcd);

        return new Angle(MathHelper.wrapDegrees(current.getYaw() + deltaYaw), MathHelper.clamp(current.getPitch() + deltaPitch, -89f, 89f));
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