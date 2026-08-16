package gg.topchdlc.mixin.movement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventVelocity;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.AntiPush;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(Entity.class)
public abstract class EntityMixin {
        @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void pushAwayFromHook(Entity entity, CallbackInfo ci) {
        if (AntiPush.INSTANCE.isEnabled() && AntiPush.INSTANCE.playerPush.get()) {
            Object self = this;
            if (self == mc.player) {
                ci.cancel();
            }
        }
    }

    @ModifyExpressionValue(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d original, @Local(argsOnly = true) Vec3d movementInput, @Local(argsOnly = true) float speed, @Local(argsOnly = true) float yaw) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            return original;
        }

        EventVelocity event = new EventVelocity(movementInput, speed, yaw, original);
        Client.EVENTS.post(event);
        return event.velocity;
    }
}
