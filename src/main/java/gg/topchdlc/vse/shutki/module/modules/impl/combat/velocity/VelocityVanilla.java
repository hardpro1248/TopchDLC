package gg.topchdlc.vse.shutki.module.modules.impl.combat.velocity;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class VelocityVanilla extends Choice {
    public VelocityVanilla() {
        super("Vanilla");
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventReceivePacket e) {
            if (e.packet instanceof EntityVelocityUpdateS2CPacket pos && pos.getEntityId() == mc.player.getId()) {
                event.cancel();
            }
        }
    }
}
