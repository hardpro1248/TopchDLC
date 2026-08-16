package gg.topchdlc.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import gg.topchdlc.vse.shutki.module.modules.impl.render.CustomWorld;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @ModifyReturnValue(method = "getClampedViewDistance", at = @At("RETURN"))
    private int topchdlc$overrideViewDistance(int original) {
        CustomWorld cw = CustomWorld.INSTANCE;
        if (!cw.isEnabled() || !cw.useFog.get()) return original;
        int chunks = Math.max(2, (int) (cw.fogEnd.get() / 16f));
        return Math.min(chunks, original);
    }
}
