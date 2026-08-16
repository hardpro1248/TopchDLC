package gg.topchdlc.mixin.fixes;

import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.storage.ReadView;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create by daun kvass
 */
@Mixin(SkullBlockEntity.class)
public class SkullBlockEntityMixin {

    @Shadow
    private @Nullable ProfileComponent owner;


    @Inject(method = "readData", at = @At("TAIL"))
    private void readLegacySkullOwner(ReadView view, CallbackInfo ci) {
        if (this.owner == null) {
            this.owner = view.read("SkullOwner", ProfileComponent.CODEC).orElse(null);
        }
    }
}