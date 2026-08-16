package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventSendPacket;
import gg.topchdlc.api.events.list.EventTravel;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class FreeCam extends Module {
    public static final FreeCam INSTANCE = new FreeCam();
    public SliderSetting speed = sliderSetting("Скорость", 0.7f, 0.1f, 10.0f).increment(0.1f);
    public CheckBox showFakePlayer = checkbox("Показать модель", true);

    public double cameraX, cameraY, cameraZ;
    public float cameraYaw, cameraPitch;
    private double prevCameraX, prevCameraY, prevCameraZ;
    private float prevCameraYaw, prevCameraPitch;
    private boolean firstTick = true;
    private double savedX, savedY, savedZ;
    private float savedYaw, savedPitch, savedHeadYaw;
    OtherClientPlayerEntity fakePlayer;

    private FreeCam() {
        super("FreeCam", Category.PLAYER, "Свободная камера");
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null) return;
            handleTick();
        }
        if (event instanceof EventTravel e) {
            e.cancel();
        }
        if (event instanceof EventSendPacket e) {
            if (e.packet instanceof PlayerMoveC2SPacket) {
                e.cancel();
            }
        }
    };

    @Override
    protected void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        savedX = mc.player.getX();
        savedY = mc.player.getY();
        savedZ = mc.player.getZ();
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();
        savedHeadYaw = mc.player.getHeadYaw();
        cameraX = savedX;
        cameraY = savedY;
        cameraZ = savedZ;
        cameraYaw = savedYaw;
        cameraPitch = savedPitch;
        prevCameraX = cameraX;
        prevCameraY = cameraY;
        prevCameraZ = cameraZ;
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
        firstTick = true;
        if (showFakePlayer.get()) {
            spawnFakePlayer();
        }
    }

    @Override
    protected void onDisable() {
        removeFakePlayer();
        if (mc.player != null) {
            mc.player.setPosition(savedX, savedY, savedZ);
            mc.player.setYaw(savedYaw);
            mc.player.setPitch(savedPitch);
            mc.player.setHeadYaw(savedHeadYaw);
            mc.player.setVelocity(Vec3d.ZERO);
            mc.player.noClip = false;
        }
    }

    private void handleTick() {
        if (!firstTick) {
            prevCameraX = cameraX;
            prevCameraY = cameraY;
            prevCameraZ = cameraZ;
            prevCameraYaw = cameraYaw;
            prevCameraPitch = cameraPitch;
        } else {
            firstTick = false;
        }
        cameraYaw = mc.player.getYaw();
        cameraPitch = mc.player.getPitch();
        mc.player.noClip = true;
        mc.player.setOnGround(false);
        mc.player.setVelocity(Vec3d.ZERO);
        mc.player.getAbilities().flying = false;
        double forward = 0, strafe = 0, vertical = 0;
        if (mc.options.forwardKey.isPressed()) forward++;
        if (mc.options.backKey.isPressed()) forward--;
        if (mc.options.leftKey.isPressed()) strafe++;
        if (mc.options.rightKey.isPressed()) strafe--;
        if (mc.options.jumpKey.isPressed()) vertical++;
        if (mc.options.sneakKey.isPressed()) vertical--;
        double yawRad = Math.toRadians(cameraYaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double moveX = forward * -sin + strafe * cos;
        double moveZ = forward * cos + strafe * sin;
        double moveY = vertical;
        double horizontalLength = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (horizontalLength > 0) {
            moveX /= horizontalLength;
            moveZ /= horizontalLength;
        }
        double spd = speed.get();
        cameraX += moveX * spd;
        cameraY += moveY * spd;
        cameraZ += moveZ * spd;
        mc.player.setPosition(cameraX, cameraY, cameraZ);
        mc.player.setVelocity(Vec3d.ZERO);
    }

    private void spawnFakePlayer() {
        if (mc.world == null || mc.player == null) return;
        fakePlayer = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        fakePlayer.copyFrom(mc.player);
        fakePlayer.setPosition(savedX, savedY, savedZ);
        fakePlayer.setYaw(savedYaw);
        fakePlayer.setPitch(savedPitch);
        fakePlayer.setHeadYaw(savedHeadYaw);
        fakePlayer.setBodyYaw(savedYaw);
        fakePlayer.getInventory().clone(mc.player.getInventory());
        mc.world.addEntity(fakePlayer);
    }

    private void removeFakePlayer() {
        if (fakePlayer != null) {
            fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
    }

    public boolean isActive() {
        return isEnabled() && mc.player != null && mc.world != null;
    }
    public double getPrevCameraX() { return prevCameraX; }
    public double getPrevCameraY() { return prevCameraY; }
    public double getPrevCameraZ() { return prevCameraZ; }
}