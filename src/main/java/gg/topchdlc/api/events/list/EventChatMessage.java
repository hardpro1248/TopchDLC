package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
/**
 * Create by daun kvass
 */
public class EventChatMessage extends Event {
    public static final EventChatMessage instance = new EventChatMessage();
    public String message;
    private boolean cancelled;
    private String newMessage;

    public static EventChatMessage build(String message) {
        instance.message = message;
        instance.reset();
        return instance;
    }
}