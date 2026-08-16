package gg.topchdlc.vse.shutki.module.modules.impl.combat.velocity;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.ArrayList;

public class VelocityGrimCancel extends Choice {
    public VelocityGrimCancel() {
        super("Grim Cancel");
    }

    int ticks;
    boolean waitingTransaction = false;
    final ArrayList<Integer> transactions = new ArrayList<>();
    final TimeUtility delay = new TimeUtility();

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventReceivePacket e) {
            if (mc.player == null) return;
            if (e.packet instanceof EntityVelocityUpdateS2CPacket pos && pos.getEntityId() == mc.player.getId()) {
                e.cancel();
                waitingTransaction = true;
                ticks = 4;
            }
            if (e.packet instanceof CommonPingS2CPacket ping) {
                if (waitingTransaction) {
                    transactions.add(ping.getParameter());
                    waitingTransaction = false;
                    e.cancel();
                }
            }
            if (e.packet instanceof PlayerPositionLookS2CPacket move) {
                for (Integer i : transactions) {
                    NetworkUtility.sendWithoutEvent(new CommonPongC2SPacket(i));
                }
                transactions.clear();
            }
        }
        if (event instanceof EventGameTick) {
            if (delay.reached(500, true) && !transactions.isEmpty()) {
                NetworkUtility.sendWithoutEvent(new CommonPongC2SPacket(transactions.getFirst()));
            }
        }
    }
}
