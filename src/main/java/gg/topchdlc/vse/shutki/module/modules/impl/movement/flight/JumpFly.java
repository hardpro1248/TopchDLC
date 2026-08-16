package gg.topchdlc.vse.shutki.module.modules.impl.movement.flight;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.util.math.Vec3d;

/**
 * Create by daun kvass
 */
public class JumpFly extends Choice {
    public JumpFly() {
        super("JumpFly");
    }

    public SliderSetting jumpPower = sliderSetting("Jump Power", 0.42f, 0.1f, 2.0f).increment(0.01f);

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventGameTick) {
            if (mc.player.input.playerInput.jump()) {
                double motionX = mc.player.getVelocity().x;
                double motionZ = mc.player.getVelocity().z;

                double motionY = jumpPower.get();

                mc.player.setVelocity(new Vec3d(motionX, motionY, motionZ));
            }
        }
    }
}