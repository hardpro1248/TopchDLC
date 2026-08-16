package gg.topchdlc.vse.shutki.module.modules.impl.movement.spider;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventMove;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class SpiderGrimGround extends Choice {
    public SpiderGrimGround() {
        super("Grim Ground");
    }

    final SliderSetting timer = sliderSetting("Timer", 0.5f, 0, 1).increment(0.1f);

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMove e) {
            if (!mc.player.isOnGround() && mc.player.horizontalCollision) {
                mc.player.setOnGround(true);
                e.ground = true;
                NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
                Client.TIMER = timer.get();
                mc.player.jump();
            }
        }
    }
}
