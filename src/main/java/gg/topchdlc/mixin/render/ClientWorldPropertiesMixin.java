package gg.topchdlc.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.topchdlc.vse.shutki.module.modules.impl.render.CustomWorld;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @ModifyReturnValue(method = "getTimeOfDay", at = @At("RETURN"))
    private long hookGetTime(long original) {
        return CustomWorld.INSTANCE.getTime(original);
    }
}
