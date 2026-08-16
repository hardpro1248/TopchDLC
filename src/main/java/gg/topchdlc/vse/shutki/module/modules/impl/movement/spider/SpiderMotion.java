package gg.topchdlc.vse.shutki.module.modules.impl.movement.spider;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventMove;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.util.math.Vec3d;

/**
 * Create by daun kvass
 */
public class SpiderMotion extends Choice {
    public SpiderMotion() {
        super("SpiderMotion");
    }
    public SliderSetting xz = sliderSetting("XZ",1,0,60).increment(0.1f);
    public SliderSetting y = sliderSetting("Y",1,0,60).increment(0.1f);

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMove e) {
            if (!mc.player.isOnGround() && mc.player.horizontalCollision) {
                Vec3d currentVel = mc.player.getVelocity();
              if(mc.options.jumpKey.isPressed()) {
                  mc.player.setVelocity(currentVel.x, y.get(), currentVel.z);
              } else mc.player.setVelocity(xz.get(),y.get(),xz.get());

            }
        }
    }
}
