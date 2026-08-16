package gg.topchdlc.vse.rotation.all.impl;

import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.SensUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class DefaultRotation extends RotationChoice {
    private DefaultRotation() {
        super("Default");
    }
    public static final DefaultRotation INSTANCE = new DefaultRotation();

    public enum Mode {
        Default, Fps
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Default);
    @Override
    public Angle calculate(Angle current, Angle target) {
        if (mc.player == null) return target;

        LivingEntity entity = TargetsUtility.getTarget();
        if (entity == null) {
            return target;
        }
        Angle result;
        if (mode.is(Mode.Default)) {
            result = calculateMode1(current, target);
        } else {
            result = calculateMode2(current, target);
        }

        return new Angle(
                result.getYaw(),
                result.getPitch()
        );
    }
    private Angle calculateMode1(Angle current, Angle target) {
        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDelta = target.getPitch() - current.getPitch();

        float yawAbs = Math.abs(yawDelta);
        float pitchAbs = Math.abs(pitchDelta);

        float yawSpeed = MathHelper.clamp(yawAbs * 0.14f + 6f, 10f, 40f);
        float pitchSpeed = MathHelper.clamp(pitchAbs * 0.10f + 4f, 6f, 16f);

        float stepYaw = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float stepPitch = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        float nextYaw = current.getYaw() + stepYaw;
        float nextPitch = MathHelper.clamp(current.getPitch() + stepPitch, -90f, 90f);

        boolean locked = yawAbs < 6f && pitchAbs < 5f;
        long t = System.currentTimeMillis();
        float sway = locked
                ? (float) (Math.sin(t / 420.0) * 0.18 + Math.sin(t / 610.0) * 0.12)
                : 0f;
        float swayPitch = locked
                ? (float) (Math.cos(t / 380.0) * 0.14 + Math.sin(t / 540.0) * 0.10)
                : 0f;

        return new Angle(nextYaw + sway, MathHelper.clamp(nextPitch + swayPitch, -90f, 90f));
    }
    private Angle calculateMode2(Angle current,Angle target){
        float resYaw = SensUtility.fixFPSrotation(target.getYaw() ,target.yaw);
        float resPitch = SensUtility.fixFPSrotation(target.getPitch(),target.pitch);
        return new Angle(resYaw, MathHelper.clamp(resPitch, -90, 90));
    }
}
