package gg.topchdlc.api.events.list;

import lombok.Getter;
import gg.topchdlc.api.events.Event;

@Getter
public class EventKey extends Event {

    private static final EventKey instance = new EventKey();

    public int key, action;

    public static EventKey build(int key, int action) {
        instance.key = key;
        instance.action = action;
        instance.reset();
        return instance;
    }
}