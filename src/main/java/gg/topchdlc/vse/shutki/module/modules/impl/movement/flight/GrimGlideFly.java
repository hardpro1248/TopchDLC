package gg.topchdlc.vse.shutki.module.modules.impl.movement.flight;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class GrimGlideFly extends Choice {
    public GrimGlideFly() {
        super("Grim Glide");
    }

    final SliderSetting speed = sliderSetting("Speed", 0.2f, 0, 1).increment(.01f);
    final SliderSetting interval = sliderSetting("Interval", 2, 1, 4);
    final SliderSetting motionY = sliderSetting("Motion Y", 0, -1, 1).increment(.01f);
    final CheckBox antiKick = checkbox("Anti kick", false);

    int delay;

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventGameTick) {
            if (mc.player.isOnGround()) return;
            if (delay > 0) {
                delay--;
                return;
            }
            if (mc.player.age % interval.getInt() == 0) {
                NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            }
            Vec3d velocity = mc.player.getVelocity();
            velocity.y = motionY.get();

            double sqrtSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (sqrtSpeed > 0 && speed.get() > 0) {
                velocity.x = (velocity.x / sqrtSpeed) * speed.get();
                velocity.z = (velocity.z / sqrtSpeed) * speed.get();
            }

            if (antiKick.get() && mc.player.age % 40 == 0) {
                velocity.y = -0.04;
                delay = 1;
            }
        }
    }
}
