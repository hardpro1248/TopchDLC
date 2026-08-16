package gg.topchdlc.mixin.movement;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.player.FreeCam;

@Mixin(ClientConnection.class)
public class MixinClientConnectionFreeCam {

    @Inject(
            method = "send(Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isActive() ) {
            if (packet instanceof PlayerMoveC2SPacket) {
                ci.cancel();
            }
        }
    }
}