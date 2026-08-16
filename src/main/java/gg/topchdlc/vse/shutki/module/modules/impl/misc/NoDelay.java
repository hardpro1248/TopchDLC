package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
/**
CODE Сделан дауном на kvaus
 */
public class NoDelay extends Module {
    public static final NoDelay INSTANCE = new NoDelay();

    private NoDelay() {
        super("NoDelay", Category.PLAYER, "Убирает задержку на разные действия");
    }
    public final CheckBox noJumpDelay = checkbox("No Jump Delay", true);
    public final CheckBox noRightClickDelay = checkbox("No RMB Delay", true);
    public final CheckBox fastXP = checkbox("Fast XP", false);
    public final SliderSetting xpDelay = sliderSetting("XP Delay (ticks)", 1, 0, 10)
            .visible(fastXP::get);
    public final SliderSetting xpPerTick = sliderSetting("XP Per Tick", 1, 1, 5)
            .visible(fastXP::get);
    private int xpTickCounter = 0;
    /** code  */
    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) onTick();
    };

    private void onTick() {
        if (!isEnabled()) return;
        if (!fastXP.get()) return;
        if (mc.player == null || mc.currentScreen != null) return;
        boolean mainHand = mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE;
        boolean offHand = mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE;
        if (!mainHand && !offHand) {
            xpTickCounter = 0;
            return;
        }
        if (!mc.options.useKey.isPressed()) {
            xpTickCounter = 0;
            return;
        }
        xpTickCounter++;
        int delay = xpDelay.getInt();
        if (delay > 0 && xpTickCounter % (delay + 1) != 0) return;
        Hand hand = mainHand ? Hand.MAIN_HAND : Hand.OFF_HAND;
        int count = xpPerTick.getInt();
        for (int i = 0; i < count; i++) {
            boolean still = hand == Hand.MAIN_HAND
                    ? mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE
                    : mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE;
            if (!still) break;
            mc.interactionManager.interactItem(mc.player, hand);
        }
    }
    @Override
    protected void onDisable() {
        super.onDisable();
        xpTickCounter = 0;
    }
}