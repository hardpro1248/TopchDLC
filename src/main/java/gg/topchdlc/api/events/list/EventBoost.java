package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;

public class EventBoost extends Event {
    private static EventBoost instance = new EventBoost();

    public double speedx, speedy, speedz, a, b;
    public float yaw, pitch;

    public static EventBoost build(float yaw, float pitch) {
        instance.speedx = 1.5;
        instance.speedy = 1.5;
        instance.speedz = 1.5;
        instance.a = 0.1;
        instance.b = 0.5;
        instance.yaw = yaw;
        instance.pitch = pitch;
        return instance;
    }
}
