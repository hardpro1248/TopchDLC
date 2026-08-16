package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventTravel extends Event {
    private static EventTravel instance = new EventTravel();

    public static EventTravel build() {
        instance.reset();
        return instance;
    }
}
