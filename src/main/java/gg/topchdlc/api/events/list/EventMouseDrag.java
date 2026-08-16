package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EventMouseDrag extends Event {
    public double mouseX, mouseY;
    public int button;
    public double deltaX, deltaY;
}
