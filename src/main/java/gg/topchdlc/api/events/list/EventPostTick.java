package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventPostTick extends Event {
    private static final EventPostTick instance = new EventPostTick();
    public static EventPostTick build() {
        return instance;
    }
}
