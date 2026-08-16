package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.EventPriority;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.elytraglide.*;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;

public class ElytraGlide extends Module {
    private ElytraGlide() {
        super("ElytraGlide", Category.MOVEMENT, "x");
    }
    public static final ElytraGlide INSTANCE = new ElytraGlide();
    ChoiceSetting<Choice> mode = choiceSetting("Mode", 0,new OldLonyglide(),new RwGlide());

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
