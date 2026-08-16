package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.EventPriority;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventMove;
import gg.topchdlc.api.events.list.EventPostMove;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.Tag;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.speed.SpeedLonyGrief;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.speed.SpeedRw;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.speed.SpeedTargetSpeed;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.speed.SpeedVanilla;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.timer.DefTimer;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.timer.TimerGrimTick;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.timer.TimerSpooky;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;

import java.lang.invoke.SerializedLambda;

/**
 * Create by daun kvass
 */
public class Timer extends Module {
    private Timer() {
        super("Timer", Category.MOVEMENT, "игрок становиться будто на лошади бистри");
    }
    public static final Timer INSTANCE = new Timer();
    ChoiceSetting<Choice> mode = choiceSetting("Mode", 0,
            new DefTimer(), new TimerGrimTick(),new TimerSpooky());

    @Override
    protected void onEnable() {
        mode.onEnabled();
    }

    @Override
    protected void onDisable() {
        mode.onDisabled();
    }

    @EventPriority(EventPriority.LAST)
    EventBus<Event> events = event -> {
        mode.onEvent(event);
    };
}
