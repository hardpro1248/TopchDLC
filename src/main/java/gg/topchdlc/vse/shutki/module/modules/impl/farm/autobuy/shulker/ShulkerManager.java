package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.shulker;

import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;

import java.awt.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import static gg.topchdlc.MinecraftHolder.mc;

/**
 * Create by daun kvass
 */
public class ShulkerManager {
    private static final int MAX_FAILS = 5;
    private static final long FULL_COOLDOWN = 5 * 60_000L;

    private final TimeUtility timer = new TimeUtility();
    private final TimeUtility cooldownTimer = new TimeUtility();

    private ShulkerFillState state = ShulkerFillState.IDLE;
    private int shulkerSlot = -1;
    private boolean allFull = false;
    private int failCount = 0;
    private final Set<Integer> fullSlots = new HashSet<>();
    private final Queue<Integer> dumpQueue = new LinkedList<>();
    private boolean dumpWaiting = false;
    private int dumpLastCount = 0;
    private Runnable onDone;

    public void configure(Runnable onDone) {
        this.onDone = onDone;
    }


    public boolean isActive() { return state != ShulkerFillState.IDLE; }
    public boolean isAllFull() { return allFull; }
    public boolean isCooldownActive() { return allFull && !cooldownTimer.reached(FULL_COOLDOWN, false); }

    public boolean checkAndStart() {
        if (!isInventoryAlmostFull()) return false;
        if (isCooldownActive()) return false;
        allFull = false;
        int slot = findShulker(-1);
        if (slot == -1) return false;
        shulkerSlot = slot;
        state = ShulkerFillState.CLOSE_AH;
        timer.reset();
        return true;
    }

    public void reset() {
        state = ShulkerFillState.IDLE;
        shulkerSlot = -1;
        allFull = false;
        failCount = 0;
        fullSlots.clear();
        dumpQueue.clear();
        dumpWaiting = false;
    }


    public void onTick() {
        if (state == ShulkerFillState.IDLE || mc.player == null) return;
        tick();
    }

    private void tick() {
        switch (state) {
            case CLOSE_AH -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = ShulkerFillState.WAIT_CLOSE;
                timer.reset();
            }
            case WAIT_CLOSE -> {
                if (mc.currentScreen == null || timer.reached(2000, false)) {
                    state = ShulkerFillState.OPEN_SHULKER;
                    timer.reset();
                }
            }
            case OPEN_SHULKER -> {
                if (!timer.reached(300, false)) break;
                if (shulkerSlot == -1) shulkerSlot = findShulker(-1);
                if (shulkerSlot == -1) { state = ShulkerFillState.REOPEN_AH; timer.reset(); break; }
                ItemStack cursor = mc.player.playerScreenHandler.getCursorStack();
                if (!cursor.isEmpty()) {
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, -999, 0, SlotActionType.PICKUP, mc.player);
                }
                mc.execute(() -> mc.setScreen(new InventoryScreen(mc.player)));
                state = ShulkerFillState.WAIT_SHULKER_OPEN;
                timer.reset();
            }
            case WAIT_SHULKER_OPEN -> {
                if (mc.currentScreen instanceof ShulkerBoxScreen || mc.currentScreen instanceof GenericContainerScreen) {
                    state = ShulkerFillState.SCAN_ITEMS;
                    break;
                }
                if (mc.currentScreen instanceof InventoryScreen && timer.reached(300, true)) {
                    ItemStack cursor = mc.player.playerScreenHandler.getCursorStack();
                    if (!cursor.isEmpty()) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, -999, 0, SlotActionType.PICKUP, mc.player);
                        break;
                    }
                    if (shulkerSlot != -1) {
                        ItemStack s = mc.player.getInventory().getStack(shulkerSlot);
                        if (s.isEmpty() || !isShulker(s.getItem())) shulkerSlot = -1;
                    }
                    if (shulkerSlot == -1) shulkerSlot = findShulker(-1);
                    if (shulkerSlot == -1) { state = ShulkerFillState.REOPEN_AH; timer.reset(); break; }
                    int screenSlot = shulkerSlot < 9 ? 36 + shulkerSlot : shulkerSlot;
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, screenSlot, 1, SlotActionType.PICKUP, mc.player);
                } else if (mc.currentScreen == null && timer.reached(500, false)) {
                    mc.execute(() -> mc.setScreen(new InventoryScreen(mc.player)));
                }
                if (timer.reached(15000, false)) {
                    ChatUtility.send(Text.literal("Таймаут открытия шалкера").withColor(new Color(255, 100, 100).getRGB()));
                    if (mc.currentScreen != null) mc.currentScreen.close();
                    state = ShulkerFillState.REOPEN_AH;
                    timer.reset();
                }
            }
            case SCAN_ITEMS -> {
                ScreenHandler handler = getHandler();
                if (handler != null) {
                    dumpQueue.clear();
                    dumpWaiting = false;
                    int containerSize = handler.slots.size() - 36;
                    if (containerSize < 0) containerSize = 0;
                    for (Slot slot : handler.slots) {
                        if (slot.id < containerSize) continue;
                        ItemStack stack = slot.getStack();
                        if (!stack.isEmpty() && !isShulker(stack.getItem())) dumpQueue.add(slot.id);
                    }
                    state = dumpQueue.isEmpty() ? ShulkerFillState.CLOSE_SHULKER : ShulkerFillState.DUMP_ITEMS;
                    timer.reset();
                } else {
                    state = ShulkerFillState.REOPEN_AH;
                    timer.reset();
                }
            }
            case DUMP_ITEMS -> {
                ScreenHandler handler = getHandler();
                if (handler == null) { state = ShulkerFillState.REOPEN_AH; timer.reset(); break; }
                if (dumpQueue.isEmpty()) { state = ShulkerFillState.CLOSE_SHULKER; timer.reset(); dumpWaiting = false; break; }

                int slotId = dumpQueue.peek();
                if (slotId >= handler.slots.size()) { dumpQueue.poll(); dumpWaiting = false; break; }
                ItemStack slotStack = handler.getSlot(slotId).getStack();

                if (dumpWaiting) {
                    if (slotStack.isEmpty()) {
                        dumpQueue.poll();
                    } else if (slotStack.getCount() == dumpLastCount) {
                        fullSlots.add(shulkerSlot);
                        failCount++;
                        if (failCount >= MAX_FAILS) {
                            ChatUtility.send(Text.literal("Все шалкеры забиты").withColor(new Color(255, 100, 100).getRGB()));
                            allFull = true;
                            cooldownTimer.reset();
                            if (mc.currentScreen != null) mc.currentScreen.close();
                            state = ShulkerFillState.REOPEN_AH;
                            timer.reset();
                        } else {
                            state = ShulkerFillState.CLOSE_SHULKER_FIND_NEXT;
                            timer.reset();
                        }
                    } else {
                        dumpQueue.poll();
                    }
                    dumpWaiting = false;
                } else {
                    if (!timer.reached(200, true)) break;
                    if (slotStack.isEmpty()) { dumpQueue.poll(); break; }
                    dumpLastCount = slotStack.getCount();
                    mc.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
                    dumpWaiting = true;
                }
            }
            case CLOSE_SHULKER -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = ShulkerFillState.WAIT_SHULKER_CLOSE;
                timer.reset();
            }
            case CLOSE_SHULKER_FIND_NEXT -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = ShulkerFillState.WAIT_NEXT_SHULKER_CLOSE;
                timer.reset();
            }
            case WAIT_NEXT_SHULKER_CLOSE -> {
                if (mc.currentScreen == null || timer.reached(1000, false)) {
                    int next = findShulker(shulkerSlot);
                    if (next != -1) {
                        shulkerSlot = next;
                        state = ShulkerFillState.OPEN_SHULKER;
                        timer.reset();
                    } else {
                        ChatUtility.send(Text.literal("Все шалкеры забиты").withColor(new Color(255, 100, 100).getRGB()));
                        allFull = true;
                        cooldownTimer.reset();
                        state = ShulkerFillState.REOPEN_AH;
                        timer.reset();
                    }
                }
            }
            case WAIT_SHULKER_CLOSE -> {
                if (mc.currentScreen == null || timer.reached(1000, false)) {
                    state = ShulkerFillState.REOPEN_AH;
                    timer.reset();
                }
            }
            case REOPEN_AH -> {
                if (timer.reached(500, false)) {
                    NetworkUtility.sendCommand("ah");
                    state = ShulkerFillState.WAIT_AH_REOPEN;
                    timer.reset();
                }
            }
            case WAIT_AH_REOPEN -> {
                if (isAhOpen()) {
                    state = ShulkerFillState.IDLE;
                    shulkerSlot = -1;
                    if (onDone != null) onDone.run();
                    break;
                }
                if (timer.reached(5000, false)) {
                    NetworkUtility.sendCommand("ah");
                    timer.reset();
                }
            }
        }
    }
    private boolean isInventoryAlmostFull() {
        if (mc.player == null) return false;
        PlayerInventory inv = mc.player.getInventory();
        int free = 0;
        for (int i = 0; i < 36; i++) if (inv.getStack(i).isEmpty()) free++;
        return free <= 5;
    }

    private int findShulker(int exclude) {
        if (mc.player == null) return -1;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 9; i < 36; i++) {
            if (i == exclude || fullSlots.contains(i)) continue;
            if (isShulker(inv.getStack(i).getItem())) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (i == exclude || fullSlots.contains(i)) continue;
            if (isShulker(inv.getStack(i).getItem())) return i;
        }
        return -1;
    }

    private boolean isShulker(Item item) {
        return item == Items.SHULKER_BOX
                || item == Items.WHITE_SHULKER_BOX || item == Items.ORANGE_SHULKER_BOX
                || item == Items.MAGENTA_SHULKER_BOX || item == Items.LIGHT_BLUE_SHULKER_BOX
                || item == Items.YELLOW_SHULKER_BOX || item == Items.LIME_SHULKER_BOX
                || item == Items.PINK_SHULKER_BOX || item == Items.GRAY_SHULKER_BOX
                || item == Items.LIGHT_GRAY_SHULKER_BOX || item == Items.CYAN_SHULKER_BOX
                || item == Items.PURPLE_SHULKER_BOX || item == Items.BLUE_SHULKER_BOX
                || item == Items.BROWN_SHULKER_BOX || item == Items.GREEN_SHULKER_BOX
                || item == Items.RED_SHULKER_BOX || item == Items.BLACK_SHULKER_BOX;
    }

    private ScreenHandler getHandler() {
        if (mc.currentScreen instanceof ShulkerBoxScreen s) return s.getScreenHandler();
        if (mc.currentScreen instanceof GenericContainerScreen s) return s.getScreenHandler();
        return null;
    }

    private boolean isAhOpen() {
        if (!(mc.currentScreen instanceof GenericContainerScreen s)) return false;
        String t = s.getTitle().getString().toLowerCase();
        return t.contains("аукцион") || t.contains("auction");
    }
}
