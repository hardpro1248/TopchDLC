package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventSendPacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.utils.network.BlinkUtility;

public class Blink extends Module {
    public static final Blink INSTANCE = new Blink();
    private Blink() {
        super("Blink", Category.PLAYER, "замариживает пакеты");
    }
    final BlinkUtility utility = new BlinkUtility();
    @Override
    protected void onDisable() {
        super.onDisable();
        utility.flush();
    }
    EventBus<Event> events = event -> {
        if (event instanceof EventSendPacket e) utility.queue(e);
    };
}
