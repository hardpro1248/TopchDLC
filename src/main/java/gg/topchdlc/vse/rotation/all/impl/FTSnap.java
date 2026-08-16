package gg.topchdlc.vse.rotation.all.impl;

import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.SensUtility;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.rotation.all.ISnapRotation;

import java.security.SecureRandom;

/**
 * Create by daun kvass
 */
public class FTSnap extends RotationChoice implements MinecraftHolder, ISnapRotation {
    private final SecureRandom random = new SecureRandom();
    private static final float DEFAULT_SNAP_SPEED = 0.55f;
    private static final int DEFAULT_HOLD_TICKS = 11;
    private static final float DEFAULT_RETURN_SPEED = 0.14f;
    private static final float DEFAULT_JITTER_YAW = 14f;
    private static final float DEFAULT_JITTER_PITCH = 3f;
    private static final float DEFAULT_MISS_INTERVAL = 7000f;
    private static final float DEFAULT_MISS_DURATION = 67f;

    public FTSnap() {
        super("FTSnap");
        this.settingsMode = enumSetting("Settings Mode", SettingsMode.Default).desc("хуй");
        this.snapSpeed = sliderSetting("Snap Speed", DEFAULT_SNAP_SPEED, 0.1f, 1.0f).increment(0.05f).visible(this::isCustomMode);
        this.returnSpeed = sliderSetting("Return Speed", DEFAULT_RETURN_SPEED, 0.05f, 1.0f).increment(0.05f).visible(this::isCustomMode);
        this.holdTicks = sliderSetting("Hold Ticks", DEFAULT_HOLD_TICKS, 1, 20).increment(1).visible(this::isCustomMode);
        this.jitterYawRange = sliderSetting("Jitter Yaw", DEFAULT_JITTER_YAW, 2f, 20f).increment(1f).visible(this::isCustomMode);
        this.jitterPitchRange = sliderSetting("Jitter Pitch", DEFAULT_JITTER_PITCH, 1f, 10f).increment(0.5f).visible(this::isCustomMode);
        this.missInterval = sliderSetting("Miss Interval", DEFAULT_MISS_INTERVAL, 10f, 80f).increment(1f).visible(this::isCustomMode);
        this.missDuration = sliderSetting("Miss Duration", DEFAULT_MISS_DURATION, 100f, 10000f).increment(10f).visible(this::isCustomMode);
    }

    private boolean isCustomMode() {
        return settingsMode.is(SettingsMode.Custom);
    }

    private boolean isDefaultMode() {
        return settingsMode.is(SettingsMode.Default);
    }

    private final EnumSetting<SettingsMode> settingsMode;
    private final SliderSetting snapSpeed;
    private final SliderSetting returnSpeed;
    private final SliderSetting holdTicks;

    private Angle preSnapAngle = null;
    private boolean snapped = false;
    private int ticksHeld = 0;

    private boolean returning = false;
    private Angle returnTarget = null;

    private final SliderSetting jitterYawRange;
    private final SliderSetting jitterPitchRange;
    private final SliderSetting missInterval;
    private final SliderSetting missDuration;

    private static int lastHitCount = -1;
    private static int hitsAfterMiss = 0;
    private static long missEndTime = 0;
    private static int swingsDone = 0;
    private static long lastAttackTime = 0;
    private float currentJitterYaw = 0;
    private float currentJitterPitch = 0;
    private static int counter = 0;

    public void prepareSnap(Angle currentAngle) {
        this.preSnapAngle = currentAngle;
        this.snapped = true;
        this.returning = false;
        this.returnTarget = null;
        this.ticksHeld = 0;
    }

    public Angle getReturnAngle() {
        Angle ret = preSnapAngle;
        this.returning = true;
        this.returnTarget = ret;
        this.snapped = false;
        this.ticksHeld = 0;
        return ret;
    }

    public boolean isSnapped() {
        return snapped;
    }

    @Override
    public boolean isReturning() {
        return returning;
    }

    public boolean shouldReturn() {
        int holdTicksValue = isDefaultMode() ? DEFAULT_HOLD_TICKS : holdTicks.getInt();
        return snapped && ticksHeld >= holdTicksValue;
    }

    public void tick() {
        if (snapped) {
            ticksHeld++;
        }
    }

    public void reset() {
        preSnapAngle = null;
        snapped = false;
        returning = false;
        returnTarget = null;
        ticksHeld = 0;
        counter = 0;
        lastHitCount = -1;
        hitsAfterMiss = 0;
        missEndTime = 0;
        lastAttackTime = 0;
    }

    @Override
    public float getReturnSpeed() {
        return isDefaultMode() ? DEFAULT_RETURN_SPEED : returnSpeed.get();
    }

    @Override
    public Angle calculate(Angle current, Angle target) {
        if (mc.player == null) return target;

        float snapSpeedValue = isDefaultMode() ? DEFAULT_SNAP_SPEED : snapSpeed.get();
        float returnSpeedValue = isDefaultMode() ? DEFAULT_RETURN_SPEED : returnSpeed.get();
        float jYaw = isDefaultMode() ? DEFAULT_JITTER_YAW : jitterYawRange.get();
        float jPitch = isDefaultMode() ? DEFAULT_JITTER_PITCH : jitterPitchRange.get();
        float missThreshold = isDefaultMode() ? DEFAULT_MISS_INTERVAL : missInterval.get();
        float missDur = isDefaultMode() ? DEFAULT_MISS_DURATION : missDuration.get();

        long now = System.currentTimeMillis();
        counter++;

        float attackProgress = mc.player.getAttackCooldownProgress(0.5f);
        boolean justAttacked = attackProgress > 0.9f && (now - lastAttackTime) > 100;

        if (justAttacked) {
            int currentHitCount = (int) (now / 500);
            if (currentHitCount != lastHitCount) {
                hitsAfterMiss++;
                lastHitCount = currentHitCount;
                lastAttackTime = now;
            }
        }

        if (hitsAfterMiss >= missThreshold && missEndTime == 0) {
            missEndTime = now + (long) missDur;
            hitsAfterMiss = 0;
            swingsDone = 0;
        }

        if (missEndTime != 0) {
            if (now < missEndTime) {
                long elapsed = now - (missEndTime - (long) missDur);
                handleMissSwings(elapsed);
                return new Angle(
                        current.getYaw() + random.nextFloat() * 6 - 3,
                        MathHelper.clamp(-80, -90f, 90f)
                );
            } else {
                missEndTime = 0;
            }
        }
        float speedMultiplier = snapped ? 1.3f : 1.0f;
        float timeScale = (now % 20000) / (94f / speedMultiplier);
        float circleYaw = (float) Math.sin(timeScale) * jYaw;
        float circlePitch = (float) Math.cos(timeScale) * jPitch;
        float variation = (float) Math.sin(timeScale * 0.7) * 0.3f;
        currentJitterYaw = circleYaw * (1 + variation);
        currentJitterPitch = circlePitch * (1 + variation);
        float gcd = (float) SensUtility.getGCDValue();
        if (snapped) {
            float yawDiff = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
            float pitchDiff = target.getPitch() - current.getPitch();

            float newYaw = current.getYaw() + yawDiff * snapSpeedValue + currentJitterYaw;
            float newPitch = current.getPitch() + pitchDiff * snapSpeedValue + currentJitterPitch;

            newYaw -= (newYaw - current.getYaw()) % gcd;
            newPitch -= (newPitch - current.getPitch()) % gcd;

            return new Angle(newYaw, MathHelper.clamp(newPitch, -90, 90));
        }

        if (returning && returnTarget != null) {
            float playerYaw = mc.player.getYaw();
            float playerPitch = mc.player.getPitch();
            float yawDiff = MathHelper.wrapDegrees(playerYaw - current.getYaw());
            float pitchDiff = playerPitch - current.getPitch();
            float newYaw = current.getYaw() + yawDiff * returnSpeedValue + currentJitterYaw * 0.5f;
            float newPitch = current.getPitch() + pitchDiff * returnSpeedValue + currentJitterPitch * 0.5f;
            newYaw -= (newYaw - current.getYaw()) % gcd;
            newPitch -= (newPitch - current.getPitch()) % gcd;
            float remainingYaw = Math.abs(MathHelper.wrapDegrees(playerYaw - newYaw));
            float remainingPitch = Math.abs(playerPitch - newPitch);

            if (remainingYaw < 1.5f && remainingPitch < 1.5f) {
                returning = false;
                returnTarget = null;
                return new Angle(playerYaw, MathHelper.clamp(playerPitch, -90, 90));
            }

            return new Angle(newYaw, MathHelper.clamp(newPitch, -90, 90));
        }

        float playerYaw = mc.player.getYaw();
        float pitchDiff = target.getPitch() - current.getPitch();
        float newPitch = current.getPitch() + pitchDiff * returnSpeedValue + currentJitterPitch;
        float newYaw = playerYaw + currentJitterYaw;

        newYaw -= (newYaw - playerYaw) % gcd;
        newPitch -= (newPitch - current.getPitch()) % gcd;

        return new Angle(newYaw, MathHelper.clamp(newPitch, -90, 90));
    }

    private void handleMissSwings(long elapsed) {
        if (swingsDone == 0 && elapsed >= 50) {
            mc.player.swingHand(Hand.MAIN_HAND);
            swingsDone = 1;
        } else if (swingsDone == 1 && elapsed >= 180) {
            mc.player.swingHand(Hand.MAIN_HAND);
            swingsDone = 2;
        }
    }
}