package gg.topchdlc.mixin.client;

import gg.topchdlc.vse.utils.client.client.ClientSpoof;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientBrandRetriever.class})
public class MixinClientBrandRetriever {
    @Inject(method = "getClientModName", at = {@At("HEAD")}, cancellable = true, remap = false)
    private static void getClientModNameHook(CallbackInfoReturnable<String> cir) {
        if(ClientSpoof.INSTANCE.clientSpoof.get())
            cir.setReturnValue(ClientSpoof.INSTANCE.getClientName());
    }
}