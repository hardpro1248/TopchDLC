package gg.topchdlc.mixin.client;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.CrystalAuto;

@Mixin(ClientPlayerInteractionManager.class)
public class CrystalPlaceMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void topchdlc$onPlaceCrystal(ClientPlayerEntity player, Hand hand,
                                      BlockHitResult hitResult,
                                      CallbackInfoReturnable<ActionResult> cir) {
        if (player == null) return;

        if (player.getStackInHand(hand).getItem() == Items.END_CRYSTAL) {
            if (CrystalAuto.INSTANCE.isEnabled()) {
                CrystalAuto.INSTANCE.onCrystalPlace(hitResult.getBlockPos());
            }
        }
    }
}