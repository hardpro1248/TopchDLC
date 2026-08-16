package gg.topchdlc.api.scripts.api.bindings.event;

import gg.topchdlc.api.events.Event;

public interface IEventProvider {
    void call(Event event);
}
