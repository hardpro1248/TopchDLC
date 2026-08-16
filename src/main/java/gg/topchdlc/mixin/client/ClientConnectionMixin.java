package gg.topchdlc.mixin.client;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.api.events.list.EventSendPacket;
import gg.topchdlc.api.events.list.EventChatMessage;
import gg.topchdlc.vse.utils.network.CommandSendHelper;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Create by daun kvass
 */
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Shadow
    private Channel channel;

    @Shadow
    public abstract void send(Packet<?> packet);

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    public void client$send(Packet<?> packet, @Nullable ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        if (packet.getClass().getSimpleName().equals("CommandExecutionC2SPacket")) {
            if (CommandSendHelper.skipNextCommand) {
                CommandSendHelper.skipNextCommand = false;
                return;
            }
            try {
                java.lang.reflect.Field f = packet.getClass().getDeclaredField("command");
                f.setAccessible(true);
                String command = (String) f.get(packet);
                EventChatMessage ev = EventChatMessage.build("/" + command);
                Client.EVENTS.post(ev);
                boolean wasCancelled = ev.isCancelled();
                if (wasCancelled) { ci.cancel(); return; }
            } catch (Exception ignored) {}
        }

        if (isInventoryPacket(packet)) {
            if (NetworkUtility.blockInventoryPackets) { ci.cancel(); return; }
            if (NetworkUtility.isQueuingPackets()) { NetworkUtility.queuePacket(packet); ci.cancel(); return; }
        }
        if (NetworkUtility.shouldTriggerEvent()) {
            EventSendPacket build = EventSendPacket.build(packet);
            Client.EVENTS.post(build);
            if (build.isCancelled()) { ci.cancel(); return; }
        }

        NetworkUtility.handleCPacket(packet);
    }

    private boolean isInventoryPacket(Packet<?> packet) {
        return packet instanceof net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket ||
               packet instanceof net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket ||
               packet instanceof net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket ||
               packet instanceof net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket ||
               packet instanceof net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket ||
               packet instanceof net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private static void client$handlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;

        if (NetworkUtility.shouldTriggerEvent()) {
            EventReceivePacket build = EventReceivePacket.build(packet);
            Client.EVENTS.post(build);
            if (build.isCancelled()) ci.cancel();
        }

        NetworkUtility.handleSPacket(packet);
    }
}
