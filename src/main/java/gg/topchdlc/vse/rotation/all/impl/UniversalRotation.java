package gg.topchdlc.vse.rotation.all.impl;

import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.util.math.MathHelper;

public class UniversalRotation extends RotationChoice {
    public UniversalRotation() {
        super("Universal");
    }

    static float x, y;

    @Override
    public Angle calculate(Angle current, Angle target) {
        if (mc.player.age < 20) {
            x = 0;
            y = 0;
        }

        float yawDelta_raw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDelta_raw = target.getPitch() - current.getPitch();

        float speedX = (float) Math.max(MathUtility.gaussian(35, 45), Math.pow(1F - Math.abs(yawDelta_raw / 180F), 2) * MathUtility.random(30, 50));

        float yawDelta = MathHelper.clamp(yawDelta_raw, -speedX, speedX);
        float pitchDelta = MathHelper.clamp(pitchDelta_raw, -5, 5);

        float tx = MathHelper.lerp(0.35F, x, yawDelta);
        float ty = MathHelper.lerp(0.35F, y, pitchDelta);

        x = tx;
        y = ty;

        x += (float) ((pitchDelta_raw / 45F) * Math.sin(System.currentTimeMillis() / 300d)) * MathUtility.random(2, 5);
        y += (float) Math.min(MathUtility.gaussian(3, 6), Math.abs((yawDelta_raw / 90F) * Math.cos(System.currentTimeMillis() / 150d)) * MathUtility.random(2, 3) * 0.1F);

        return new Angle(current.getYaw() + x, MathHelper.clamp(current.getPitch() + y, -90, 90));
    }
}