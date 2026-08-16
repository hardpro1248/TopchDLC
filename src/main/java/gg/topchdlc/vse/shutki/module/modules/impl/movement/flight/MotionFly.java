package gg.topchdlc.vse.shutki.module.modules.impl.movement.flight;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public class MotionFly extends Choice {
    public MotionFly() {
        super("MotionFly");
    }

    public SliderSetting xz = sliderSetting("XZ", 1, 0, 10).increment(0.1f);
    public SliderSetting y = sliderSetting("Y", 1, 0, 10).increment(0.1f);

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventGameTick) {
            PlayerInput input = mc.player.input.playerInput;

            double motionX = 0;
            double motionY = 0;
            double motionZ = 0;

            if (input.jump()) {
                motionY = y.get();
            } else if (input.sneak()) {
                motionY = -y.get();
            } else {
                motionY = 0;
            }

            float forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
            float side = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);

            if (forward != 0 || side != 0) {
                double speed = xz.get();
                float yaw = mc.player.getYaw();

                double angle = Math.toRadians(calculateYaw(yaw, forward, side));

                motionX = -Math.sin(angle) * speed;
                motionZ = Math.cos(angle) * speed;
            }

            mc.player.setVelocity(new Vec3d(motionX, motionY, motionZ));
        }
    }

    private float calculateYaw(float yaw, float forward, float side) {
        if (forward < 0) yaw += 180;
        float forwardOffset = 90;
        if (forward < 0) forwardOffset = -45;
        else if (forward > 0) forwardOffset = 45;
        if (side > 0) yaw -= forwardOffset;
        else if (side < 0) yaw += forwardOffset;
        return yaw;
    }
}