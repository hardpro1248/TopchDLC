package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import net.minecraft.entity.Entity;

public class EventAttack extends Event {
    private static EventAttack instance = new EventAttack();

    public Entity target;
    public Entity entity;

    public static EventAttack build(Entity target) {
        instance.target = target;
        instance.reset();
        return instance;
    }
}
