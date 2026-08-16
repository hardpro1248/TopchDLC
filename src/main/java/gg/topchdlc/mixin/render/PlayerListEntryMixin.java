package gg.topchdlc.mixin.render;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.NameProtect;
/**
 * Create by daun kvass
 */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void nameprotect$displayName(CallbackInfoReturnable<@Nullable Text> cir) {
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
