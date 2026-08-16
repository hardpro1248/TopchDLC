package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventPostMotion extends Event {
    private static final EventPostMotion instance = new EventPostMotion();
    public static EventPostMotion build() {
        return instance;
    }
}
