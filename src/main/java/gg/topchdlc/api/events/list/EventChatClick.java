package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import net.minecraft.text.ClickEvent;

public class EventChatClick extends Event {
    private static final EventChatClick instance = new EventChatClick();

    public ClickEvent clickEvent;

    public static EventChatClick build(ClickEvent clickEvent) {
        instance.reset();
        instance.clickEvent = clickEvent;
        return instance;
    }
}
