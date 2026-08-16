package gg.topchdlc.vse.utils.player;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPositionEntity;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.List;

@UtilityClass
public class MoveUtility implements MinecraftHolder {

    public float getBPS(LivingEntity entity) {
        return (float)
                (Math.hypot(entity.getX() - entity.lastX, Math.hypot(entity.getY() - entity.lastY, entity.getZ() - entity.lastZ)) * 20d) * Client.TIMER;
    }

    public boolean hasElytra() {
        return mc.player.getEquippedStack(EquipmentSlot.CHEST).get(DataComponentTypes.GLIDER) != null;
    }

    public void startGliding() {
        PlayerInput prevInput = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(prevInput.forward(), prevInput.backward(), prevInput.left(), prevInput.right(), /* jump = */ false, prevInput.sneak(), prevInput.sprint());
        mc.player.startGliding();
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }

    public Vec2f getMovementInput() {
        PlayerInput input = mc.player.input.playerInput;
        float x = 0f, z = 0f;
        if (input.forward()) x = 1f;
        else if (input.backward()) x = -1f;
        if (input.left()) z = -1f;
        else if (input.right()) z = 1f;
        return new Vec2f(x, z);
    }

    public Vec2f getMovementVector(Vec2f input, float yaw) {
        float x = input.x;
        float z = input.y;
        yaw = (yaw + 90f) * 0.017453292f;
        float sin = MathHelper.sin(yaw);
        float cos = MathHelper.cos(yaw);
        return new Vec2f((x * cos - z * sin), (z * cos + x * sin));
    }

    public void stopGliding() {
        mc.player.stopGliding();
        NetworkUtility.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }

    public float getSpeed() {
        double dx = mc.player.getX() - mc.player.lastX;
        double dz = mc.player.getZ() - mc.player.lastZ;
        double dy = mc.player.getY() - mc.player.lastY;
        return (float) (Math.sqrt(dx * dx + dy * dy + dz * dz));
    }
    public float getSpeedXZ() {
        double dx = mc.player.getX() - mc.player.lastX;
        double dz = mc.player.getZ() - mc.player.lastZ;
        return (float) (Math.sqrt(dx * dx + dz * dz));
    }

    public void setSpeed(double motion) {
        PlayerInput playerInput = mc.player.input.playerInput;

        float forward = playerInput.forward() ? 1 :
                playerInput.backward() ? -1 : 0;
        float strafe = playerInput.left() ? 1 :
                playerInput.right() ? -1 : 0;

        if (forward != 0 && strafe != 0) {
            forward *= 0.7f;
            strafe *= 0.7f;
        }

        float yaw = Client.ROTATION.getRotate().getYaw();

        mc.player.setVelocity(
                (Math.sin(-yaw * MathUtility.TO_RADIANS) * forward
                        + Math.cos(yaw * MathUtility.TO_RADIANS) * strafe) * motion,
                mc.player.getVelocity().y,
                (Math.cos(-yaw * MathUtility.TO_RADIANS) * forward
                        + Math.sin(yaw * MathUtility.TO_RADIANS) * strafe) * motion
        );
    }
    public boolean hasMovement(PlayerInput input) {
        return input.forward() || input.backward() || input.left() || input.right();
    }

    public static void DPSMUSORA(double x, double y, double z) {
        if (mc.player == null) return;

        mc.player.setPosition(
                mc.player.getX() + x,
                mc.player.getY() + y,
                mc.player.getZ() + z
        );
    }
    public static float getdir() {
        boolean isr=isRightDown();
        boolean isl=isLeftDown();
        boolean isf=isForDown();
        boolean isb=isBackDown();
        float startfloat=Client.ROTATION.getRotate().getYaw();

        if(!isr && !isb && !isl && !isf) {
            return -1;
        }


        if(isb) {
            startfloat-=180;
        }

        boolean willmove=(isb || isf) && !(isb && isf) ;
        if(!willmove) {
            if(isl) {
                startfloat-=90;
            }
            if(isr) {
                startfloat+=90;
            }
        }else {

            if(startfloat==mc.player.getYaw()) {
                if(isl) {
                    startfloat-=45;
                }
                if(isr) {
                    startfloat+=45;
                }
            }else {
                if(isl) {
                    startfloat+=45;
                }
                if(isr) {
                    startfloat-=45;
                }
            }

        }



        if(!willmove) {
            willmove=(isl || isr) && !(isl && isr);
        }
        double yawt = Math.toRadians(startfloat);

        if(!willmove) {
            return -1;
        }

        return startfloat;
    }
    public static boolean isForDown() {
        if (weirdscreen()) return false;
        return mc.options.forwardKey.isPressed();
    }

    public static boolean isBackDown() {
        if (weirdscreen()) return false;
        return mc.options.backKey.isPressed();
    }

    public static boolean isLeftDown() {
        if (weirdscreen()) return false;
        return mc.options.leftKey.isPressed();
    }

    public static boolean isRightDown() {
        if (weirdscreen()) return false;
        return mc.options.rightKey.isPressed();
    }

    public static boolean weirdscreen() {
        if (mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof SignEditScreen) {
            return true;
        }
        return false;
    }

    public double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public void silentCorrection(final EventInput event, float yaw) {
        silentCorrectRaw(event, mc.player.getYaw() - yaw);
    }

    public void silentCorrectRaw(final EventInput event, float deltaYaw) {
        float dYaw = deltaYaw * MathUtility.TO_RADIANS_F;
        float sin = MathHelper.sin(dYaw);
        float cos = MathHelper.cos(dYaw);
        float forward = event.getForward();
        float strafe = event.getStrafe();

        event.setStrafe(Math.round(strafe * cos - forward * sin));
        event.setForward(Math.round(forward * cos + strafe * sin));
    }

    public Vec3d getResolvedPlayerPos() {
        return getResolvedPos(mc.player);
    }

    public Vec3d getResolvedPos(Entity entity) {
        return ((ResolvedPositionEntity) entity).hachclientport$getResolvedPos();
    }

    public boolean isKeyPressed() {
        if (mc.player == null) ;
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();

    }
    public List<KeyBinding> getMovementKeys() {
        return List.of(
            mc.options.forwardKey,
            mc.options.backKey,
            mc.options.leftKey,
            mc.options.rightKey,
            mc.options.jumpKey,
            mc.options.sneakKey
        );
    }
}