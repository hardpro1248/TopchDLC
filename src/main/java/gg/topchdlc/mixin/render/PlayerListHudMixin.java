package gg.topchdlc.mixin.render;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.NameProtect;
/**
 * Create by daun kvass
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void nameprotect$getPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        NameProtect np = NameProtect.INSTANCE;
        if (!np.isEnabled()) return;

        Text original = cir.getReturnValue();
        if (original == null) return;

        String str = original.getString();
        String replaced = np.replace(str);
        if (!replaced.equals(str)) {
            cir.setReturnValue(Text.literal(replaced).setStyle(original.getStyle()));
        }
    }
}
