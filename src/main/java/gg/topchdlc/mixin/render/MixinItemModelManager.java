package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.ItemReplacer;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Create by daun kvass
 */
@Mixin(ItemModelManager.class)
public abstract class MixinItemModelManager {
    @ModifyVariable(
            method = "update",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack hookUpdateStack(ItemStack stack) {
        return ItemReplacer.INSTANCE.getRenderStack(stack);
    }
}
