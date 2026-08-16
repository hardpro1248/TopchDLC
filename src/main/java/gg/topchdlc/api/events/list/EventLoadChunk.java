package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import lombok.AllArgsConstructor;
import net.minecraft.util.math.ChunkPos;

@AllArgsConstructor
public class EventLoadChunk extends Event {
    public int x, z;

    public ChunkPos chunkPos() {
        return new ChunkPos(x, z);
    }
}
