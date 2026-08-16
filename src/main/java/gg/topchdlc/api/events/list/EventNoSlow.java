package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventNoSlow extends Event {
    private static final EventNoSlow instance = new EventNoSlow();
    public static EventNoSlow build() {
        instance.reset();
        return instance;
    }
}
