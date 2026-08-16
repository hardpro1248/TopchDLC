package gg.topchdlc.vse.shutki.module.modules.impl.combat.velocity;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;

public class VelocityMatrix extends Choice {
    public VelocityMatrix() {
        super("Matrix");
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventGameTick) {
            if (mc.player.hurtTime > 0 && !mc.player.isOnGround()) {
                double var3 = mc.player.getYaw() * 0.017453292F;
                double var5 = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
                mc.player.setVelocity(-Math.sin(var3) * var5, mc.player.getVelocity().y, Math.cos(var3) * var5);
                mc.player.setSprinting(mc.player.age % 2 != 0);
            }
        }
    }
}
