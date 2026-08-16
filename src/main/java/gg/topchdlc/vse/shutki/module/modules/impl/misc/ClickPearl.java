package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.player.swap.FastSwapUtil;
import gg.topchdlc.vse.utils.player.swap.LegitItemUseUtil;
import gg.topchdlc.vse.utils.player.swap.SwapUtility;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ClickPearl extends Module {
    public static final ClickPearl INSTANCE = new ClickPearl();
    private final TimeUtility enderpearltime = new TimeUtility();
    private final LegitItemUseUtil legitItemUse = new LegitItemUseUtil();

    public enum EnderPearl {
        Matrix("Matrix"),
        Grim("Grim");

        private final String name;
        EnderPearl(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private ClickPearl() {
        super("ClickPearl", Category.PLAYER, "Быстрое бросание эндер жемчуга");
    }

    public KeybindSetting enderpearl = keybindSetting("Бинд перла", -1);
    public EnumSetting<EnderPearl> enderpearlmode = enumSetting("Режим Перла", EnderPearl.Matrix);

    private final EventBus<Event> events = event -> {
        if (event instanceof EventKey e) {
            if (mc.player == null || e.action != 1) return;
            handleKeyPress(e);
        }
        if (event instanceof EventGameTick) {
            legitItemUse.onTick();
        }
    };

    private void handleKeyPress(EventKey e) {
        if (mc.player == null) return;

        if (enderpearl.getBind() != -1 && e.key == enderpearl.getBind()) {
            if (!enderpearltime.reached(200)) return;
            usePearl();
            enderpearltime.reset();
        }
    }

    private void usePearl() {
        if (Client.IS_PANIC || mc.player == null) return;

        int pearlSlot = findItemSlot(Items.ENDER_PEARL);
        if (pearlSlot == -1) return;

        switch (enderpearlmode.get()) {
            case Grim -> {
                if (!legitItemUse.isBusy()) {
                    legitItemUse.startLegitUse(pearlSlot);
                }
            }
            case Matrix -> SwapUtility.safeUseItem(pearlSlot);

        }
    }

    private int findItemSlot(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }

    @Override
    protected void onEnable() {
        Client.EVENTS.register(events);
    }

    @Override
    protected void onDisable() {
        Client.EVENTS.unregister(events);
        legitItemUse.reset();
        FastSwapUtil.reset();
    }
}