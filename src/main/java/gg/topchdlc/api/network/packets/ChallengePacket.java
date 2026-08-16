package gg.topchdlc.api.network.packets;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Create by daun kvass
 */
public record ChallengePacket(int id) implements CustomPayload {
    public static final Id<ChallengePacket> ID = new Id<>(Identifier.of("topchdlc", "challenge"));

    public static final PacketCodec<PacketByteBuf, ChallengePacket> CODEC = new PacketCodec<>() {
        @Override
        public void encode(PacketByteBuf buf, ChallengePacket packet) {
            buf.writeInt(packet.id());
        }

        @Override
        public ChallengePacket decode(PacketByteBuf buf) {
            return new ChallengePacket(buf.readInt());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}