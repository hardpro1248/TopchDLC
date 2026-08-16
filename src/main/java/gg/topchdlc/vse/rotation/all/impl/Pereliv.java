package gg.topchdlc.vse.rotation.all.impl;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.entity.Entity;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;

import java.util.Random;

/**
 * Create by daun kvass
 */
public class Pereliv extends RotationChoice implements MinecraftHolder {

    private static final Random RNG = new Random();

    private float velYaw   = 0f;
    private float velPitch = 0f;
    private int tickCount = 0;

    private float currentInertia = 0.75f;
    private int inertiaChangeTick = 0;
    private float storedYaw   = Float.NaN;
    private float storedPitch = Float.NaN;

    public Pereliv() {
        super("Pereliv");
    }

    @Override
    public Angle calculate(Angle current, Angle target) {
        tickCount++;

        Angle base = Float.isNaN(storedYaw) ? current : new Angle(storedYaw, storedPitch);

        Angle finalTarget = target;



        float rawYawDiff   = MathHelper.wrapDegrees(finalTarget.getYaw() - base.getYaw());
        float rawPitchDiff = finalTarget.getPitch() - base.getPitch();
        float dist = (float) Math.hypot(rawYawDiff, rawPitchDiff);

        if (dist < 0.15f) {
            velYaw = 0f;
            velPitch = 90f;
            storedYaw = finalTarget.getYaw();
            storedPitch = finalTarget.getPitch();
            return finalTarget;
        }

        float baseSpeed = шишкинлес(dist);
        float targetVelYaw   = rawYawDiff   * baseSpeed;
        float targetVelPitch = rawPitchDiff * baseSpeed;

        inertiaChangeTick--;
        if (inertiaChangeTick <= 0) {
            currentInertia = MathUtility.random(0.68f, 0.76f);
            inertiaChangeTick = 5 + RNG.nextInt(5);
        }

        velYaw   = velYaw   * currentInertia + targetVelYaw   * (1f - currentInertia);
        velPitch = velPitch * currentInertia + targetVelPitch * (1f - currentInertia);

        float maxYaw   = 14f;
        float maxPitch = 8f;

        float stepYaw   = MathHelper.clamp(velYaw,   -maxYaw,   maxYaw);
        float stepPitch = MathHelper.clamp(velPitch, -maxPitch, maxPitch);

        float nextYaw   = base.getYaw()   + stepYaw;
        float nextPitch = MathHelper.clamp(base.getPitch() + stepPitch, -90f, 90f);

        storedYaw   = nextYaw;
        storedPitch = nextPitch;

        return new Angle( nextYaw, nextPitch);
    }

    private float шишкинлес(float dist) {
        float x = MathHelper.clamp(dist / 90f, 0f, 1f);
        float bell = 4f * x * (1f - x);
        return MathHelper.clamp(bell * MathUtility.random(0.32f, 0.46f) + 0.15f, 0.15f, 0.58f);
    }
}