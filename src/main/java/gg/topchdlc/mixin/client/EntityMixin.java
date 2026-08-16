package gg.topchdlc.mixin.client;

import gg.topchdlc.api.events.list.EventPostMove;
import gg.topchdlc.api.events.list.EventTravel;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.Hitbox;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver.BackTrackPosResolver;
import gg.topchdlc.vse.shutki.module.modules.impl.render.FreeLook;
import gg.topchdlc.vse.utils.client.mixin.IEntity;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPositionEntity;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPotationEntity;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.Client;
import gg.topchdlc.vse.rotation.Angle;

import java.util.ArrayList;
import java.util.List;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(Entity.class)
public abstract class EntityMixin implements ResolvedPositionEntity, ResolvedPotationEntity, IEntity {

    @Unique
    public Vec3d resolvedPos = Vec3d.ZERO;

    @Unique
    public Vec2f resolvedRot = Vec2f.ZERO;

    @Unique
    double prevServerX, prevServerY, prevServerZ;

    @Unique
    public List<BackTrackPosResolver.Position> positonHistory = new ArrayList<>();

    @Override
    public List<BackTrackPosResolver.Position> getPositionHistory() {
        return positonHistory;
    }

    @Unique
    boolean isInWeb = false;

    @Override
    public boolean client$inWeb() {
        return isInWeb;
    }

    @Override
    public void client$setWeb(boolean inWeb) {
        isInWeb = inWeb;
    }

    @Inject(method = "checkBlockCollision", at = @At("HEAD"))
    private void checkBlockCollision(Vec3d from, Vec3d _to, EntityCollisionHandler.Impl collisionHandler, LongSet collidedBlockPositions, int i, CallbackInfoReturnable<Integer> cir) {
        isInWeb = false;
    }

    @Override
    @Unique
    public Vec3d hachclientport$getResolvedPos() {
        return this.resolvedPos == null ? new Vec3d(this.getX(), this.getY(), this.getZ()) : this.resolvedPos;
    }

    @Unique
    public Vec2f hachclientport$getResolvedRot() {
        return this.resolvedRot == null
                ? new Vec2f((float) this.getRotationVector().getX(), (float) this.getRotationVector().getY())
                : new Vec2f(this.resolvedRot.x, this.resolvedRot.y);
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double deltaX, double deltaY, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (self != mc.player || Client.MODULES == null) return;

        FreeLook freeLook = FreeLook.INSTANCE;
        if (freeLook != null && freeLook.isEnabled()) {
            freeLook.handleMouseLook(deltaX, deltaY);
            ci.cancel();
        }
    }
    @Inject(method = "updateTrackedPositionAndAngles", at = @At("HEAD"))
    public void onUpdateTrackedPosition(Vec3d pos, float yaw, float pitch, CallbackInfo ci) {
        resolvedPos = pos;
        resolvedRot = new Vec2f(yaw, pitch);

        prevServerX = pos.x;
        prevServerY = pos.y;
        prevServerZ = pos.z;
        positonHistory.add(new BackTrackPosResolver.Position(pos.x, pos.y, pos.z));
        positonHistory.removeIf(BackTrackPosResolver.Position::shouldRemove);

    }

    @Unique
    private boolean client$local;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityType<?> type, World world, CallbackInfo ci) {
        client$local = (Entity)(Object)this instanceof ClientPlayerEntity;
    }

    @Shadow public abstract double getX();
    @Shadow public abstract double getY();
    @Shadow public abstract double getZ();
    @Shadow public abstract Vec3d getRotationVector();

    @Inject(method = "getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
    private void client$getRotationVector(float pitch, float yaw, CallbackInfoReturnable<Vec3d> cir) {
        if (!Client.ROTATION.isRotating() || Client.IS_PANIC || !client$local) return;
        Angle angle = Client.ROTATION.getRotate();
        float f = angle.getPitch() * (float) (Math.PI / 180.0);
        float g = -angle.getYaw() * (float) (Math.PI / 180.0);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        cir.setReturnValue(new Vec3d(i * j, -k, h * j));
    }

    @Inject(method = "isLogicalSideForUpdatingMovement", at = @At("HEAD"), cancellable = true)
    private void isLogicalSideForUpdatingMovement(CallbackInfoReturnable<Boolean> cir) {
        if (Client.IS_PANIC || !((Entity)(Object)this instanceof ClientPlayerEntity)) return;
        cir.setReturnValue(true);
    }

    @Redirect(method = { "getLerpedYaw" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F"))
    private float getYaw(Entity instance) {
        if (Client.IS_PANIC || !client$local || !Client.ROTATION.isRotating()) return instance.getYaw();
        return Client.ROTATION.getVisual().getYaw();
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void move(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (!client$local) return;
        if (Client.IS_PANIC) return;

        EventTravel travel = EventTravel.build();
        Client.EVENTS.post(travel);

        if (travel.isCancelled()) ci.cancel();
    }

    @Inject(method = "updateVelocity", at = @At("TAIL"))
    private void inject2(float speed, Vec3d movementInput, CallbackInfo ci) {
        if (client$local && !Client.IS_PANIC) {
            Client.EVENTS.post(EventPostMove.build());
        }
    }

    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void client$getTargetingMargin(CallbackInfoReturnable<Float> cir) {
        if (Client.IS_PANIC) return;
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity)) return;

        if (!Hitbox.INSTANCE.isEnabled()) return;
        if (Hitbox.INSTANCE.ignFr.get() && self instanceof PlayerEntity player && Client.FRIENDS.isFriend(player)) return;
        float base = cir.getReturnValue();
        float extra = Hitbox.INSTANCE.expand.get();
        cir.setReturnValue(base + extra);
    }
}