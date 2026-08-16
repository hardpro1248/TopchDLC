package gg.topchdlc.api.network.packets;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Create by daun kvass
 */
public record VersionPacket(int major, int minor, int patch, boolean snapshot) implements CustomPayload {
    public static final Id<VersionPacket> ID = new Id<>(Identifier.of("topchdlc", "version"));

    public static final PacketCodec<PacketByteBuf, VersionPacket> CODEC = new PacketCodec<>() {
        @Override
        public void encode(PacketByteBuf buf, VersionPacket packet) {
            buf.writeInt(packet.major());
            buf.writeInt(packet.minor());
            buf.writeInt(packet.patch());
            buf.writeBoolean(packet.snapshot());
        }

        @Override
        public VersionPacket decode(PacketByteBuf buf) {
            return new VersionPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}