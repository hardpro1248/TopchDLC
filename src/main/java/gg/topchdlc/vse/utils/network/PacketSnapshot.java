package gg.topchdlc.vse.utils.network;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;

import static gg.topchdlc.MinecraftHolder.mc;

public record PacketSnapshot(Packet<?> packet, PacketSide side, long ts) {
    public static PacketSnapshot send(Packet<?> packet) {
        return new PacketSnapshot(packet, PacketSide.SEND, System.currentTimeMillis());
    }

    public static PacketSnapshot receive(Packet<?> packet) {
        return new PacketSnapshot(packet, PacketSide.RECEIVE, System.currentTimeMillis());
    }

    public void handle() {
        ClientPlayNetworkHandler net = mc.getNetworkHandler();
        if (net == null) return;
        switch (side) {
            case SEND -> NetworkUtility.sendWithoutEvent(packet);
            case RECEIVE -> NetworkUtility.handlePacket(packet);
        }
    }
}
