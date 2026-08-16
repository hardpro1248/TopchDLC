package gg.topchdlc.api.network.packets;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Create by daun kvass
 */
public record ChallengeResponsePacket(int id) implements CustomPayload {
    public static final Id<ChallengeResponsePacket> ID = new Id<>(Identifier.of("topchdlc", "challenge_response"));

    public static final PacketCodec<PacketByteBuf, ChallengeResponsePacket> CODEC = new PacketCodec<>() {
        @Override
        public void encode(PacketByteBuf buf, ChallengeResponsePacket packet) {
            buf.writeInt(packet.id());
        }

        @Override
        public ChallengeResponsePacket decode(PacketByteBuf buf) {
            return new ChallengeResponsePacket(buf.readInt());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}