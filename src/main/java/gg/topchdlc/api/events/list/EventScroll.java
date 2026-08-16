package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EventScroll extends Event {
    public double horizontal, vertical;
}
