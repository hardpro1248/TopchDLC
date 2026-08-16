package gg.topchdlc.vse.shutki.module.modules.impl.farm;

import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.Client;
import gg.topchdlc.api.autobuy.AutoBuyUtil;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
/**
 * Create by daun kvass
 */
public class AutoSosiski extends Module {
    public static final AutoSosiski INSTANCE = new AutoSosiski();

    private final SliderSetting actionDelay = sliderSetting("Задержка", 300f, 100f, 1000f)
            .increment(50f);
    private final SliderSetting targetLevel = sliderSetting("Целевой уровень защиты", 5f, 1f, 5f)
            .increment(1f);
    private final CheckBox autoCraft = checkbox("AutoCraft", false);
    private final CheckBox autoSellAfter = checkbox("AutoSell после", false);
    private final SliderSetting sellPrice = sliderSetting("Цена продажи", 5000f, 100f, 999999f)
            .increment(100f).visible(autoSellAfter::get);
    private enum State {
        IDLE,
        CRAFT_OPEN_SHOP,
        CRAFT_WAIT_SHOP,
        CRAFT_CLICK_GOLD,
        CRAFT_WAIT_GOLD_MENU,
        CRAFT_SHIFT_EMERALD,
        CRAFT_CLOSE_SHOP,
        CRAFT_WAIT_SHOP_CLOSE,
        CRAFT_OPEN_CRAFT,
        CRAFT_WAIT_CRAFT,
        CRAFT_PLACE_SLOT1,
        CRAFT_PLACE_SLOT2,
        CRAFT_PLACE_SLOT3,
        CRAFT_PLACE_SLOT4,
        CRAFT_PLACING,
        CRAFT_WAIT_RESULT,
        CRAFT_TAKE_RESULT,
        CRAFT_CLOSE_CRAFT,
        CRAFT_WAIT_CRAFT_CLOSE,
        BUY_XP_SEARCH,
        BUY_XP_WAIT_AH,
        BUY_XP_WAIT_RESULTS,
        BUY_XP_BUY_CLICK,
        BUY_XP_WAIT_BUY,
        BUY_XP_CLOSE,
        BUY_XP_WAIT_CLOSE,
        DROP_XP,
        WAIT_XP_PICKUP,
        FIND_ANVIL,
        OPEN_ANVIL,
        WAIT_ANVIL_OPEN,
        ANVIL_PLACE_ITEM1,
        ANVIL_PLACE_ITEM2,
        ANVIL_WAIT_RESULT,
        ANVIL_TAKE_RESULT,
        ANVIL_CLOSE,
        ANVIL_WAIT_CLOSE,
        SELL_SELECT_SLOT,
        SELL_WAIT_SLOT,
        SELL_SEND_CMD,
        SELL_WAIT_DONE,
        DONE,
        ERROR
    }

    private State state = State.IDLE;
    private final TimeUtility timer = new TimeUtility();
    private String errorMessage = "";
    private final List<Integer> bootSlots = new ArrayList<>();
    private int slot1 = -1, slot2 = -1;
    private int resultBootSlot = -1;
    private int xpBuyAttempts = 0;
    private static final int MAX_XP_BUY_ATTEMPTS = 5;
    private int craftGridSlotIdx = 0;
    private int craftClicksLeft = 0;
    private static final int[] CRAFT_GRID_SLOTS = {1, 3, 4, 6};
    private static final int CRAFT_PER_SLOT = 16;

    private AutoSosiski() {
        super("AutoSosiski", Category.PLAYER, "sosiski");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        reset();
        state = State.IDLE;
        ChatUtility.send(Text.literal("AutoSosiski Запущен").withColor(new Color(100, 255, 100).getRGB()));
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        reset();
    }

    private void reset() {
        state = State.IDLE;
        bootSlots.clear();
        slot1 = -1;
        slot2 = -1;
        resultBootSlot = -1;
        xpBuyAttempts = 0;
        errorMessage = "";
        timer.reset();
    }

    private int getProtectionLevel(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        ItemEnchantmentsComponent enchants = stack.getEnchantments();
        for (var entry : enchants.getEnchantmentEntries()) {
            String id = entry.getKey().getKey().toString();
            if (id.contains("protection")) {
                return entry.getIntValue();
            }
        }
        return getProtectionFromLore(stack);
    }
    private int getProtectionFromLore(ItemStack stack) {
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return -1;
        for (var line : lore.lines()) {
            String text = line.getString().toLowerCase();
            if (text.contains("защита") || text.contains("protection")) {
                if (text.contains(" v") || text.contains(" 5")) return 5;
                if (text.contains(" iv") || text.contains(" 4")) return 4;
                if (text.contains(" iii") || text.contains(" 3")) return 3;
                if (text.contains(" ii") || text.contains(" 2")) return 2;
                if (text.contains(" i") || text.contains(" 1")) return 1;
            }
        }
        return -1;
    }
    private boolean isDiamondBoots(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.DIAMOND_BOOTS;
    }
    private int countEmeralds() {
        if (mc.player == null) return 0;
        var inv = mc.player.getInventory();
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.getItem() == Items.EMERALD) total += s.getCount();
        }
        return total;
    }
    private List<Integer> findAllBoots() {
        List<Integer> slots = new ArrayList<>();
        if (mc.player == null) return slots;
        var inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (isDiamondBoots(stack)) {
                slots.add(i);
            }
        }
        return slots;
    }
    private int[] findMergePair(List<Integer> slots) {
        int target = (int) targetLevel.get();
        var inv = mc.player.getInventory();
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ItemStack a = inv.getStack(slots.get(i));
                ItemStack b = inv.getStack(slots.get(j));
                int lvlA = getProtectionLevel(a);
                int lvlB = getProtectionLevel(b);
                if (lvlA == lvlB && lvlA >= 0 && lvlA < target) {
                    return new int[]{slots.get(i), slots.get(j)};
                }
            }
        }
        return null;
    }
    private BlockPos findAnvil() {
        if (mc.player == null || mc.world == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    var block = mc.world.getBlockState(pos).getBlock();
                    if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
    private int inventorySlotToAnvilContainer(int invSlot) {
        if (invSlot >= 9 && invSlot <= 35) {
            return invSlot - 9 + 3;
        } else if (invSlot >= 0 && invSlot <= 8) {
            return invSlot + 30;
        }
        return -1;
    }
    private void error(String msg) {
        errorMessage = msg;
        ChatUtility.send(Text.literal(" §f" + msg).withColor(new Color(255, 100, 100).getRGB()));
        setEnabled(false);
    }
    private int getXpLevel() {
        if (mc.player == null) return 0;
        return mc.player.experienceLevel;
    }
    private boolean hasXpBottles() {
        if (mc.player == null) return false;
        var inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) return true;
        }
        return false;
    }
    EventBus<Event> events = event -> {
        if (!(event instanceof EventGameTick)) return;
        if (mc.player == null || mc.world == null) return;

        long delay = (long) actionDelay.get();

        switch (state) {
            case IDLE: {
                List<Integer> boots = findAllBoots();
                if (autoCraft.get() && boots.size() < 2) {
                    if (countEmeralds() >= 4) {
                        ChatUtility.send(Text.literal("Мало ботинок, крафчу из изумрудов").withColor(new Color(255, 220, 100).getRGB()));
                        state = State.CRAFT_OPEN_CRAFT;
                        timer.reset();
                        return;
                    }
                    ChatUtility.send(Text.literal("Мало ботинок, покупаю изумруды").withColor(new Color(255, 220, 100).getRGB()));
                    state = State.CRAFT_OPEN_SHOP;
                    timer.reset();
                    return;
                }
                if (boots.size() < 2) {
                    error("Недостаточно алмазных ботинок (нужно минимум 2)");
                    return;
                }
                int[] pair = findMergePair(boots);
                if (pair == null) {
                    int target = (int) targetLevel.get();
                    boolean hasTarget = false;
                    var inv = mc.player.getInventory();
                    for (int s : boots) {
                        if (getProtectionLevel(inv.getStack(s)) >= target) {
                            hasTarget = true;
                            resultBootSlot = s;
                            break;
                        }
                    }
                    if (hasTarget && autoSellAfter.get() && !autoCraft.get()) {
                        state = State.SELL_SELECT_SLOT;
                        timer.reset();
                    } else if (autoCraft.get()) {
                        if (countEmeralds() >= 4) {
                            ChatUtility.send(Text.literal("Нет пары, крафчу ещё ботинки").withColor(new Color(255, 220, 100).getRGB()));
                            state = State.CRAFT_OPEN_CRAFT;
                        } else {
                            ChatUtility.send(Text.literal("Нет пары, покупаю изумруды").withColor(new Color(255, 220, 100).getRGB()));
                            state = State.CRAFT_OPEN_SHOP;
                        }
                        timer.reset();
                    } else if (hasTarget && autoSellAfter.get()) {
                        state = State.SELL_SELECT_SLOT;
                        timer.reset();
                    } else {
                        error("Нет подходящей пары ботинок для соединения");
                    }
                    return;
                }
                slot1 = pair[0];
                slot2 = pair[1];
                if (getXpLevel() < 5 && !hasXpBottles()) {
                    xpBuyAttempts = 0;
                    state = State.BUY_XP_SEARCH;
                } else {
                    state = State.DROP_XP;
                }
                timer.reset();
                break;
            }
            case CRAFT_OPEN_SHOP: {
                if (!timer.reached(delay, false)) return;
                mc.player.networkHandler.sendChatMessage("/shop");
                state = State.CRAFT_WAIT_SHOP;
                timer.reset();
                break;
            }

            case CRAFT_WAIT_SHOP: {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    if (timer.reached(8000, false)) {
                        ChatUtility.send(Text.literal("меню  не открылся, пробую снова").withColor(new Color(255, 100, 100).getRGB()));
                        state = State.CRAFT_OPEN_SHOP;
                        timer.reset();
                    }
                    break;
                }
                boolean goldVisible = false;
                for (Slot slot : screen.getScreenHandler().slots) {
                    if (!slot.getStack().isEmpty() && slot.getStack().getItem() == Items.GOLD_INGOT) {
                        goldVisible = true;
                        break;
                    }
                }
                if (goldVisible) {
                    state = State.CRAFT_CLICK_GOLD;
                    timer.reset();
                } else if (timer.reached(10000, false)) {
                    ChatUtility.send(Text.literal("не появился в меню куда тыкать").withColor(new Color(255, 100, 100).getRGB()));
                    mc.currentScreen.close();
                    state = State.CRAFT_OPEN_SHOP;
                    timer.reset();
                }
                break;
            }

            case CRAFT_CLICK_GOLD: {
                if (!timer.reached(delay, false)) return;
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    state = State.CRAFT_OPEN_SHOP;
                    timer.reset();
                    return;
                }
                int goldSlot = -1;
                for (Slot slot : screen.getScreenHandler().slots) {
                    ItemStack stack = slot.getStack();
                    if (!stack.isEmpty() && stack.getItem() == Items.GOLD_INGOT) {
                        goldSlot = slot.id;
                        break;
                    }
                }
                if (goldSlot == -1) {
                    ChatUtility.send(Text.literal("куда тыкнуть не нашел в меню").withColor(new Color(255, 100, 100).getRGB()));
                    mc.currentScreen.close();
                    state = State.IDLE;
                    timer.reset();
                    return;
                }
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, goldSlot, 0, SlotActionType.PICKUP, mc.player);
                state = State.CRAFT_WAIT_GOLD_MENU;
                timer.reset();
                break;
            }

            case CRAFT_WAIT_GOLD_MENU: {
                if (!timer.reached(1200, false)) return;
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    state = State.CRAFT_OPEN_SHOP;
                    timer.reset();
                    break;
                }
                boolean emeraldVisible = false;
                for (Slot slot : screen.getScreenHandler().slots) {
                    if (!slot.getStack().isEmpty() && slot.getStack().getItem() == Items.EMERALD) {
                        emeraldVisible = true;
                        break;
                    }
                }
                if (emeraldVisible) {
                    state = State.CRAFT_SHIFT_EMERALD;
                    timer.reset();
                } else if (timer.reached(10000, false)) {
                    ChatUtility.send(Text.literal("Изумруд не появился в меню, пробую снова").withColor(new Color(255, 100, 100).getRGB()));
                    mc.currentScreen.close();
                    state = State.CRAFT_OPEN_SHOP;
                    timer.reset();
                }
                break;
            }
            case CRAFT_SHIFT_EMERALD: {
                if (!timer.reached(delay, false)) return;
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    state = State.CRAFT_OPEN_SHOP;
                    timer.reset();
                    return;
                }
                int emeraldSlot = -1;
                for (Slot slot : screen.getScreenHandler().slots) {
                    ItemStack stack = slot.getStack();
                    if (!stack.isEmpty() && stack.getItem() == Items.EMERALD) {
                        emeraldSlot = slot.id;
                        break;
                    }
                }
                if (emeraldSlot == -1) {
                    ChatUtility.send(Text.literal("Изумруд не найден в меню ").withColor(new Color(255, 100, 100).getRGB()));
                    mc.currentScreen.close();
                    state = State.IDLE;
                    timer.reset();
                    return;
                }
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, emeraldSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                ChatUtility.send(Text.literal("Купил изумруды, закрываю  меюн").withColor(new Color(255, 220, 100).getRGB()));
                state = State.CRAFT_CLOSE_SHOP;
                timer.reset();
                break;
            }

            case CRAFT_CLOSE_SHOP: {
                if (!timer.reached(delay, false)) return;
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = State.CRAFT_WAIT_SHOP_CLOSE;
                timer.reset();
                break;
            }

            case CRAFT_WAIT_SHOP_CLOSE: {
                if (mc.currentScreen != null) {
                    if (timer.reached(500, false)) {
                        mc.currentScreen.close();
                        timer.reset();
                    }
                    return;
                }
                if (!timer.reached(300, false)) return;
                state = State.CRAFT_OPEN_CRAFT;
                timer.reset();
                break;
            }

            case CRAFT_OPEN_CRAFT: {
                if (!timer.reached(delay, false)) return;
                mc.player.networkHandler.sendChatMessage("/craft");
                state = State.CRAFT_WAIT_CRAFT;
                timer.reset();
                break;
            }

            case CRAFT_WAIT_CRAFT: {
                if (mc.currentScreen instanceof CraftingScreen) {
                    state = State.CRAFT_PLACE_SLOT1;
                    timer.reset();
                    break;
                }
                if (timer.reached(8000, false)) {
                    ChatUtility.send(Text.literal(" §f/craft не открылся, пробую снова").withColor(new Color(255, 100, 100).getRGB()));
                    state = State.CRAFT_OPEN_CRAFT;
                    timer.reset();
                }
                break;
            }

            case CRAFT_PLACE_SLOT1:
            case CRAFT_PLACE_SLOT2:
            case CRAFT_PLACE_SLOT3:
            case CRAFT_PLACE_SLOT4: {
                if (!timer.reached(delay, false)) return;
                if (!(mc.currentScreen instanceof CraftingScreen screen)) {
                    state = State.CRAFT_OPEN_CRAFT;
                    timer.reset();
                    return;
                }
                CraftingScreenHandler handler = (CraftingScreenHandler) screen.getScreenHandler();
                var inv = mc.player.getInventory();
                ItemStack cursorStack = mc.player.currentScreenHandler.getCursorStack();
                if (!cursorStack.isEmpty()) {
                    int returnSlot = -1;
                    for (int i = 9; i < 36; i++) {
                        ItemStack s = inv.getStack(i);
                        if (s.getItem() == cursorStack.getItem() && s.getCount() < s.getMaxCount()) {
                            returnSlot = i - 9 + 10;
                            break;
                        }
                    }
                    if (returnSlot == -1) {
                        for (int i = 9; i < 36; i++) {
                            if (inv.getStack(i).isEmpty()) {
                                returnSlot = i - 9 + 10;
                                break;
                            }
                        }
                    }
                    if (returnSlot != -1) {
                        mc.interactionManager.clickSlot(handler.syncId, returnSlot, 0, SlotActionType.PICKUP, mc.player);
                    }
                    timer.reset();
                    return;
                }

                craftGridSlotIdx = state == State.CRAFT_PLACE_SLOT1 ? 0
                                 : state == State.CRAFT_PLACE_SLOT2 ? 1
                                 : state == State.CRAFT_PLACE_SLOT3 ? 2 : 3;
                int emeraldInvSlot = -1;
                for (int i = 0; i < 36; i++) {
                    if (inv.getStack(i).getItem() == Items.EMERALD) {
                        emeraldInvSlot = i;
                        break;
                    }
                }
                if (emeraldInvSlot == -1) {
                    int gridSlot = CRAFT_GRID_SLOTS[craftGridSlotIdx];
                    ItemStack inGrid = handler.getSlot(gridSlot).getStack();
                    if (inGrid.getItem() == Items.EMERALD && inGrid.getCount() > 0) {
                        craftClicksLeft = 0;
                        state = craftGridSlotIdx == 0 ? State.CRAFT_PLACE_SLOT2
                              : craftGridSlotIdx == 1 ? State.CRAFT_PLACE_SLOT3
                              : craftGridSlotIdx == 2 ? State.CRAFT_PLACE_SLOT4
                              : State.CRAFT_WAIT_RESULT;
                    } else {
                        ChatUtility.send(Text.literal(" §fИзумруды кончились во время крафта").withColor(new Color(255, 100, 100).getRGB()));
                        mc.currentScreen.close();
                        state = State.IDLE;
                    }
                    timer.reset();
                    return;
                }
                int emeraldContainerSlot = emeraldInvSlot < 9 ? emeraldInvSlot + 37 : emeraldInvSlot - 9 + 10;
                int gridSlot = CRAFT_GRID_SLOTS[craftGridSlotIdx];
                int alreadyInSlot = 0;
                ItemStack inGrid = handler.getSlot(gridSlot).getStack();
                if (inGrid.getItem() == Items.EMERALD) alreadyInSlot = inGrid.getCount();
                craftClicksLeft = CRAFT_PER_SLOT - alreadyInSlot;
                if (craftClicksLeft <= 0) {
                    state = craftGridSlotIdx == 0 ? State.CRAFT_PLACE_SLOT2
                          : craftGridSlotIdx == 1 ? State.CRAFT_PLACE_SLOT3
                          : craftGridSlotIdx == 2 ? State.CRAFT_PLACE_SLOT4
                          : State.CRAFT_WAIT_RESULT;
                    timer.reset();
                    return;
                }
                mc.interactionManager.clickSlot(handler.syncId, emeraldContainerSlot, 0, SlotActionType.PICKUP, mc.player);
                state = State.CRAFT_PLACING;
                timer.reset();
                break;
            }

            case CRAFT_PLACING: {
                if (!timer.reached(50, false)) return;
                if (!(mc.currentScreen instanceof CraftingScreen screen)) {
                    state = State.CRAFT_OPEN_CRAFT;
                    timer.reset();
                    return;
                }
                CraftingScreenHandler handler = (CraftingScreenHandler) screen.getScreenHandler();
                int gridSlot = CRAFT_GRID_SLOTS[craftGridSlotIdx];
                if (craftClicksLeft <= 0) {
                    ItemStack cursor = mc.player.currentScreenHandler.getCursorStack();
                    if (!cursor.isEmpty()) {
                        var inv = mc.player.getInventory();
                        int returnSlot = -1;
                        for (int i = 0; i < 36; i++) {
                            ItemStack s = inv.getStack(i);
                            if (s.getItem() == Items.EMERALD && s.getCount() < s.getMaxCount()) {
                                returnSlot = i < 9 ? i + 37 : i - 9 + 10;
                                break;
                            }
                        }
                        if (returnSlot == -1) {
                            for (int i = 9; i < 36; i++) {
                                if (inv.getStack(i).isEmpty()) {
                                    returnSlot = i - 9 + 10;
                                    break;
                                }
                            }
                        }
                        if (returnSlot != -1) {
                            mc.interactionManager.clickSlot(handler.syncId, returnSlot, 0, SlotActionType.PICKUP, mc.player);
                        } else {
                            mc.interactionManager.clickSlot(handler.syncId, gridSlot, 0, SlotActionType.THROW, mc.player);
                        }
                        timer.reset();
                        return;
                    }
                    state = craftGridSlotIdx == 0 ? State.CRAFT_PLACE_SLOT2
                          : craftGridSlotIdx == 1 ? State.CRAFT_PLACE_SLOT3
                          : craftGridSlotIdx == 2 ? State.CRAFT_PLACE_SLOT4
                          : State.CRAFT_WAIT_RESULT;
                    timer.reset();
                    return;
                }

                ItemStack cursor = mc.player.currentScreenHandler.getCursorStack();
                if (cursor.isEmpty()) {
                    var inv = mc.player.getInventory();
                    int nextEmeraldSlot = -1;
                    for (int i = 0; i < 36; i++) {
                        if (inv.getStack(i).getItem() == Items.EMERALD) {
                            nextEmeraldSlot = i < 9 ? i + 37 : i - 9 + 10;
                            break;
                        }
                    }
                    if (nextEmeraldSlot == -1) {
                        ChatUtility.send(Text.literal(" §fИзумруды кончились, не хватило для крафта").withColor(new Color(255, 100, 100).getRGB()));
                        mc.currentScreen.close();
                        state = State.IDLE;
                        timer.reset();
                        return;
                    }
                    mc.interactionManager.clickSlot(handler.syncId, nextEmeraldSlot, 0, SlotActionType.PICKUP, mc.player);
                    timer.reset();
                    return;
                }
                if (cursor.getItem() != Items.EMERALD) {
                    var inv = mc.player.getInventory();
                    int returnSlot = -1;
                    for (int i = 9; i < 36; i++) {
                        if (inv.getStack(i).isEmpty()) {
                            returnSlot = i - 9 + 10;
                            break;
                        }
                    }
                    if (returnSlot != -1) {
                        mc.interactionManager.clickSlot(handler.syncId, returnSlot, 0, SlotActionType.PICKUP, mc.player);
                    }
                    timer.reset();
                    return;
                }
                mc.interactionManager.clickSlot(handler.syncId, gridSlot, 1, SlotActionType.PICKUP, mc.player);
                craftClicksLeft--;
                timer.reset();
                break;
            }

            case CRAFT_WAIT_RESULT: {
                if (!timer.reached(delay, false)) return;
                if (!(mc.currentScreen instanceof CraftingScreen screen)) {
                    state = State.CRAFT_OPEN_CRAFT;
                    timer.reset();
                    return;
                }
                CraftingScreenHandler handler = (CraftingScreenHandler) screen.getScreenHandler();
                StringBuilder slotInfo = new StringBuilder("§eСлоты крафта: ");
                for (int gridSlot : CRAFT_GRID_SLOTS) {
                    ItemStack s = handler.getSlot(gridSlot).getStack();
                    slotInfo.append("[").append(gridSlot).append("]=")
                            .append(s.isEmpty() ? "пусто" : s.getItem().toString().replace("minecraft:", "") + "x" + s.getCount())
                            .append(" ");
                }
                ChatUtility.send(Text.literal(slotInfo.toString()).withColor(new Color(255, 220, 100).getRGB()));
                ItemStack result = handler.getSlot(0).getStack();
                ChatUtility.send(Text.literal("§eРезультат слот 0: " + (result.isEmpty() ? "пусто" : result.getItem() + "x" + result.getCount())).withColor(new Color(255, 220, 100).getRGB()));
                if (!result.isEmpty()) {
                    state = State.CRAFT_TAKE_RESULT;
                    timer.reset();
                } else if (timer.reached(5000, false)) {
                    ChatUtility.send(Text.literal(" §fРезультат крафта не появился").withColor(new Color(255, 100, 100).getRGB()));
                    mc.currentScreen.close();
                    state = State.IDLE;
                    timer.reset();
                }
                break;
            }

            case CRAFT_TAKE_RESULT: {
                if (!timer.reached(150, false)) return;
                if (!(mc.currentScreen instanceof CraftingScreen screen)) {
                    state = State.CRAFT_OPEN_CRAFT;
                    timer.reset();
                    return;
                }
                CraftingScreenHandler handler = (CraftingScreenHandler) screen.getScreenHandler();
                mc.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
                ChatUtility.send(Text.literal("§aAutoSosiski: §fСкрафтил ботинки из изумрудов!").withColor(new Color(100, 255, 100).getRGB()));
                state = State.CRAFT_CLOSE_CRAFT;
                timer.reset();
                break;
            }

            case CRAFT_CLOSE_CRAFT: {
                if (!timer.reached(delay, false)) return;
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = State.CRAFT_WAIT_CRAFT_CLOSE;
                timer.reset();
                break;
            }

            case CRAFT_WAIT_CRAFT_CLOSE: {
                if (mc.currentScreen != null) {
                    if (timer.reached(500, false)) {
                        mc.currentScreen.close();
                        timer.reset();
                    }
                    return;
                }
                if (!timer.reached(300, false)) return;
                state = State.IDLE;
                timer.reset();
                break;
            }

            case BUY_XP_SEARCH: {
                if (!timer.reached(delay, false)) return;
                if (xpBuyAttempts >= MAX_XP_BUY_ATTEMPTS) {
                    error("Не удалось купить опыт за " + MAX_XP_BUY_ATTEMPTS + " попыток");
                    return;
                }
                NetworkUtility.sendCommand("ah search опыт");
                state = State.BUY_XP_WAIT_AH;
                timer.reset();
                ChatUtility.send(Text.literal(": §fИщу опыт на аукционе...").withColor(new Color(255, 220, 100).getRGB()));
                break;
            }

            case BUY_XP_WAIT_AH: {
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    state = State.BUY_XP_WAIT_RESULTS;
                    timer.reset();
                    break;
                }
                if (timer.reached(10000, false)) {
                    ChatUtility.send(Text.literal(" §fТаймаут — аукцион не открылся, пробую снова").withColor(new Color(255, 100, 100).getRGB()));
                    xpBuyAttempts++;
                    state = State.BUY_XP_SEARCH;
                    timer.reset();
                }
                break;
            }

            case BUY_XP_WAIT_RESULTS: {
                if (!timer.reached(800, false)) return;
                state = State.BUY_XP_BUY_CLICK;
                timer.reset();
                break;
            }

            case BUY_XP_BUY_CLICK: {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    state = State.BUY_XP_SEARCH;
                    xpBuyAttempts++;
                    timer.reset();
                    return;
                }
                if (!timer.reached(3000, false)) return;
                int cheapestSlot = -1;
                int cheapestPricePerUnit = Integer.MAX_VALUE;
                for (Slot slot : screen.getScreenHandler().slots) {
                    ItemStack stack = slot.getStack();
                    if (stack.isEmpty()) continue;
                    if (stack.getItem() != Items.EXPERIENCE_BOTTLE) continue;
                    if (stack.getCount() < 64) continue;
                    long price = AutoBuyUtil.getPrice(stack);
                    if (price == Long.MAX_VALUE) continue;
                    int pricePerUnit = (int) Math.min(price / stack.getCount(), Integer.MAX_VALUE);
                    if (pricePerUnit < cheapestPricePerUnit) {
                        cheapestPricePerUnit = pricePerUnit;
                        cheapestSlot = slot.id;
                    }
                }
                if (cheapestSlot == -1) {
                    int cheapestPrice = Integer.MAX_VALUE;
                    for (Slot slot : screen.getScreenHandler().slots) {
                        ItemStack stack = slot.getStack();
                        if (stack.isEmpty()) continue;
                        if (stack.getItem() != Items.EXPERIENCE_BOTTLE) continue;
                        long price = AutoBuyUtil.getPrice(stack);
                        if (price == Long.MAX_VALUE) continue;
                        int ppu = (int) Math.min(price / Math.max(1, stack.getCount()), Integer.MAX_VALUE);
                        if (ppu < cheapestPrice) {
                            cheapestPrice = ppu;
                            cheapestSlot = slot.id;
                        }
                    }
                }
                if (cheapestSlot == -1) {
                    ChatUtility.send(Text.literal("Опыт не найден на аукционе").withColor(new Color(255, 100, 100).getRGB()));
                    xpBuyAttempts++;
                    state = State.BUY_XP_SEARCH;
                    timer.reset();
                    return;
                }
                mc.interactionManager.clickSlot(
                        screen.getScreenHandler().syncId,
                        cheapestSlot,
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                );
                ChatUtility.send(Text.literal("Покупаю опыт за " + cheapestPricePerUnit ).withColor(new Color(255, 220, 100).getRGB()));
                state = State.BUY_XP_WAIT_BUY;
                timer.reset();
                break;
            }

            case BUY_XP_WAIT_BUY: {
                if (!timer.reached(delay, false)) return;
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    for (Slot slot : screen.getScreenHandler().slots) {
                        ItemStack stack = slot.getStack();
                        if (stack.getItem() == Items.LIME_STAINED_GLASS_PANE ||
                            stack.getItem() == Items.GREEN_STAINED_GLASS_PANE) {
                            mc.interactionManager.clickSlot(
                                    screen.getScreenHandler().syncId,
                                    slot.id, 0, SlotActionType.PICKUP, mc.player
                            );
                            timer.reset();
                            return;
                        }
                    }
                }
                if (hasXpBottles()) {
                    ChatUtility.send(Text.literal("Опыт куплен, закрываю аукцион").withColor(new Color(100, 255, 100).getRGB()));
                    state = State.BUY_XP_CLOSE;
                    timer.reset();
                    return;
                }
                if (timer.reached(8000, false)) {
                    ChatUtility.send(Text.literal("Покупка не подтвердилась").withColor(new Color(255, 100, 100).getRGB()));
                    xpBuyAttempts++;
                    state = State.BUY_XP_SEARCH;
                    timer.reset();
                }
                break;
            }

            case BUY_XP_CLOSE: {
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = State.BUY_XP_WAIT_CLOSE;
                timer.reset();
                break;
            }
            case BUY_XP_WAIT_CLOSE: {
                if (mc.currentScreen != null) {
                    if (timer.reached(500, false)) {
                        mc.currentScreen.close();
                        timer.reset();
                    }
                    return;
                }
                if (!timer.reached(200, false)) return;
                state = State.DROP_XP;
                timer.reset();
                break;
            }
            case DROP_XP: {
                if (getXpLevel() >= 5) {
                    ChatUtility.send(Text.literal("Опыт набран " + getXpLevel() ).withColor(new Color(100, 255, 100).getRGB()));
                    state = State.FIND_ANVIL;
                    timer.reset();
                    break;
                }
                if (!timer.reached(200, false)) return;
                var inv = mc.player.getInventory();
                int bottleSlot = -1;
                for (int i = 0; i < 36; i++) {
                    if (inv.getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
                        bottleSlot = i;
                        break;
                    }
                }
                if (bottleSlot == -1) {
                    ChatUtility.send(Text.literal("Бутылки кончились, покупаю").withColor(new Color(255, 220, 100).getRGB()));
                    xpBuyAttempts = 0;
                    state = State.BUY_XP_SEARCH;
                    timer.reset();
                    return;
                }
                if (bottleSlot > 8) {
                    mc.interactionManager.clickSlot(
                            mc.player.playerScreenHandler.syncId,
                            bottleSlot, 0, SlotActionType.SWAP, mc.player
                    );
                    inv.setSelectedSlot(0);
                } else {
                    inv.setSelectedSlot(bottleSlot);
                }
                mc.player.setPitch(90f);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                timer.reset();
                break;
            }

            case WAIT_XP_PICKUP: {
                state = State.FIND_ANVIL;
                timer.reset();
                break;
            }
            case FIND_ANVIL: {
                BlockPos anvil = findAnvil();
                if (anvil == null) {
                    error("Наковальня не найдена в радиусе 3 блоков");
                    return;
                }
                state = State.OPEN_ANVIL;
                timer.reset();
                break;
            }

            case OPEN_ANVIL: {
                if (!timer.reached(200, false)) return;
                BlockPos anvil = findAnvil();
                if (anvil == null) {
                    error("Наковальня исчезла");
                    return;
                }
                Vec3d center = Vec3d.ofCenter(anvil);
                Vec3d diff = center.subtract(mc.player.getEyePos());
                double hDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
                float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
                float pitch = (float) Math.toDegrees(-Math.atan2(diff.y, hDist));
                mc.player.setYaw(yaw);
                mc.player.setPitch(pitch);
                BlockHitResult hitResult = new BlockHitResult(center, Direction.UP, anvil, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                state = State.WAIT_ANVIL_OPEN;
                timer.reset();
                break;
            }

            case WAIT_ANVIL_OPEN: {
                if (mc.currentScreen instanceof AnvilScreen) {
                    state = State.ANVIL_PLACE_ITEM1;
                    timer.reset();
                    break;
                }
                if (timer.reached(800, false)) {
                    BlockPos anvil = findAnvil();
                    if (anvil == null) {
                        error("Наковальня не найдена");
                        return;
                    }
                    Vec3d center = Vec3d.ofCenter(anvil);
                    Vec3d diff = center.subtract(mc.player.getEyePos());
                    double hDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
                    float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
                    float pitch = (float) Math.toDegrees(-Math.atan2(diff.y, hDist));
                    mc.player.setYaw(yaw);
                    mc.player.setPitch(pitch);
                    BlockHitResult hitResult = new BlockHitResult(center, Direction.UP, anvil, false);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
                    timer.reset();
                }
                break;
            }

            case ANVIL_PLACE_ITEM1: {
                if (!timer.reached(delay, false)) return;
                if (!(mc.currentScreen instanceof AnvilScreen screen)) {
                    state = State.OPEN_ANVIL;
                    timer.reset();
                    return;
                }
                AnvilScreenHandler handler = (AnvilScreenHandler) screen.getScreenHandler();
                int containerSlot1 = inventorySlotToAnvilContainer(slot1);
                if (containerSlot1 == -1) {
                    error("Неверный слот инвентаря: " + slot1);
                    return;
                }
                mc.interactionManager.clickSlot(handler.syncId, containerSlot1, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.PICKUP, mc.player);
                state = State.ANVIL_PLACE_ITEM2;
                timer.reset();
                break;
            }

            case ANVIL_PLACE_ITEM2: {
                if (!timer.reached(delay, false)) return;
                if (!(mc.currentScreen instanceof AnvilScreen screen)) {
                    state = State.OPEN_ANVIL;
                    timer.reset();
                    return;
                }
                AnvilScreenHandler handler = (AnvilScreenHandler) screen.getScreenHandler();
                int containerSlot2 = inventorySlotToAnvilContainer(slot2);
                if (containerSlot2 == -1) {
                    error("Неверный слот инвентаря: " + slot2);
                    return;
                }
                mc.interactionManager.clickSlot(handler.syncId, containerSlot2, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(handler.syncId, 1, 0, SlotActionType.PICKUP, mc.player);
                state = State.ANVIL_WAIT_RESULT;
                timer.reset();
                break;
            }

            case ANVIL_WAIT_RESULT: {
                if (!timer.reached(delay, false)) return;
                if (!(mc.currentScreen instanceof AnvilScreen screen)) {
                    state = State.OPEN_ANVIL;
                    timer.reset();
                    return;
                }
                AnvilScreenHandler handler = (AnvilScreenHandler) screen.getScreenHandler();
                ItemStack result = handler.getSlot(2).getStack();
                if (!result.isEmpty()) {
                    state = State.ANVIL_TAKE_RESULT;
                    timer.reset();
                    break;
                }
                if (timer.reached(5000, false)) {
                    ItemStack s0 = handler.getSlot(0).getStack();
                    ItemStack s1 = handler.getSlot(1).getStack();
                    if (!s0.isEmpty()) mc.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
                    if (!s1.isEmpty()) mc.interactionManager.clickSlot(handler.syncId, 1, 0, SlotActionType.QUICK_MOVE, mc.player);
                    ChatUtility.send(Text.literal(" Наковальня не дала результат, пробую снова").withColor(new Color(255, 100, 100).getRGB()));
                    state = State.ANVIL_CLOSE;
                    timer.reset();
                }
                break;
            }

            case ANVIL_TAKE_RESULT: {
                if (!timer.reached(150, false)) return;
                if (!(mc.currentScreen instanceof AnvilScreen screen)) {
                    state = State.OPEN_ANVIL;
                    timer.reset();
                    return;
                }
                AnvilScreenHandler handler = (AnvilScreenHandler) screen.getScreenHandler();
                mc.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, mc.player);
                ChatUtility.send(Text.literal("Соединил ботинки").withColor(new Color(100, 255, 100).getRGB()));
                state = State.ANVIL_CLOSE;
                timer.reset();
                break;
            }

            case ANVIL_CLOSE: {
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = State.ANVIL_WAIT_CLOSE;
                timer.reset();
                break;
            }

            case ANVIL_WAIT_CLOSE: {
                if (mc.currentScreen != null) {
                    if (timer.reached(500, false)) {
                        mc.currentScreen.close();
                        timer.reset();
                    }
                    return;
                }
                if (!timer.reached(150, false)) return;
                state = State.IDLE;
                timer.reset();
                break;
            }
            case SELL_SELECT_SLOT: {
                if (!timer.reached(delay, false)) return;
                if (resultBootSlot < 0 || resultBootSlot > 8) {
                    var inv = mc.player.getInventory();
                    int target = (int) targetLevel.get();
                    resultBootSlot = -1;
                    for (int i = 0; i <= 8; i++) {
                        ItemStack s = inv.getStack(i);
                        if (isDiamondBoots(s) && getProtectionLevel(s) >= target) {
                            resultBootSlot = i;
                            break;
                        }
                    }
                    if (resultBootSlot == -1) {
                        error("Готовые ботинки не найдены в хотбаре для продажи");
                        return;
                    }
                }
                mc.player.getInventory().setSelectedSlot(resultBootSlot);
                state = State.SELL_WAIT_SLOT;
                timer.reset();
                break;
            }

            case SELL_WAIT_SLOT: {
                if (!timer.reached(200, false)) return;
                state = State.SELL_SEND_CMD;
                timer.reset();
                break;
            }

            case SELL_SEND_CMD: {
                if (!timer.reached(delay, false)) return;
                int price = (int) sellPrice.get();
                if (Client.AUTOBUY_ITEMS != null && resultBootSlot >= 0) {
                    var inv = mc.player.getInventory();
                    net.minecraft.item.ItemStack soldItem = inv.getStack(resultBootSlot);
                    if (!soldItem.isEmpty()) {
                        Client.AUTOBUY_ITEMS.addSellHistoryItem(soldItem, 0, price);
                    }
                }
                NetworkUtility.sendCommand("ah sell " + price);
                ChatUtility.send(Text.literal("Продаю ботинки за " + price).withColor(new Color(100, 255, 100).getRGB()));
                state = State.SELL_WAIT_DONE;
                timer.reset();
                break;
            }

            case SELL_WAIT_DONE: {
                if (!timer.reached(delay * 2, false)) return;
                resultBootSlot = -1;
                state = State.IDLE;
                timer.reset();
                break;
            }

            case ERROR:
            case DONE:
                break;
        }
    };

}
