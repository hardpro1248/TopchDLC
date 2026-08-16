package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventBlockPush;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;

public class AntiPush extends Module {
    public static final AntiPush INSTANCE = new AntiPush();
    private AntiPush() {
        super("Anti Push", Category.MOVEMENT, "не толкает от выбраного условия");
    }

    public final CheckBox playerPush = checkbox("From players", true);
    public final CheckBox blockPush = checkbox("From blocks", true);
    
    EventBus<Event> events = event -> {
        if (event instanceof EventBlockPush e) {
            if (blockPush.get()) {
                e.cancel();
            }
        }
    };
}
