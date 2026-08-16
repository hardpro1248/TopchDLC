package gg.topchdlc.vse.shutki.module.modules.impl.movement.speed;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventMoveVelocity;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;

public class SpeedVanilla extends Choice {
    public SpeedVanilla() {
        super("Velocity");
    }

    final SliderSetting horizontal = sliderSetting("Horizontal", 1, 0, 10).increment(0.01f);
    final SliderSetting vertical = sliderSetting("Vertical", 1, 0, 10).increment(0.01f);

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMoveVelocity e) {
            e.movement.x *= horizontal.get();
            e.movement.y *= vertical.get();
            e.movement.z *= horizontal.get();
        }
    }
}
