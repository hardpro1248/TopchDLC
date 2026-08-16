package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import net.minecraft.util.math.BlockPos;

public class EventWorldEmit extends Event {
    private static final EventWorldEmit instance = new EventWorldEmit();

    public int eventId, data;
    public BlockPos pos;

    public static EventWorldEmit build(int eventId, int data, BlockPos pos) {
        instance.eventId = eventId;
        instance.data = data;
        instance.pos = pos;
        instance.reset();
        return instance;
    }
}
