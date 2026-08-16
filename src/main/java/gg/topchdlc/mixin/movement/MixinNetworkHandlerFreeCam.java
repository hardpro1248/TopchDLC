package gg.topchdlc.mixin.movement;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.player.FreeCam;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinNetworkHandlerFreeCam {

    @Inject(method = "onPlayerPositionLook", at = @At("TAIL"))
    private void onServerTeleport(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (FreeCam.INSTANCE.isActive()) {
           
        }
    }
}