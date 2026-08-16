package gg.topchdlc.vse.shutki.module.modules.impl.combat.velocity;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class VelocityGrimNew extends Choice {
    public VelocityGrimNew() {
        super("Grim New");
    }

    boolean flag = false;
    int look = 0;

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventReceivePacket e) {
            if (mc.player == null) return;
            if (look > 0) {
                look--;
                return;
            }
            if (e.packet instanceof EntityVelocityUpdateS2CPacket pos && pos.getEntityId() == mc.player.getId()) {
                e.cancel();
                flag = true;
            }
            if (e.packet instanceof PlayerPositionLookS2CPacket) {
                look = 5;
            }
        }
        if (event instanceof EventGameTick) {
            if (flag && look <= 0) {
                NetworkUtility.send(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), Client.ROTATION.getRotate().getYaw(), Client.ROTATION.getRotate().getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
                NetworkUtility.send(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, BlockPos.ofFloored(mc.player.getEntityPos()), Direction.DOWN));
            }
        }
    }
}
