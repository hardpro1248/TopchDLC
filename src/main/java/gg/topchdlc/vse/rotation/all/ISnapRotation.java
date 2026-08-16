package gg.topchdlc.vse.rotation.all;

import gg.topchdlc.vse.rotation.Angle;

/**
 * Create by daun kvass
 */
public interface ISnapRotation {
    void prepareSnap(Angle currentAngle);
    Angle getReturnAngle();
    boolean isSnapped();
    boolean shouldReturn();
    void tick();
    void reset();
    boolean isReturning();
    default float getReturnSpeed() { return 0.5f; }
    default boolean isAimedAt(Angle current, Angle target, float thresholdDegrees) {
        float yawDiff = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(target.getYaw() - current.getYaw()));
        float pitchDiff = Math.abs(target.getPitch() - current.getPitch());
        return yawDiff < thresholdDegrees && pitchDiff < thresholdDegrees;
    }
}
