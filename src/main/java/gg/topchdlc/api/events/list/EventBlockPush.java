package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventBlockPush extends Event {
    private static EventBlockPush instance = new EventBlockPush();

    double x, z;

    public static EventBlockPush build(double x, double z) {
        instance.x = x;
        instance.z = z;
        instance.reset();
        return instance;
    }
}
