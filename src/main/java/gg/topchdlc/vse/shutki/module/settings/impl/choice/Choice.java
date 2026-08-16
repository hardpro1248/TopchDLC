package gg.topchdlc.vse.shutki.module.settings.impl.choice;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;

public abstract class Choice extends Group implements EventBus<Event> {
    protected Choice(String name) {
        super(name);
    }

    public void onEnabled() {}
    public void onDisabled() {}

    @Override
    public void onEvent(Event event) {
    }
}
