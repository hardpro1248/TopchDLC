package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.*;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

public class NoFall extends Module {
    public static final NoFall INSTANCE = new NoFall();
    private NoFall() {
        super("No Fall", Category.MOVEMENT, "Отменяет урон при падении");
    }

    private enum Mode { Packet, Motion, MatrixGround, JumpReset, GrimLast,Test }
    private final EnumSetting<Mode> mode = enumSetting("Reset mode", Mode.Packet);

    @Override
    protected void onEnable() {
        flags = 0;
        shouldReset = false;
    }

    int flags = 0;
    boolean shouldReset = false;
    boolean lastGround = false;

    private Vec3d vec3 = new Vec3d(1337.0,0.0,1337.0);
    private double prevFallDistance = 0.0;
    private boolean prevOnGround = false;
    private boolean flag = false;

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            switch (mode.get()) {
                case Motion, Packet -> {
                    Block block = mc.world.getBlockState(mc.player.getBlockPos().add(0, (int) -Math.min(2F, Math.abs(mc.player.getVelocity().y) + 1), 0)).getBlock();
                    if (!block.getDefaultState().isAir() && !block.getDefaultState().isLiquid() && mc.player.fallDistance > 3.25) {
                        if (mode.is(Mode.Packet)) {
                            NetworkUtility.send(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 1e-9, mc.player.getZ(), Client.ROTATION.getRotate().getYaw(), Client.ROTATION.getRotate().getPitch(), false, false));
                        } else {
                            mc.player.setVelocity(0, 0.1, 0);
                        }
                        mc.player.fallDistance = 0;
                    }
                }
                case Test -> {
                    if (mc.player.fallDistance > 3.25);{
                        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false));
                    }
                }
                case MatrixGround -> {
                    if (!shouldReset) shouldReset = mc.player.fallDistance > 3.25;
                }
                case JumpReset -> {
                    BlockState block = mc.world.getBlockState(mc.player.getBlockPos().add(0, -1, 0));
                    if (mc.player.fallDistance > 3.25) {
                        shouldReset = true;
                        mc.player.fallDistance = 0;
                    }
                }
            }
        }

        if (event instanceof EventMove move) {
            switch (mode.get()) {
                case GrimLast -> {
                    if (move.ground && !prevOnGround && prevFallDistance >= 3.0) {
                        flag = true;
                        move.x = vec3.x;
                        move.y = vec3.y;
                        move.z = vec3.z;
                        mc.player.fallDistance = 0.0;
                    }
                    prevOnGround = move.ground;
                    prevFallDistance = mc.player.fallDistance;

                    if (flag && mc.player.isOnGround()) {
                        flag = false;
                    }
                }
            }
        }

        if (event instanceof EventSendPacket p) {
            if (p.packet instanceof PlayerMoveC2SPacket f) {
                switch (mode.get()) {
                    case MatrixGround -> {
                        if (f.isOnGround() && shouldReset) {
                            p.cancel();
                            NetworkUtility.sendWithoutEvent(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 1e-4, mc.player.getZ(), Client.ROTATION.getRotate().getYaw(), Client.ROTATION.getRotate().getPitch(), false, false));
                        }
                    }
                }
            }
        }
        if (event instanceof EventMove e) {
            if (mode.is(Mode.JumpReset) && shouldReset && e.ground) {
                e.ground = false;
                e.y += 1e-2;
            }
        }
        if (event instanceof EventReceivePacket p) {
            if (p.packet instanceof PlayerPositionLookS2CPacket l) {
                if (mode.is(Mode.JumpReset)) {
                    flags++;
                }
            }
        }
        if (event instanceof EventInput e) {
            switch (mode.get()) {
                case MatrixGround -> {
                    if (shouldReset && mc.player.isOnGround()) {
                        e.setJump(true);
                        e.setSneak(true);
                        e.setStrafe(0);
                        e.setForward(0);
                        shouldReset = false;
                    }
                }
                case JumpReset -> {
                    if (shouldReset) {
                        e.setJump(true);
                        e.setSneak(true);
                        e.setStrafe(0);
                        e.setForward(0);
                        if (mc.player.isOnGround()) {
                            shouldReset = false;
                        }
                    }
                }
            }
        }
    };
}
