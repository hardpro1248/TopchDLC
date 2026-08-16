package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autosell;

import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import gg.topchdlc.api.autobuy.AutoBuyUtil;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;

import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

import static gg.topchdlc.MinecraftHolder.mc;

/**
 * Create by daun kvass
 */
public class AutoSellManager {
    private final InventoryHelper inventory = new InventoryHelper();
    private final SellPriceCalculator priceCalc = new SellPriceCalculator();

    private final Queue<SellTask> pendingCheckQueue = new LinkedList<>();
    private final Queue<SellTask> readyToSellQueue = new LinkedList<>();
    private final TimeUtility sellTimer = new TimeUtility();
    private final TimeUtility checkTimer = new TimeUtility();

    private SellState state = SellState.IDLE;
    private SellTask current = null;
    private int marketMinPerUnit = Integer.MAX_VALUE;
    private int foundSlot = -1;
    private int actualCount = 0;
    private int swapSourceSlot = -1;
    private boolean enabled = false;
    private AutoSellMode mode = AutoSellMode.берет_цену_с_ах;
    private float markupPercent = 10f;
    private long delay = 1500;


    public void setEnabled(boolean v) {
        this.enabled = v;
        if (!v) reset();
    }

    public void setMode(AutoSellMode mode) { this.mode = mode; }
    public void setMarkupPercent(float v) { this.markupPercent = v; }
    public void setDelay(long v) { this.delay = v; }


    public boolean isActive() {
        return state != SellState.IDLE || !pendingCheckQueue.isEmpty() || !readyToSellQueue.isEmpty();
    }

    public boolean isSelling() { return state != SellState.IDLE; }

    public String getStatus() {
        if (state == SellState.IDLE) {
            int p = pendingCheckQueue.size(), r = readyToSellQueue.size();
            return (p > 0 || r > 0) ? "Ожидание: " + p + " | Готово: " + r : "Не активен";
        }
        return current == null ? "Подготовка..." : "Продаю: " + current.displayName + " x" + actualCount;
    }

    public int getPendingCount() { return pendingCheckQueue.size(); }
    public int getReadyCount() { return readyToSellQueue.size(); }

    public void enqueueSell(ItemStack boughtItem, int totalBuyPrice, ItemBuy matchedItem) {
        if (!enabled) return;

        String cleanName = cleanName(matchedItem.getSearchName());
        int count = boughtItem.getCount();
        int pricePerUnit = count > 0 ? totalBuyPrice / count : totalBuyPrice;

        pendingCheckQueue.add(new SellTask(boughtItem, totalBuyPrice,
                matchedItem.getDisplayName(), cleanName, matchedItem));

        ChatUtility.send(Text.literal("AutoSell: Ожидаю → " + matchedItem.getDisplayName()
                        + " x" + count + " (куплено по " + fmt(pricePerUnit) + "$/шт)")
                .withColor(new Color(255, 220, 100).getRGB()));
    }

    public void reset() {
        state = SellState.IDLE;
        pendingCheckQueue.clear();
        readyToSellQueue.clear();
        current = null;
        foundSlot = -1;
        actualCount = 0;
        swapSourceSlot = -1;
        marketMinPerUnit = Integer.MAX_VALUE;
    }

    public void onTick() {
        if (!enabled || mc.player == null) return;

        checkPending();

        if (state != SellState.IDLE) {
            tick();
            return;
        }

        if (!readyToSellQueue.isEmpty()) {
            current = readyToSellQueue.poll();
            startSell();
        }
    }


    private void checkPending() {
        if (pendingCheckQueue.isEmpty() || !checkTimer.reached(500, true)) return;

        for (SellTask task : pendingCheckQueue) {
            task.checkAttempts++;

            var hotbar = inventory.findInHotbar(task);
            if (hotbar != null) {
                readyToSellQueue.add(task);
                pendingCheckQueue.remove(task);
                ChatUtility.send(Text.literal("AutoSell: ✓ Хотбар[" + hotbar.slotIndex() + "] → "
                        + task.displayName + " x" + hotbar.count()).withColor(new Color(100, 255, 100).getRGB()));
                return;
            }

            var inv = inventory.findInInventory(task);
            if (inv != null) {
                readyToSellQueue.add(task);
                pendingCheckQueue.remove(task);
                ChatUtility.send(Text.literal("AutoSell: ✓ Инв[" + inv.slotIndex() + "] → "
                        + task.displayName + " x" + inv.count()).withColor(new Color(100, 255, 100).getRGB()));
                return;
            }

            if (task.checkAttempts >= SellTask.MAX_CHECK_ATTEMPTS) {
                pendingCheckQueue.remove(task);
                ChatUtility.send(Text.literal("AutoSell: ✗ " + task.displayName + " не найден")
                        .withColor(new Color(255, 100, 100).getRGB()));
                return;
            }
        }
    }


    private void startSell() {
        sellTimer.reset();
        marketMinPerUnit = Integer.MAX_VALUE;
        swapSourceSlot = -1;

        var result = inventory.find(current);
        if (result == null) {
            ChatUtility.send(Text.literal("AutoSell: ✗ Предмет исчез: " + current.displayName)
                    .withColor(new Color(255, 100, 100).getRGB()));
            finish();
            return;
        }

        foundSlot = result.slotIndex();
        actualCount = result.count();

        if (foundSlot > 8) {
            swapSourceSlot = foundSlot;
            state = SellState.SWAP_TO_HOTBAR;
        } else {
            mc.player.getInventory().setSelectedSlot(foundSlot);
            state = SellState.CLOSING_SCREEN;
        }
    }


    private void tick() {
        switch (state) {
            case SWAP_TO_HOTBAR -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = SellState.WAIT_SWAP;
                sellTimer.reset();
            }
            case WAIT_SWAP -> {
                if (mc.currentScreen != null) { mc.currentScreen.close(); sellTimer.reset(); break; }
                if (!sellTimer.reached(400, false)) break;
                int hotbarTarget = mc.player.getInventory().getSelectedSlot();
                mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                        swapSourceSlot, hotbarTarget, SlotActionType.SWAP, mc.player);
                foundSlot = hotbarTarget;
                state = SellState.CLOSING_SCREEN;
                sellTimer.reset();
            }
            case CLOSING_SCREEN -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = SellState.WAIT_SCREEN_CLOSED;
                sellTimer.reset();
            }
            case WAIT_SCREEN_CLOSED -> {
                if (mc.currentScreen != null) { mc.currentScreen.close(); break; }
                if (!sellTimer.reached(2000, false)) break;
                if (foundSlot >= 0 && foundSlot <= 8) mc.player.getInventory().setSelectedSlot(foundSlot);
                state = (mode == AutoSellMode.берет_цену_с_ах) ? SellState.MARKET_SEND_SEARCH : SellState.SELL_PREPARE;
                sellTimer.reset();
            }
            case MARKET_SEND_SEARCH -> {
                if (!sellTimer.reached(delay, false)) break;
                NetworkUtility.sendCommand("ah search " + current.searchName);
                ChatUtility.send(Text.literal("AutoSell: /ah search " + current.searchName)
                        .withColor(new Color(150, 150, 255).getRGB()));
                state = SellState.MARKET_WAIT_AH_OPEN;
                sellTimer.reset();
            }
            case MARKET_WAIT_AH_OPEN -> {
                if (isAhOpen()) { state = SellState.MARKET_WAIT_SEARCH_RESULTS; sellTimer.reset(); break; }
                if (sellTimer.reached(5000, false)) {
                    ChatUtility.send(Text.literal("AutoSell: Таймаут → цена покупки")
                            .withColor(new Color(255, 200, 100).getRGB()));
                    state = SellState.MARKET_CLOSE_AH;
                    sellTimer.reset();
                }
            }
            case MARKET_WAIT_SEARCH_RESULTS -> {
                if (sellTimer.reached(1500, false)) { state = SellState.MARKET_ANALYZE; sellTimer.reset(); }
            }
            case MARKET_ANALYZE -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    marketMinPerUnit = Integer.MAX_VALUE;
                    for (var slot : screen.getScreenHandler().slots) {
                        ItemStack stack = slot.getStack();
                        if (stack.isEmpty()) continue;
                        long total = AutoBuyUtil.getPrice(stack);
                        if (total == Long.MAX_VALUE) continue;
                        int cnt = stack.getCount();
                        long ppu = cnt > 0 ? total / cnt : total;
                        if (ppu < marketMinPerUnit) marketMinPerUnit = (int) Math.min(ppu, Integer.MAX_VALUE);
                    }
                    if (marketMinPerUnit != Integer.MAX_VALUE) {
                        ChatUtility.send(Text.literal("AutoSell: Мин. цена: " + fmt(marketMinPerUnit) + "$/шт")
                                .withColor(new Color(100, 255, 100).getRGB()));
                    } else {
                        ChatUtility.send(Text.literal("AutoSell: Лоты не найдены → цена покупки")
                                .withColor(new Color(255, 200, 100).getRGB()));
                    }
                }
                state = SellState.MARKET_CLOSE_AH;
                sellTimer.reset();
            }
            case MARKET_CLOSE_AH -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = SellState.MARKET_WAIT_CLOSED;
                sellTimer.reset();
            }
            case MARKET_WAIT_CLOSED -> {
                if (mc.currentScreen != null) { mc.currentScreen.close(); break; }
                if (sellTimer.reached(2000, false)) { state = SellState.SELL_PREPARE; sellTimer.reset(); }
            }
            case SELL_PREPARE -> {
                var result = inventory.find(current);
                if (result == null) {
                    ChatUtility.send(Text.literal("AutoSell: ✗ Предмет исчез!")
                            .withColor(new Color(255, 100, 100).getRGB()));
                    finish();
                    return;
                }
                foundSlot = result.slotIndex();
                actualCount = result.count();
                if (foundSlot > 8) {
                    int ht = mc.player.getInventory().getSelectedSlot();
                    mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                            foundSlot, ht, SlotActionType.SWAP, mc.player);
                    foundSlot = ht;
                }
                state = SellState.SELL_SELECT_SLOT;
                sellTimer.reset();
            }
            case SELL_SELECT_SLOT -> {
                mc.player.getInventory().setSelectedSlot(foundSlot);
                state = SellState.SELL_WAIT_SLOT;
                sellTimer.reset();
            }
            case SELL_WAIT_SLOT -> {
                if (sellTimer.reached(300, false)) { state = SellState.SELL_SEND_COMMAND; sellTimer.reset(); }
            }
            case SELL_SEND_COMMAND -> {
                if (!sellTimer.reached(delay, false)) break;
                ItemStack hand = mc.player.getMainHandStack();
                if (hand.isEmpty()) { finish(); return; }
                actualCount = hand.getCount() > 1 ? hand.getCount() : current.originalCount;
                int totalPrice = priceCalc.calculate(current, mode, marketMinPerUnit, markupPercent, actualCount);
                NetworkUtility.sendCommand("ah sell " + totalPrice);
                ChatUtility.send(Text.literal("AutoSell: ✓ " + current.displayName
                                + " x" + actualCount + " → " + fmt(totalPrice) + "$")
                        .withColor(new Color(50, 255, 50).getRGB()));
                state = SellState.SELL_WAIT_DONE;
                sellTimer.reset();
            }
            case SELL_WAIT_DONE -> {
                if (sellTimer.reached(delay, false)) finish();
            }
            case SELL_REOPEN_AH -> {
                if (!sellTimer.reached(delay, false)) break;
                NetworkUtility.sendCommand("ah");
                state = SellState.SELL_WAIT_AH_REOPEN;
                sellTimer.reset();
            }
            case SELL_WAIT_AH_REOPEN -> {
                if (isAhOpen()) { state = SellState.DONE; sellTimer.reset(); break; }
                if (sellTimer.reached(5000, false)) {
                    NetworkUtility.sendCommand("ah");
                    sellTimer.reset();
                }
            }
            case DONE -> {
                current = null;
                marketMinPerUnit = Integer.MAX_VALUE;
                foundSlot = -1;
                actualCount = 0;
                state = SellState.IDLE;
            }
        }
    }

    private void finish() {
        current = null;
        foundSlot = -1;
        actualCount = 0;
        if (!readyToSellQueue.isEmpty()) {
            current = readyToSellQueue.poll();
            startSell();
        } else {
            state = SellState.SELL_REOPEN_AH;
            sellTimer.reset();
        }
    }

    private boolean isAhOpen() {
        if (!(mc.currentScreen instanceof GenericContainerScreen s)) return false;
        String t = s.getTitle().getString().toLowerCase();
        return t.contains("аукцион") || t.contains("auction");
    }

    private String cleanName(String name) {
        return name.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("[^a-zA-Zа-яА-ЯёЁ ]", "")
                .replaceAll("\\s+", " ").trim();
    }

    private String fmt(int price) {
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }
}
