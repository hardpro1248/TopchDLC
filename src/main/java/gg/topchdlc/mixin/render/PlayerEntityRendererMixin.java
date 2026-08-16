package gg.topchdlc.mixin.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;
import gg.topchdlc.mixin.accessor.PlayerEntityRenderStateAccessor;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.mixin.IPlayerEntityRenderState;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer {
    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, EntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("HEAD"))
    private void topchdlc$updateRenderState(PlayerLikeEntity playerLikeEntity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (playerLikeEntity instanceof AbstractClientPlayerEntity player) {
            ((IPlayerEntityRenderState) state).topchdlc$setEntity(player);
        }
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void topchdlc$updateRenderStateTail(PlayerLikeEntity playerLikeEntity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (playerLikeEntity instanceof AbstractClientPlayerEntity player) {
            boolean isMe = player.getGameProfile().name().equals(mc.getSession().getUsername());
            boolean isFriend = Client.INSTANCE.FRIENDS.isFriend(player);
            boolean customCape = ClientSettings.INSTANCE.customizeSetting.cape.get();
            ((PlayerEntityRenderStateAccessor) state).setCapeVisible((isMe || isFriend) && customCape);
        }
    }


    @Inject(method = "updateGliding(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("HEAD"), cancellable = true)
    private void updateGliding(PlayerLikeEntity playerLikeEntity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        ci.cancel();

        if (playerLikeEntity instanceof AbstractClientPlayerEntity player) {
            state.glidingTicks = player.getGlidingTicks() + tickProgress;
            Vec3d look = new Angle(player.getYaw(), player.getPitch()).toVector();

            Vec3d velocity = player.getState().getVelocity().lerp(player.getVelocity(), (double) tickProgress);

            if (velocity.horizontalLengthSquared() > 1.0E-5F && look.horizontalLengthSquared() > 1.0E-5F) {
                state.applyFlyingRotation = true;
                double dot = velocity.getHorizontal().normalize().dotProduct(look.getHorizontal().normalize());
                double cross = velocity.x * look.z - velocity.z * look.x;
                state.flyingRotation = (float) (Math.signum(cross) * Math.acos(Math.min(1.0, Math.abs(dot))));
            } else {
                state.applyFlyingRotation = false;
                state.flyingRotation = 0.0F;
            }
        }
    }
}