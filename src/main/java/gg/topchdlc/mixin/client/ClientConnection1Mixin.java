package gg.topchdlc.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.ClientConnection$1")
public class ClientConnection1Mixin {
    @Inject(method = "initChannel", at = @At("TAIL"))
    private void client$applyProxy(Channel channel, CallbackInfo ci, @Local ChannelPipeline pipeline) {
        ClientSettings.INSTANCE.proxy.fire(pipeline);
    }
}
