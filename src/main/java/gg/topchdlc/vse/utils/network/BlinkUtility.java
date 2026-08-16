package gg.topchdlc.vse.utils.network;

import com.google.common.collect.Queues;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.list.EventPacket;
import net.minecraft.network.packet.Packet;

import java.util.Queue;
import java.util.function.Predicate;

public class BlinkUtility implements MinecraftHolder {
    public final Queue<PacketSnapshot> queue = Queues.newConcurrentLinkedQueue();

    public void queue(EventPacket event) {
        event.cancel();
        queue.add(event.toSnapshot());
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void queue(Packet<?> packet, PacketSide side) {
        queue.add(new PacketSnapshot(packet, side, System.currentTimeMillis()));
    }

    public void flush() {
        flush(ignored -> true);
    }

    public void clear() {
        queue.clear();
    }

    public void flushTime(long delay) {
        long now = System.currentTimeMillis();
        flush(it -> now - it.ts() >= delay);
    }

    public void flush(Predicate<PacketSnapshot> filter) {
        queue.removeIf(snap -> {
            if (filter.test(snap)) {
                snap.handle();
                return true;
            }
            return false;
        });
    }
}
