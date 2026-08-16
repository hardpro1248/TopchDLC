package gg.topchdlc.api.events.list;

import gg.topchdlc.vse.utils.network.PacketSide;
import net.minecraft.network.packet.Packet;

public class EventSendPacket extends EventPacket {
    private static final EventSendPacket instance = new EventSendPacket();

    private EventSendPacket() {
        super(PacketSide.SEND);
    }

    public static EventSendPacket build(Packet<?> packet) {
        instance.reset(packet);
        return instance;
    }
}
