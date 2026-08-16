package gg.topchdlc.vse.shutki.module.modules.impl.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;

public class AutoLeave extends Module {
    public static final AutoLeave INSTANCE = new AutoLeave();

    private AutoLeave() {
        super("Auto Leave", Category.PLAYER, "Ливает от игроков в радиусе");
    }

    public SliderSetting radius = sliderSetting("Radius", 16, 1, 64);
    public CheckBox ignoreFriends = checkbox("Ignore friends", true);
    public EnumSetting<mode> modes = enumSetting("Mode", mode.Disconect);
    @AllArgsConstructor
    @Getter
    public enum mode {
        Hub("hub"),
        Disconect("Disconnect");
        final String renderName;
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null) return;
            float maxDistSq = radius.get() * radius.get();

            if (modes.is(mode.Disconect)) {
                for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                    if (p == mc.player) continue;

                    if (ignoreFriends.get() && Client.FRIENDS.isFriend(p)) continue;
                    if (mc.player.squaredDistanceTo(p) <= maxDistSq) {
                        mc.getNetworkHandler().getConnection().disconnect(Text.of("leave for  " + p.getName().getString()));
                        this.toggle();
                        return;
                    }
                }
            }

            if(modes.is(mode.Hub)){
                for (AbstractClientPlayerEntity p : mc.world.getPlayers()){
                    if (p == mc.player) continue;

                    if (ignoreFriends.get() && Client.FRIENDS.isFriend(p)) continue;
                    if (mc.player.squaredDistanceTo(p) <= maxDistSq){
                        mc.player.networkHandler.sendChatMessage("/hub");
                        this.toggle();
                        return;
                    }
                }
            }
        }
    };
}