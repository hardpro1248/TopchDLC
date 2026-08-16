package gg.topchdlc.mixin.client;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventWorldEmit;
import net.minecraft.client.world.WorldEventHandler;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldEventHandler.class)
public class WorldEventHandlerMixin {
    @Inject(method = "processWorldEvent", at = @At("HEAD"), cancellable = true)
    public void processWorldEvent(int eventId, BlockPos pos, int data, CallbackInfo ci) {
        EventWorldEmit emit = EventWorldEmit.build(eventId, data, pos);
        Client.EVENTS.post(emit);

        if (emit.isCancelled()) ci.cancel();
    }
}
