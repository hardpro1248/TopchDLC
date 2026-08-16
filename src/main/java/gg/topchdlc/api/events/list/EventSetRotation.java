package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventSetRotation extends Event {
    public static final EventSetRotation instance = new EventSetRotation();

    public float yaw, pitch;

    public static EventSetRotation build(float yaw, float pitch) {
        instance.yaw = yaw;
        instance.pitch = pitch;
        instance.reset();
        return instance;
    }
}
