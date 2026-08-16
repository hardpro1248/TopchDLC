package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.helper.AssistModule;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    float old_health = 0;

    @Inject(method = "attack", at = @At("HEAD"))
    private void mixin$attack(Entity target, CallbackInfo ci) {
        if (!(target instanceof LivingEntity)) return;

        LivingEntity livingTarget = (LivingEntity) target;
        old_health = livingTarget.getHealth();
    }

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void lele(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (!AssistModule.INSTANCE.isEnabled()
                || !AssistModule.INSTANCE.cancelSettings.get().contains(AssistModule.CancelMode.NoFluidSlowdown)) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.isSubmergedInWater() || player.isInLava()) {
            float speed = cir.getReturnValue();
            if (player.isOnGround()) {
                cir.setReturnValue(speed * 5.0F);
            } else {
                cir.setReturnValue(speed * 25.0F);
            }
        }
    }
}
