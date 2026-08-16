package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.math.TimeUtility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * Действия на среднюю кнопку мыши (колесико).
 * По умолчанию: бросок жемчуга Края, если он есть в инвентаре.
 */
public class MiddleClick extends Module {
    public static final MiddleClick INSTANCE = new MiddleClick();

    public enum Action {
        Pearl("Жемчуг"),
        Friend("Добавить в друзья"),
        None("Ничего");

        private final String name;
        Action(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    public final EnumSetting<Action> action = enumSetting("Действие", Action.Pearl);
    public final CheckBox onlyInGame = checkbox("Только в игре", true);

    private final TimeUtility cooldown = new TimeUtility();

    private MiddleClick() {
        super("MiddleClick", Category.Misc, "Действия на среднюю кнопку мыши");

    }

    private final EventBus<Event> events = event -> {
        if (!(event instanceof EventKey e)) return;
        if (e.action != 1) return;
        // Код средней кнопки мыши: -100 - 2 = -102
        if (e.key != -102) return;
        if (mc.player == null) return;
        if (onlyInGame.get() && mc.currentScreen != null) return;
        if (!cooldown.reached(200)) return;
        cooldown.reset();

        switch (action.get()) {
            case Pearl -> throwPearl();
            case Friend -> addFriend();
            case None -> {}
        }
    };

    private void throwPearl() {
        if (mc.player == null) return;
        int slot = findPearlSlot();
        if (slot == -1) return;
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
        }
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
    }

    private int findPearlSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ENDER_PEARL) return i;
        }
        return -1;
    }

    private void addFriend() {
        if (mc.player == null || mc.world == null) return;
        if (mc.targetedEntity instanceof PlayerEntity target) {
            String name = target.getGameProfile().name();
            if (Client.FRIENDS.isFriend(name)) {
                Client.FRIENDS.removeFriend(name);
                Client.NOTIFIES.add(net.minecraft.text.Text.of("Удален из друзей: " + name), gg.topchdlc.api.render.system.IconUse.REMOVEFRIEND, 2000);
            } else {
                Client.FRIENDS.addFriend(name);
                Client.NOTIFIES.add(net.minecraft.text.Text.of("Добавлен в друзья: " + name), gg.topchdlc.api.render.system.IconUse.ADDFRIEND, 2000);
            }
            Client.FRIENDS.save();
        }
    }

    @Override
    protected void onEnable() {
        Client.EVENTS.register(events);
    }

    @Override
    protected void onDisable() {
        Client.EVENTS.unregister(events);
    }
}
