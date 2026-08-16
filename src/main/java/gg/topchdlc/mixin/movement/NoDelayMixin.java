package gg.topchdlc.mixin.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.NoDelay;

public class NoDelayMixin {
    @Mixin(MinecraftClient.class)
    public static abstract class RightClick {

        @Shadow
        private int itemUseCooldown;

        @Inject(method = "tick", at = @At("HEAD"))
        private void noRmbDelay(CallbackInfo ci) {
            if (!NoDelay.INSTANCE.isEnabled()) return;
            if (!NoDelay.INSTANCE.noRightClickDelay.get()) return;

            if (this.itemUseCooldown > 0) {
                this.itemUseCooldown = 0;
            }
        }
    }

    @Mixin(LivingEntity.class)
    public static abstract class Jump {

        @Shadow
        private int jumpingCooldown;

        @Inject(method = "tickMovement", at = @At("HEAD"))
        private void noJumpDelay(CallbackInfo ci) {
            if (!((Object) this instanceof ClientPlayerEntity)) return;
            if (!NoDelay.INSTANCE.isEnabled()) return;
            if (!NoDelay.INSTANCE.noJumpDelay.get()) return;

            this.jumpingCooldown = 0;
        }
    }
}