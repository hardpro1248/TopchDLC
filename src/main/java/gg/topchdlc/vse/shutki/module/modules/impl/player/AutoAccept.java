package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.Locale;
/**
 * Create by daun kvass
 */
public class AutoAccept extends Module {
    public static final AutoAccept INSTANCE = new AutoAccept();
    
    private final CheckBox onlyFriends = checkbox("Только друзья", false);
    private AutoAccept() {
        super("Auto Accept", Category.PLAYER, "само принимает тп");
    }
    EventBus<Event> events = event -> {
        if (mc.player == null || mc.world == null) return;
        
        if (event instanceof EventReceivePacket e) {
            if (e.getPacket() instanceof GameMessageS2CPacket packet) {
                String raw = packet.content().getString().toLowerCase(Locale.ROOT);
                if (raw.contains("телепортироваться") || 
                    raw.contains("has requested teleport") ||
                    raw.contains("запрос на телепортацию от игрока")||
                    raw.contains("просит к вам телепортироваться")) {
                    if (onlyFriends.get()) {
                        boolean isFriend = false;
                        for (String friend : Client.FRIENDS.getFriendList()) {
                            if (raw.contains(friend.toLowerCase(Locale.ROOT))) {
                                isFriend = true;
                                break;
                            }
                        }
                        if (!isFriend) return;
                    }
                   NetworkUtility.sendCommand("tpaccept");
                }
            }
        }
    };
}
