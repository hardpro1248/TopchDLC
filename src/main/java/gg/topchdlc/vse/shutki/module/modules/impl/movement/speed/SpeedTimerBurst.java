package gg.topchdlc.vse.shutki.module.modules.impl.movement.speed;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.player.MoveUtility;

/**
 * Create by daun kvass
 */
public class SpeedTimerBurst extends Choice {

    private final CheckBox autoJump = checkbox("Авто Прыжок", true);

    private final SliderSetting slowTimer = sliderSetting("Таймер Замедления", 0.30f, 0.01f, 1.00f).increment(0.01f);
    private final SliderSetting slowSec = sliderSetting("Секунды Замедления", 3.0f, 0.1f, 20.0f).increment(0.1f);

    private final SliderSetting fastTimer = sliderSetting("Таймер Ускорения", 1.60f, 1.00f, 20.00f).increment(0.01f);
    private final SliderSetting fastSec = sliderSetting("Секунды Ускорения", 4.0f, 0.1f, 20.0f).increment(0.1f);

    private long phaseStartTime = 0L;
    private boolean isSlowPhase = true;

    public SpeedTimerBurst() {
        super("TimerPulse");
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;

        if (event instanceof EventInput) {
            if (autoJump.get() && MoveUtility.hasMovement(mc.player.input.playerInput) && mc.player.isOnGround()) {
                mc.player.jump();
            }
        }

        if (event instanceof EventGameTick) {
            if (!MoveUtility.hasMovement(mc.player.input.playerInput)) {
                resetTimer();
                return;
            }

            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - phaseStartTime;

            if (isSlowPhase) {
                Client.TIMER = slowTimer.get();

                long slowDurationMs = (long) (slowSec.get() * 1000.0f);
                if (elapsedTime >= slowDurationMs) {
                    isSlowPhase = false;
                    phaseStartTime = currentTime;
                }
            } else {
                Client.TIMER = fastTimer.get();

                long fastDurationMs = (long) (fastSec.get() * 1000.0f);
                if (elapsedTime >= fastDurationMs) {
                    isSlowPhase = true;
                    phaseStartTime = currentTime;
                }
            }
        }
    }

    private void resetTimer() {
        Client.TIMER = 1.0f;
        isSlowPhase = true;
        phaseStartTime = System.currentTimeMillis();
    }

    @Override
    public void onEnabled() {
        resetTimer();
    }

    @Override
    public void onDisabled() {
        resetTimer();
    }
}