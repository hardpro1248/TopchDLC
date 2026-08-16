package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class Event2D extends Event {

    private static final Event2D instance = new Event2D();

    public static Event2D build() { return instance; }
}
