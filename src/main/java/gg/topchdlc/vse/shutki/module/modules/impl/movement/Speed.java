package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.EventPriority;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.speed.*;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;


public class Speed extends Module {
    private Speed() {
        super("Speed", Category.MOVEMENT, "Позволяет бегать как Флэш");
    }
    public static final Speed INSTANCE = new Speed();
    ChoiceSetting<Choice> mode = choiceSetting("Mode", 0,
             new SpeedLonyGrief(), new SpeedVanilla(),new SpeedRw(), new SpeedTargetSpeed(),new SpeedMotion(),new SpeedMetaHvH(),
            new SpeedTimerBurst());

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
