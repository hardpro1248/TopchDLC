/*package gg.topchdlc.api.party;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.other.commands.impl.GpsCommand;
import gg.topchdlc.vse.utils.client.client.ClientSettings;

import java.util.List;

public class PartyManager implements MinecraftHolder {

    public static final PartyManager INSTANCE = new PartyManager();

    private int tickCounter = 0;
    private static final int POLL_INTERVAL = 20;

    public PartyManager() {
        Client.EVENTS.register(this);
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) {
            if (mc.player == null) return;
            tickCounter++;
            if (tickCounter % POLL_INTERVAL != 0) return;
            PartyApiClient.fetchPartyStateAsync();
            PartyApiClient.fetchInvitesAsync();
            PartyApiClient.sendPositionAsync();
        }

        if (event instanceof EventKey e && e.getAction() == 1) {
            if (!ClientSettings.INSTANCE.partyGpsBind.matches(e)) return;
            if (mc.player == null || mc.currentScreen != null) return;

            List<PartyPlayerPos> members = PartyApiClient.getCached();
            if (members.isEmpty()) return;

            if (members.size() == 1) {
                PartyPlayerPos p = members.get(0);
                GpsCommand.INSTANCE.setTarget(p.x(), p.z());
            } else {
                mc.execute(() -> mc.setScreen(new PartyGpsScreen(members)));
            }
        }
    };
}
*/