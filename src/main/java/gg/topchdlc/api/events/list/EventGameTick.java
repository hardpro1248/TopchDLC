package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventGameTick extends Event {
    private static final EventGameTick instance = new EventGameTick();

    public static EventGameTick build() {
        return instance;
    }
}
