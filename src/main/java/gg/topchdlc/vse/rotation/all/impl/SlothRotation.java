package gg.topchdlc.vse.rotation.all.impl;

import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SlothRotation extends RotationChoice {

    private LivingEntity trackedTarget;

    private float currentYaw;
    private float currentPitch;

    private float velocityYaw;
    private float velocityPitch;

    private double aimPointX;
    private double aimPointY;
    private double aimPointZ;

    private float noiseSeedA;
    private float noiseSeedB;
    private final float noiseStrength = 1.35F;

    private int hitPhase;
    private int hitTimer;
    private float pitchBeforeHit;

    private long firstSeenTime;
    private int reactionMs;
    private boolean reactionComplete;

    private float lastSentYaw;
    private float lastSentPitch;

    private float smoothYaw;
    private float smoothPitch;

    private float outYaw;
    private float outPitch;

    public SlothRotation() {
        super("Sloth");
    }

    public void reset() {
        trackedTarget = null;
        velocityYaw = velocityPitch = 0.0F;
        aimPointX = aimPointY = aimPointZ = 0.0;
        noiseSeedA = 0.0F;
        noiseSeedB = 0.0F;
        hitPhase = hitTimer = 0;
        firstSeenTime = 0;
        reactionComplete = false;
        reactionMs = 0;

        if (mc.player != null) {
            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            lastSentYaw = currentYaw;
            lastSentPitch = currentPitch;
            smoothYaw = currentYaw;
            smoothPitch = currentPitch;
        } else {
            currentYaw = currentPitch = 0.0F;
            lastSentYaw = lastSentPitch = 0.0F;
            smoothYaw = smoothPitch = 0.0F;
        }
    }

    private float calcGcd() {
        double s = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        return (float) (s * s * s * 1.2);
    }

    private void pickAimPoint(LivingEntity e) {
        Box bb = e.getBoundingBox();
        double w = bb.maxX - bb.minX;
        double h = bb.maxY - bb.minY;
        double d = bb.maxZ - bb.minZ;

        aimPointX = (Math.random() - 0.5) * w * 0.15;
        aimPointY = (Math.random() - 0.5) * h * 0.09;
        aimPointZ = (Math.random() - 0.5) * d * 0.15;
    }

    public void onAttack() {
        hitPhase = 1;
        hitTimer = 0;
        pitchBeforeHit = currentPitch;
    }

    private float measureAngle(LivingEntity e) {
        if (mc.player == null) return 0.0F;

        Vec3d eyes = mc.player.getEyePos();
        Vec3d mid = e.getBoundingBox().getCenter();
        Vec3d delta = mid.subtract(eyes);

        float needYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float needPitch = (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalLength()));

        float dYaw = Math.abs(MathHelper.wrapDegrees(needYaw - mc.player.getYaw()));
        float dPitch = Math.abs(needPitch - mc.player.getPitch());

        return dYaw + dPitch;
    }

    private int computeReaction(float angle) {
        if (angle > 130.0F) return 160 + (int) (Math.random() * 70);
        if (angle > 70.0F) return 100 + (int) (Math.random() * 55);
        if (angle > 30.0F) return 55 + (int) (Math.random() * 30);
        return 18 + (int) (Math.random() * 16);
    }

    private boolean isMovingForward() {
        if (mc.player == null) return false;
        return mc.options.forwardKey.isPressed();
    }

    private boolean isOvertakingTarget(LivingEntity target) {
        if (mc.player == null || target == null) return false;

        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d targetPos = target.getEntityPos();

        Vec3d playerVel = new Vec3d(
                mc.player.getX() - mc.player.lastX,
                mc.player.getY() - mc.player.lastY,
                mc.player.getZ() - mc.player.lastZ
        );

        Vec3d targetVel = new Vec3d(
                target.getX() - target.lastX,
                target.getY() - target.lastY,
                target.getZ() - target.lastZ
        );

        Vec3d toTarget = targetPos.subtract(playerPos).normalize();

        double playerSpeedToTarget = playerVel.dotProduct(toTarget);
        double targetSpeedToPlayer = targetVel.dotProduct(toTarget.multiply(-1));

        double relativeSpeed = playerSpeedToTarget + targetSpeedToPlayer;

        double distance = Math.sqrt(
                Math.pow(playerPos.x - targetPos.x, 2) +
                        Math.pow(playerPos.z - targetPos.z, 2)
        );

        return relativeSpeed > 0.05 && distance < 4.0;
    }

    private float[] generateNoise(float dist) {
        noiseSeedA += 0.031F + (float) (Math.random() * 0.021F);
        noiseSeedB += 0.057F + (float) (Math.random() * 0.014F);

        float scale = MathHelper.clamp(dist / 5.0F, 0.2F, 1.0F);
        float amp = noiseStrength * scale;

        float w1 = (float) Math.sin(noiseSeedA * 1.05) * 0.33F;
        float w2 = (float) Math.cos(noiseSeedA * 0.62 + 1.10) * 0.24F;
        float w3 = (float) Math.sin(noiseSeedB * 1.31 + 0.48) * 0.29F;
        float w4 = (float) Math.cos(noiseSeedB * 1.87 + 2.05) * 0.20F;

        float yawNoise = (w1 + w3) * amp;
        float pitchNoise = (w2 + w4) * amp * 0.46F;

        yawNoise += ((float) Math.random() - 0.5F) * amp * 0.10F;
        pitchNoise += ((float) Math.random() - 0.5F) * amp * 0.07F;

        return new float[]{yawNoise, pitchNoise};
    }

    private float easeOutCubic(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        float inv = 1.0F - x;
        return 1.0F - inv * inv * inv;
    }

    private float easeInOutQuad(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x < 0.5F ? 2.0F * x * x : 1.0F - (float) Math.pow(-2.0F * x + 2.0F, 2) / 2.0F;
    }

    private float dampedSpring(float current, float target, float vel, float stiffness, float damping) {
        float diff = target - current;
        float acc = diff * stiffness - vel * damping;
        return vel + acc;
    }

    private float angularLerp(float from, float to, float alpha) {
        alpha = MathHelper.clamp(alpha, 0.0F, 1.0F);
        float delta = MathHelper.wrapDegrees(to - from);
        return from + delta * alpha;
    }

    private float calculateCurrentAngle(float targetYaw, float targetPitch) {
        float dYaw = Math.abs(MathHelper.wrapDegrees(targetYaw - currentYaw));
        float dPitch = Math.abs(targetPitch - currentPitch);
        return dYaw + dPitch;
    }

    private void updateRotations(LivingEntity target) {
        if (mc.player == null || target == null) return;

        boolean playerFlying = mc.player.isGliding();

        if (trackedTarget != target) {
            trackedTarget = target;

            currentYaw = mc.player.getYaw();
            currentPitch = mc.player.getPitch();
            lastSentYaw = currentYaw;
            lastSentPitch = currentPitch;
            smoothYaw = currentYaw;
            smoothPitch = currentPitch;
            velocityYaw = velocityPitch = 0.0F;

            pickAimPoint(target);

            hitPhase = hitTimer = 0;
            noiseSeedA = (float) (Math.random() * Math.PI * 2);
            noiseSeedB = (float) (Math.random() * Math.PI * 2);

            float angleDiff = measureAngle(target);
            reactionMs = computeReaction(angleDiff);
            firstSeenTime = System.currentTimeMillis();
            reactionComplete = false;
        }

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetCenter = target.getBoundingBox().getCenter();
        float distance = (float) eyePos.distanceTo(targetCenter);

        float gcd = calcGcd();

        if (!reactionComplete) {
            long elapsed = System.currentTimeMillis() - firstSeenTime;

            if (elapsed < reactionMs) {
                float progress = elapsed / (float) Math.max(reactionMs, 1);
                float shake = 1.0F - easeInOutQuad(progress) * 0.4F;

                float jitterY = ((float) Math.random() - 0.5F) * 0.30F * shake;
                float jitterP = ((float) Math.random() - 0.5F) * 0.19F * shake;

                float outY = lastSentYaw + jitterY;
                float outP = MathHelper.clamp(lastSentPitch + jitterP, -89.0F, 89.0F);

                outY -= (outY - lastSentYaw) % gcd;
                outP -= (outP - lastSentPitch) % gcd;

                lastSentYaw = outY;
                lastSentPitch = outP;

                outYaw = outY;
                outPitch = outP;
                return;
            }

            reactionComplete = true;
        }

        float[] noise = generateNoise(distance);

        if (hitPhase > 0) {
            hitTimer++;

            int upDuration = 18;
            int downDuration = 26;
            float targetPitchUp = -88.0F;

            if (hitPhase == 1) {
                float t = hitTimer / (float) upDuration;
                t = MathHelper.clamp(t, 0.0F, 1.0F);
                float curved = easeOutCubic(t);
                currentPitch = MathHelper.lerp(curved, pitchBeforeHit, targetPitchUp);

                if (hitTimer >= upDuration) {
                    hitPhase = 2;
                    hitTimer = 0;
                }
            } else if (hitPhase == 2) {
                float goal = pitchBeforeHit;
                float t = hitTimer / (float) downDuration;
                t = MathHelper.clamp(t, 0.0F, 1.0F);
                float curved = easeInOutQuad(t);
                currentPitch = MathHelper.lerp(curved, targetPitchUp, goal);

                if (hitTimer >= downDuration) {
                    hitPhase = 0;
                    hitTimer = 0;
                }
            }

            float outY = currentYaw + noise[0];
            float outP = MathHelper.clamp(currentPitch + noise[1], -89.0F, 89.0F);

            outY -= (outY - lastSentYaw) % gcd;
            outP -= (outP - lastSentPitch) % gcd;

            lastSentYaw = outY;
            lastSentPitch = outP;

            outYaw = outY;
            outPitch = outP;
            return;
        }

        if (Math.random() < 0.018) {
            pickAimPoint(target);
        }

        Vec3d targetVel = new Vec3d(
                target.getX() - target.lastX,
                target.getY() - target.lastY,
                target.getZ() - target.lastZ
        );

        int predictTicks = target.isGliding() ? 0 : 2;
        Vec3d predictedCenter = targetCenter.add(targetVel.multiply(predictTicks));
        Vec3d aimPos = predictedCenter.add(aimPointX, aimPointY, aimPointZ);
        Vec3d direction = aimPos.subtract(eyePos);

        float wantYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalLength()));

        float diffYaw = MathHelper.wrapDegrees(wantYaw - currentYaw);
        float diffPitch = wantPitch - currentPitch;

        float speedMultiplier = 1.0F;

        if (playerFlying) {
            float currentAngle = calculateCurrentAngle(wantYaw, wantPitch);

            if (currentAngle > 120.0F) {
                speedMultiplier = 0.22F;
            } else if (currentAngle > 80.0F) {
                float t = (currentAngle - 80.0F) / 40.0F;
                speedMultiplier = MathHelper.lerp(easeInOutQuad(t), 0.40F, 0.22F);
            } else if (currentAngle > 25.0F) {
                float t = (currentAngle - 25.0F) / 55.0F;
                speedMultiplier = MathHelper.lerp(easeInOutQuad(t), 0.70F, 0.40F);
            } else {
                speedMultiplier = 0.70F + (0.30F * (1.0F - currentAngle / 25.0F));
            }
        } else {
            boolean movingForward = isMovingForward();
            boolean overtaking = isOvertakingTarget(target);
            if (movingForward || overtaking) {
                speedMultiplier = 0.55F;
            }
        }

        float stiffness = (0.045F + (float) Math.random() * 0.011F) * speedMultiplier;
        float damping = 0.72F + (0.10F * (1.0F - speedMultiplier));

        float totalDiff = (float) Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);

        if (totalDiff > 34.0F) {
            stiffness += 0.021F * speedMultiplier;
        } else if (totalDiff < 3.8F) {
            stiffness *= 0.42F;
        }

        stiffness += MathHelper.clamp((distance - 1.5F) / 8.0F, 0.0F, 0.05F) * speedMultiplier;

        velocityYaw = dampedSpring(currentYaw, currentYaw + diffYaw, velocityYaw, stiffness, damping);
        velocityPitch = dampedSpring(currentPitch, wantPitch, velocityPitch, stiffness * 0.92F, damping);

        float maxVelYaw = 8.2F * speedMultiplier;
        float maxVelPitch = 6.3F * speedMultiplier;

        velocityYaw = MathHelper.clamp(velocityYaw, -maxVelYaw, maxVelYaw);
        velocityPitch = MathHelper.clamp(velocityPitch, -maxVelPitch, maxVelPitch);

        currentYaw += velocityYaw;
        currentPitch += velocityPitch;

        currentPitch = MathHelper.clamp(currentPitch, -89.0F, 89.0F);

        float smoothFactor = playerFlying ? (0.25F + speedMultiplier * 0.45F) : 0.80F;

        smoothYaw = angularLerp(smoothYaw, currentYaw, smoothFactor);
        smoothPitch = angularLerp(smoothPitch, currentPitch, smoothFactor * 0.9F);

        float outY = smoothYaw + noise[0];
        float outP = smoothPitch + noise[1];
        outP = MathHelper.clamp(outP, -89.0F, 89.0F);

        outY -= (outY - lastSentYaw) % gcd;
        outP -= (outP - lastSentPitch) % gcd;

        lastSentYaw = outY;
        lastSentPitch = outP;

        outYaw = outY;
        outPitch = outP;
    }

    @Override
    public Angle calculate(Angle current, Angle target) {
        if (mc.player == null) return target;

        LivingEntity t = TargetsUtility.getTarget();
        if (t == null) return target;

        updateRotations(t);

        return new Angle(outYaw, outPitch);
    }
}
