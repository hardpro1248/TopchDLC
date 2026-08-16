package gg.topchdlc.vse.shutki.module.modules.impl.movement.speed;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.*;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

/**
 * Create by daun kvass
 */
public class SpeedRw extends Choice {
public SpeedRw(){super("SpeedGrimTimer");}
    private int ticks;
    private int groundTicks;

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;
        if(event instanceof EventGameTick){
            if(mc.options.jumpKey.isPressed()){
                mc.options.jumpKey.setPressed(false);
            }
        }
        if (event instanceof EventPostMove) {
            Client.TIMER = 1.7f;

            if (ticks > 3) {
                double bst = 0.03;
                if (ticks % 2 == 0) {
                    mc.player.addVelocityInternal(new Vec3d(0, 0.03F, 0));
                    if (mc.player.isOnGround()) bst = 0.085;
                }

                double yaw = Math.toRadians(MoveUtility.getdir());
                double xt = -Math.sin(yaw);
                double zt = Math.cos(yaw);
                if (MoveUtility.getdir() == -1.0F) {
                    xt = 0.0;
                    zt = 0.0;
                }
                mc.player.addVelocityInternal(new Vec3d(xt * bst, 0, zt * bst));
            }

            ticks++;
        }
        if (event instanceof EventInput) {
            if (mc.player.verticalCollision) groundTicks++;
            else groundTicks = 0;

            if (groundTicks >= 1) mc.player.jump();
        }
        if (event instanceof EventPostMotion) {
            if (ticks % 2 == 0) {
                Client.TIMER = 0.3f;
                NetworkUtility.sendSilentPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }
        if (event instanceof EventPacket e) {
            if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
                if (ticks % 2 == 1) {
                    ticks++;
                }

                Client.TIMER = 1f;
            }
        }
    }
    @Override
    public void onEnabled() {
        ticks = 0;
        groundTicks = 0;
        Client.TIMER = 1.0f;
    }

    @Override
    public void onDisabled() {
        Client.TIMER = 1.0f;
        ticks = 0;

    }
}

