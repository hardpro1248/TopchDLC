package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventPostMove extends Event {
    private static EventPostMove instance = new EventPostMove();

    public static EventPostMove build() {
        return instance;
    }
}
