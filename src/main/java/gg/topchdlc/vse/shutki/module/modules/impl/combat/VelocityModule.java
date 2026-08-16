package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.velocity.VelocityGrimCancel;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.velocity.VelocityGrimNew;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.velocity.VelocityMatrix;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.velocity.VelocityVanilla;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;


public class VelocityModule extends Module {
    private VelocityModule() {
        super("Velocity", Category.COMBAT, "позволяет не отталкивается");
    }
    public static final VelocityModule INSTANCE = new VelocityModule();
    final ChoiceSetting<Choice> mode = choiceSetting("Mode", 0,
            new VelocityGrimCancel(),
            new VelocityGrimNew(),
            new VelocityMatrix(),
            new VelocityVanilla());

    EventBus<Event> events = event -> {
        mode.onEvent(event);
    };
}
