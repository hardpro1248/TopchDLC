package gg.topchdlc.api.events.list;

import gg.topchdlc.vse.utils.network.PacketSide;
import net.minecraft.network.packet.Packet;

public class EventReceivePacket extends EventPacket {
    private EventReceivePacket() {
        super(PacketSide.RECEIVE);
    }

    private static final EventReceivePacket instance = new EventReceivePacket();

    public static EventReceivePacket build(Packet<?> packet) {
        instance.reset(packet);
        return instance;
    }
}
