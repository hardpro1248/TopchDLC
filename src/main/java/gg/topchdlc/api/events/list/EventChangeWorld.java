package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import lombok.AllArgsConstructor;
import net.minecraft.client.world.ClientWorld;

@AllArgsConstructor
public class EventChangeWorld extends Event {
    public final ClientWorld world;
}
