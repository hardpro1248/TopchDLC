package gg.topchdlc.vse.shutki.module.modules.impl.farm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.itemlist.ItemListSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.blocklist.BlockListSetting;
import gg.topchdlc.vse.utils.math.TimeUtility;

/**
 * Create by daun kvass
 */
public class ChestStealer extends Module {
    public static final ChestStealer INSTANCE = new ChestStealer();

    private final SliderSetting lootDelay = sliderSetting("Задержка лута (мс)", 100f, 20f, 1000f).increment(10f);

    private final CheckBox onlyValuable = checkbox("Ресы токо под данж вардена", false);
    private final ItemListSetting customItems = itemListSetting("Доп. предметы")
            .visible(onlyValuable::get);
    private final BlockListSetting customBlocks = blockListSetting("Доп. блоки")
            .visible(onlyValuable::get);

    private final CheckBox autoHub = checkbox("Авто /hub", false);
    private final SliderSetting hubDelay = sliderSetting("КД /hub (мс)", 3000f, 500f, 30000f).increment(500f).visible(autoHub::get);
    private final CheckBox autoClose = checkbox("Авто закрытие", true);

    private final TimeUtility lootTimer = new TimeUtility();
    private final TimeUtility hubTimer = new TimeUtility();

    private boolean looting = false;
    private boolean lootDone = false;

    private ChestStealer() {
        super("Stealer", Category.PLAYER, "само забирает ресурсы из контейнеров");
    }


    @Override
    protected void onEnable() {
        super.onEnable();
        looting = false;
        lootDone = false;
        lootTimer.reset();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        looting = false;
        lootDone = false;
    }

    private boolean isDefaultValuable(ItemStack stack) {
        var item = stack.getItem();
        return item == Items.TOTEM_OF_UNDYING
                || item == Items.DRAGON_HEAD
                || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.WITHER_SKELETON_SKULL
                || item == Items.SKELETON_SKULL
                || item == Items.CREEPER_HEAD
                || item == Items.SNOWBALL
                || item == Items.SPLASH_POTION
                || item == Items.EXPERIENCE_BOTTLE
                || item == Items.AMETHYST_SHARD
                || item == Items.ZOMBIE_HEAD
                || item == Items.ENDER_PEARL
                || item == Items.IRON_NUGGET
                || item == Items.GUNPOWDER
                || item == Items.NETHERITE_SCRAP
                || item == Items.PIGLIN_HEAD
                || item == Items.PLAYER_HEAD
                || item == Items.ZOMBIE_VILLAGER_SPAWN_EGG
                || item == Items.TNT
                || item == Items.ARROW
                || item == Items.TIPPED_ARROW
                || item == Items.SPECTRAL_ARROW
                || item == Items.TRIPWIRE_HOOK
                || item == Items.PAPER;
    }

    private boolean shouldSteal(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (!onlyValuable.get()) return true;

        if (isDefaultValuable(stack)) return true;

        if (customItems.contains(stack.getItem())) return true;

        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (customBlocks.contains(block)) return true;
        }

        return false;
    }

    EventBus<Event> events = event -> {
        if (!(event instanceof EventGameTick)) return;
        if (mc.player == null || mc.world == null) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            looting = false;
            lootDone = false;
            return;
        }
        if (!lootTimer.reached((long) lootDelay.get(), false)) return;
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler == null) return;
        int totalSlots = handler.slots.size();
        int containerSlots = totalSlots - 36;
        if (containerSlots <= 0) return;

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = handler.slots.get(i).getStack();

            if (shouldSteal(stack)) {
                mc.interactionManager.clickSlot(
                        handler.syncId,
                        i,
                        0,
                        SlotActionType.QUICK_MOVE,
                        mc.player
                );
                lootTimer.reset();
                looting = true;
                lootDone = false;
                return;
            }
        }

        if (looting || !lootDone) {
            lootDone = true;
            looting = false;
            if (autoHub.get() && hubTimer.reached((long) hubDelay.get(), false)) {
                mc.player.networkHandler.sendChatMessage("/hub");
                hubTimer.reset();
            }
            if (autoClose.get()) {
                mc.execute(() -> mc.player.closeHandledScreen());
            }
        }
    };
}