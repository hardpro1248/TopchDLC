package gg.topchdlc.mixin.client;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.list.EventChatMessage;
import gg.topchdlc.api.events.list.EventLoadChunk;
import gg.topchdlc.api.events.list.EventSetRotation;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
/**
 * Create by daun kvass
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin implements MinecraftHolder {

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void InjectSendChatCommand(String command, CallbackInfo ci) {
        if (Client.IS_PANIC) return;
        Client.EVENTS.post(EventChatMessage.build("/" + command));
        boolean wasCancelled = EventChatMessage.instance.isCancelled();
        if (wasCancelled) ci.cancel();
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void InjectSendChatMessage(String content, CallbackInfo ci) {
        if (Client.IS_PANIC) {
            return;
        }

        Client.EVENTS.post(EventChatMessage.build(content));
        boolean wasCancelled = EventChatMessage.instance.isCancelled();
        if (wasCancelled) ci.cancel();
    }
    @Inject(method = "setPosition", at = @At("HEAD"), cancellable = true)
    private static void InjectSetPosition(EntityPosition pos, Set<PositionFlag> flags, Entity entity, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ClientPlayerEntity)) return;
        cir.cancel();
        EntityPosition playerPosition = EntityPosition.fromEntity(entity);
        EntityPosition playerPosition2 = EntityPosition.apply(playerPosition, pos, flags);
        boolean bl2 = playerPosition.position().squaredDistanceTo(playerPosition2.position()) > 4096.0;
        if (bl && !bl2) {
            EventSetRotation event = EventSetRotation.build(playerPosition2.yaw(), playerPosition2.pitch());
            Client.EVENTS.post(event);
            if (!event.isCancelled()) {
                entity.updateTrackedPositionAndAngles(playerPosition2.position(), playerPosition2.yaw(), playerPosition2.pitch());
            } else {
                entity.updateTrackedPositionAndAngles(playerPosition2.position(), entity.getYaw(), entity.getPitch());
            }
            entity.setVelocity(playerPosition2.deltaMovement());
            cir.setReturnValue(true);
        } else {
            entity.setPosition(playerPosition2.position());
            entity.setVelocity(playerPosition2.deltaMovement());

            EventSetRotation event = EventSetRotation.build(playerPosition2.yaw(), playerPosition2.pitch());
            Client.EVENTS.post(event);
            if (!event.isCancelled()) {
                entity.setYaw(event.yaw);
                entity.setPitch(event.pitch);
            }
            EntityPosition playerPosition3 = new EntityPosition(entity.getLastRenderPos(), Vec3d.ZERO, entity.lastYaw, entity.lastPitch);
            EntityPosition playerPosition4 = EntityPosition.apply(playerPosition3, pos, flags);
            entity.setLastPositionAndAngles(playerPosition4.position(), playerPosition4.yaw(), playerPosition4.pitch());
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void topchdlc$onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        Client.EVENTS.post(new EventLoadChunk(packet.getChunkX(), packet.getChunkZ()));
    }
}
