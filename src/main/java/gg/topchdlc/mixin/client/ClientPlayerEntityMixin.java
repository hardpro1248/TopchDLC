package gg.topchdlc.mixin.client;

import gg.topchdlc.api.events.list.*;
import gg.topchdlc.vse.shutki.module.modules.impl.player.Sprint;
import gg.topchdlc.vse.utils.attack.AttackHandle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.Client;
import gg.topchdlc.vse.rotation.Angle;

@Mixin(value = ClientPlayerEntity.class,priority = 800)
public abstract class ClientPlayerEntityMixin {
    @Shadow protected abstract void sendSprintingPacket();
    @Shadow protected abstract boolean isCamera();

    @Shadow private double lastXClient;

    @Shadow private double lastYClient;

    @Shadow private double lastZClient;

    @Shadow private float lastYawClient;

    @Shadow private float lastPitchClient;

    @Shadow private int ticksSinceLastPositionPacketSent;

    @Shadow @Final public ClientPlayNetworkHandler networkHandler;

    @Shadow private boolean lastOnGround;

    @Shadow private boolean lastHorizontalCollision;

    @Shadow @Final protected MinecraftClient client;

    @Shadow private boolean autoJumpEnabled;

    @Shadow public Input input;

    @Shadow protected abstract boolean wouldCollideAt(BlockPos pos);

    @Shadow public abstract void move(MovementType type, Vec3d movement);


    @Redirect(
            method = "applyMovementSpeedFactors",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec2f;multiply(F)Lnet/minecraft/util/math/Vec2f;", ordinal = 1)
    )
    private Vec2f cancelItemSlowdown(Vec2f vec2f, float multiplier) {
        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        EventNoSlow event = EventNoSlow.build();
        Client.EVENTS.post(event);

        if (event.isCancelled() && player.isUsingItem() && !player.hasVehicle()) {
            return vec2f.multiply(1.0F);
        }

        return vec2f.multiply(multiplier);
    }
    private boolean hui() {
        return Sprint.INSTANCE.isEnabled() && Sprint.INSTANCE.ignoreHunger.get();
    }
    @Inject(method = "canSprint", at = @At("RETURN"), cancellable = true)
    private void hunger(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !hui()) return;

        ClientPlayerEntity player = (ClientPlayerEntity)(Object)this;
        if (player.getHungerManager().getFoodLevel() > 6) return;
        if (this.input == null || !this.input.playerInput.forward()) return;

        cir.setReturnValue(true);
    }
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V", shift = At.Shift.AFTER))
    private void tick(CallbackInfo ci) {
        if (Client.IS_PANIC) return;
        Client.EVENTS.post(EventPostMotion.build());

    }

    @Redirect(method = { "tick" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float getYaw(ClientPlayerEntity clientPlayerEntity) {
        if (Client.IS_PANIC || !Client.ROTATION.isRotating()) return clientPlayerEntity.getYaw();
        return Client.ROTATION.getRotate().getYaw();
    }
    @Redirect(method = { "tick" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float getPitch(ClientPlayerEntity clientPlayerEntity) {
        if (Client.IS_PANIC || !Client.ROTATION.isRotating()) return clientPlayerEntity.getPitch();
        return Client.ROTATION.getRotate().getPitch();
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), name = "arg2", ordinal = 0, index = 2, argsOnly = true)
    private Vec3d hookMove(Vec3d movement, MovementType type) {
        if (Client.IS_PANIC) {
            return movement;
        }
        return Client.EVENTS.post(new EventMoveVelocity(type, movement)).movement;
    }


    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void sendMovementPackets(CallbackInfo ci) {
        if (Client.IS_PANIC) {
            return;
        }
        ci.cancel();
        Entity main = ((Entity)(Object)this);
        Angle angle = Client.ROTATION.getRotate();
        EventMove move = EventMove.build(main.getX(), main.getY(), main.getZ(), angle.getYaw(), angle.getPitch(), main.getVelocity(), main.isOnGround(), main.horizontalCollision);
        Client.EVENTS.post(move);
        if (move.isCancelled()) return;
        this.sendSprintingPacket();
        if (this.input != null && this.input.playerInput != null) {
            this.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket(this.input.playerInput));
        }
        
        if (this.isCamera()) {
            double diffX = move.getX() - this.lastXClient;
            double diffY = move.getY() - this.lastYClient;
            double diffZ = move.getZ() - this.lastZClient;
            double diffYaw = move.getYaw() - this.lastYawClient;
            double diffPitch = move.getPitch() - this.lastPitchClient;
            this.ticksSinceLastPositionPacketSent++;
            boolean changedPosition = MathHelper.squaredMagnitude(diffX, diffY, diffZ) > MathHelper.square(2.0E-4) || this.ticksSinceLastPositionPacketSent >= 20;
            boolean changedYaw = diffYaw != 0.0 || diffPitch != 0.0;
            if (changedPosition && changedYaw) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(move.getPos(), move.getYaw(), move.getPitch(), move.isGround(), move.horizontalCollision));
            } else if (changedPosition) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(move.getPos(), move.isGround(), move.horizontalCollision));
            } else if (changedYaw) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(move.getYaw(), move.getPitch(), move.isGround(), move.horizontalCollision));
            } else if (this.lastOnGround != move.isGround() || this.lastHorizontalCollision != move.horizontalCollision) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(move.isGround(), move.horizontalCollision));
            }

            if (changedPosition) {
                this.lastXClient = move.getX();
                this.lastYClient = move.getY();
                this.lastZClient = move.getZ();
                this.ticksSinceLastPositionPacketSent = 0;
            }

            if (changedYaw) {
                this.lastYawClient = move.getYaw();
                this.lastPitchClient = move.getPitch();
            }

            if (move.isGround()) {
                ++AttackHandle.ticksOnBlock;
            } else AttackHandle.ticksOnBlock = 0;

            this.lastOnGround = move.isGround();
            this.lastHorizontalCollision = move.horizontalCollision;
            this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void pushOfBlock(double x, double z, CallbackInfo ci) {
        if (Client.IS_PANIC) {
            return;
        }
        
        ci.cancel();

        EventBlockPush eventPush = EventBlockPush.build(x, z);
        Client.EVENTS.post(eventPush);

        if (eventPush.isCancelled()) return;

        BlockPos blockPos = BlockPos.ofFloored(x, ((Entity)(Object)this).getY(), z);
        if (this.wouldCollideAt(blockPos)) {
            double d = x - blockPos.getX();
            double e = z - blockPos.getZ();
            Direction direction = null;
            double f = Double.MAX_VALUE;
            Direction[] directions = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

            for (Direction direction2 : directions) {
                double g = direction2.getAxis().choose(d, 0.0, e);
                double h = direction2.getDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - g : g;
                if (h < f && !this.wouldCollideAt(blockPos.offset(direction2))) {
                    f = h;
                    direction = direction2;
                }
            }

            if (direction != null) {
                Vec3d vec3d = ((Entity)(Object)this).getVelocity();
                if (direction.getAxis() == Direction.Axis.X) {
                    ((Entity)(Object)this).setVelocity(0.1 * direction.getOffsetX(), vec3d.y, vec3d.z);
                } else {
                    ((Entity)(Object)this).setVelocity(vec3d.x, vec3d.y, 0.1 * direction.getOffsetZ());
                }
            }
        }
    }
}
