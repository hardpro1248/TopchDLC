package gg.topchdlc.vse.shutki.module.modules.impl.movement.timer;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;

/**
 * Create by daun kvass
 */
public class DefTimer extends Choice {
    public DefTimer() {
        super("TimerD");
    }
    public static final DefTimer INSTANCE = new DefTimer();

    private final SliderSetting strapon = sliderSetting("Strapon",1,0.1f,5).increment(0.1f);

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;
        if(event instanceof EventGameTick){
            Client.TIMER = strapon.get();
        }
    }
    @Override
    public void onEnabled() {

        Client.TIMER = 1.0f;
    }

    @Override
    public void onDisabled() {
        Client.TIMER = 1.0f;


    }
}
