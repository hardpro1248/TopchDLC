package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.vse.utils.network.PacketSide;
import gg.topchdlc.vse.utils.network.PacketSnapshot;
import lombok.Getter;
import net.minecraft.network.packet.Packet;

@Getter
public abstract class EventPacket extends Event {
    public Packet<?> packet;
    public final PacketSide side;
    public long ts;

    protected EventPacket(PacketSide side) {
        this.side = side;
    }

    protected void reset(Packet<?> packet) {
        super.reset();
        this.packet = packet;
        this.ts = System.currentTimeMillis();
    }

    public PacketSnapshot toSnapshot() {
        return new PacketSnapshot(packet, side, ts);
    }
}
