package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventSendPacket;
import gg.topchdlc.api.events.list.EventTravel;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class AirStuck extends Module {
    public static final AirStuck INSTANCE = new AirStuck();
    private AirStuck() {
        super("AirStuck", Category.PLAYER, "Замораживает тебя, как в глыбе");
    }

    @AllArgsConstructor @Getter
    public enum Mode implements EnumChoice {
        Motion("Motion"), CancelPacket("Default"); final String renderName;
    }
    EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.Motion));
    EventBus<Event> events = event -> {
        if (event instanceof EventTravel e) {
            e.cancel();
        }
        if (event instanceof EventSendPacket e) {
            if (e.packet instanceof PlayerMoveC2SPacket && mode.is(Mode.CancelPacket)) {
                e.cancel();
            }
        }
    };
}