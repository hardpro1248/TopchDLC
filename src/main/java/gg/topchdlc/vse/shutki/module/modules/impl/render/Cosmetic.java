package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.render.cosmetic.*;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;

/**
 * Create by daun kvass
 */
public class Cosmetic extends Module {
    public static final Cosmetic INSTANCE = new Cosmetic();

    public final Group chinaHat   = group("ChinaHat").toggleable(false);
    public final Group jumpCircle  = group("JumpCircle").toggleable(false);
    public final Group wings      = group("Wings").toggleable(false);
    public final Group trail = group("Trail").toggleable(false);

    private Cosmetic() {
        super("Cosmetic", Category.RENDER, "Косметические эффекты");
        trail.add(Trails.INSTANCE.self);
        trail.add(Trails.INSTANCE.friends);
        trail.add(Trails.INSTANCE.colorEnd);
        trail.add(Trails.INSTANCE.colorStart);
        trail.add(Trails.INSTANCE.useCustomColor);
        trail.add(Trails.INSTANCE.duration);
        trail.add(Trails.INSTANCE.opacity);


        chinaHat.add(ChinaHat.INSTANCE.mode);
        chinaHat.add(ChinaHat.INSTANCE.self);
        chinaHat.add(ChinaHat.INSTANCE.friends);
        chinaHat.add(ChinaHat.INSTANCE.others);
        chinaHat.add(ChinaHat.INSTANCE.useCustomColor);
        chinaHat.add(ChinaHat.INSTANCE.customColor);

        jumpCircle.add(JumpCircle.INSTANCE.image);

        wings.add(Wings.INSTANCE.showOnSelf);
        wings.add(Wings.INSTANCE.showOnFriends);
        wings.add(Wings.INSTANCE.showOnOthers);
        wings.add(Wings.INSTANCE.useCustomColor);
        wings.add(Wings.INSTANCE.color);
        wings.add(Wings.INSTANCE.size);
        wings.add(Wings.INSTANCE.fillAlpha);

    }

    @Override
    protected void onEnable() {
        syncAll();
        Client.EVENTS.register(tickBus);
        Client.EVENTS.register(ChinaHat.INSTANCE.bus);
        Client.EVENTS.register(JumpCircle.INSTANCE.events);
        Client.EVENTS.register(Trails.INSTANCE.bus);
        Client.EVENTS.register(Wings.INSTANCE.bus);
    }

    @Override
    protected void onDisable() {
        disableAll();
        Client.EVENTS.unregister(tickBus);
        Client.EVENTS.unregister(ChinaHat.INSTANCE.bus);
        Client.EVENTS.unregister(JumpCircle.INSTANCE.events);
        Client.EVENTS.unregister(Wings.INSTANCE.bus);
        Client.EVENTS.unregister(Trails.INSTANCE.bus);
    }

    private final EventBus<Event> tickBus = event -> {
        if (event instanceof EventGameTick) syncAll();
    };

    private void syncAll() {
        ChinaHat.INSTANCE.setEnabled(chinaHat.isEnabled());
        JumpCircle.INSTANCE.setEnabled(jumpCircle.isEnabled());
        Trails.INSTANCE.setEnabled(trail.isEnabled());
        Wings.INSTANCE.setEnabled(wings.isEnabled());
    }

    private void disableAll() {
        ChinaHat.INSTANCE.setEnabled(false);
        Trails.INSTANCE.setEnabled(false);
        JumpCircle.INSTANCE.setEnabled(false);
        Wings.INSTANCE.setEnabled(false);
    }
}
