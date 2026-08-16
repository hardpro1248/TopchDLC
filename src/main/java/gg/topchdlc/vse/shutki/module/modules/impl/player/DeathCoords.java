package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventSendPacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;

public class DeathCoords extends Module {
    public static final DeathCoords INSTANCE = new DeathCoords();
    private DeathCoords() {
        super("DeathCoords", Category.PLAYER, "пишет корды смерти");
        setEnabled(true, false);
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventSendPacket ev) {
            if (ev.packet instanceof DeathMessageS2CPacket) {
                String text = String.format("%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
                ChatUtility.send(Text.literal(text).styled(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(text))));
            }
        }
    };
}
