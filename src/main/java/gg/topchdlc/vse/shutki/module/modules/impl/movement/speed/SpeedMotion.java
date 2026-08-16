package gg.topchdlc.vse.shutki.module.modules.impl.movement.speed;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

/**
 * Create by daun kvass
 */
public class SpeedMotion extends Choice {
    public SpeedMotion() {
        super("Motion");
    }

    public SliderSetting speed = sliderSetting("Speed", 0.5f, 0.1f, 2.0f).increment(0.01f);

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null) return;

            PlayerInput input = mc.player.input.playerInput;

            float forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
            float side = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);

            if (forward == 0 && side == 0) {
                return;
            }

          /*  if (mc.player.isOnGround()) {
                mc.player.jump();
                Vec3d vel = mc.player.getVelocity();
                mc.player.setVelocity(vel.x, jumpMotion.get(), vel.z);
            }*/

            float yaw = mc.player.getYaw();
            double angle = Math.toRadians(calculateYaw(yaw, forward, side));

            double motionX = -Math.sin(angle) * speed.get();
            double motionZ = Math.cos(angle) * speed.get();

            mc.player.setVelocity(new Vec3d(motionX, mc.player.getVelocity().y, motionZ));
        }
    }

    private float calculateYaw(float yaw, float forward, float side) {
        if (forward < 0) yaw += 180;

        float forwardOffset = 90;
        if (forward < 0) {
            forwardOffset = -45;
        } else if (forward > 0) {
            forwardOffset = 45;
        }

        if (side > 0) {
            yaw -= forwardOffset;
        } else if (side < 0) {
            yaw += forwardOffset;
        }

        return yaw;
    }
}