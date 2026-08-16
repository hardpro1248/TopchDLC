package gg.topchdlc.mixin.client;

import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommandC2SPacket.class)
public class ClientCommandC2SPacketMixin {
    @Shadow @Final private ClientCommandC2SPacket.Mode mode;

    @Inject(method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("TAIL"))
    public void onInit(PacketByteBuf buf, CallbackInfo ci) {
        update(this.mode);
    }
    @Inject(method = "<init>(Lnet/minecraft/entity/Entity;Lnet/minecraft/network/packet/c2s/play/ClientCommandC2SPacket$Mode;I)V", at = @At("TAIL"))
    public void onInit(Entity entity, ClientCommandC2SPacket.Mode mode, int mountJumpHeight, CallbackInfo ci) {
        update(mode);
    }
    @Inject(method = "<init>(Lnet/minecraft/entity/Entity;Lnet/minecraft/network/packet/c2s/play/ClientCommandC2SPacket$Mode;)V", at = @At("TAIL"))
    public void onInit(Entity entity, ClientCommandC2SPacket.Mode mode, CallbackInfo ci) {
        update(mode);
    }

    @Unique
    private void update(ClientCommandC2SPacket.Mode mode) {
        if (mode == ClientCommandC2SPacket.Mode.START_SPRINTING) {
            NetworkUtility.updateServerSprint(true);
        } else if (mode == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
            NetworkUtility.updateServerSprint(false);
        }
    }
}
