package gg.topchdlc.vse.rotation.all.impl;

import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.rotation.all.ISnapRotation;
import net.minecraft.util.math.MathHelper;
/**
 * Create by daun kvass
 */
public class SnapRotation extends RotationChoice implements ISnapRotation {
    private static final float DEFAULT_SNAP_SPEED   = 0.3f;
    private static final float DEFAULT_RETURN_SPEED = 0.2f;
    private static final int   DEFAULT_HOLD_TICKS   = 40;

    public SnapRotation() {
        super("Snap");
    }

    public static final SnapRotation INSTANCE = new SnapRotation();

    private Angle preSnapAngle = null;
    private boolean snapped    = false;
    private boolean returning  = false;
    private int ticksHeld      = 0;

    public void prepareSnap(Angle currentAngle) {
        this.preSnapAngle = currentAngle;
        this.snapped   = true;
        this.returning = false;
        this.ticksHeld = 0;
    }
    public Angle getReturnAngle() {
        Angle ret  = preSnapAngle;
        returning  = true;
        snapped    = false;
        return ret;
    }
    public boolean isSnapped()    { return snapped; }
    public boolean isReturning()  { return returning; }
    public boolean shouldReturn() {
        int hold =  DEFAULT_HOLD_TICKS  ;
        return snapped && ticksHeld >= hold;
    }
    public void tick()  { if (snapped) ticksHeld++; }
    public void reset() {
        preSnapAngle = null;
        snapped      = false;
        returning    = false;
        ticksHeld    = 0;
    }

    @Override
    public float getReturnSpeed() {
        return  DEFAULT_RETURN_SPEED ;
    }

    @Override
    public Angle calculate(Angle current, Angle target) {
        float snap   =  DEFAULT_SNAP_SPEED   ;
        float ret    = DEFAULT_RETURN_SPEED;
        float speed  = returning ? ret : snap;

        float yawDiff   = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDiff = target.getPitch() - current.getPitch();
        float newYaw    = current.getYaw()   + yawDiff   * speed;
        float newPitch  = current.getPitch() + pitchDiff * speed;

        if (returning && Math.abs(yawDiff) < 1 && Math.abs(pitchDiff) < 1) {
            returning    = false;
            preSnapAngle = null;
            ticksHeld    = 0;
        }
        return new Angle(newYaw, newPitch);
    }
}
