package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autoparser;

import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import gg.topchdlc.api.autobuy.AutoBuyUtil;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static gg.topchdlc.MinecraftHolder.mc;

public class AutoParserManager {
    private final TimeUtility parserTimer = new TimeUtility();
    private final TimeUtility intervalTimer = new TimeUtility();

    private boolean running = false;
    private ParserState state = ParserState.IDLE;
    private int currentIndex = 0;
    private List<ItemBuy> items = new ArrayList<>();
    private final Set<String> autoParserItemKeys = new HashSet<>();

    private float parserPercent = 95f;
    private boolean autoEnabled = false;
    private long autoIntervalMs = 5 * 60_000L;
    private boolean funtimeMode = false;

    private BiConsumer<String, Integer> onPriceFound;
    private Function<ItemBuy, String> keyProvider;
    private Supplier<List<ItemBuy>> autoItemsSupplier;

    public void configure(float parserPercent, boolean autoEnabled, long autoIntervalMs,
                          boolean funtimeMode,
                          BiConsumer<String, Integer> onPriceFound,
                          Function<ItemBuy, String> keyProvider,
                          Supplier<List<ItemBuy>> autoItemsSupplier) {
        this.parserPercent = parserPercent;
        this.autoEnabled = autoEnabled;
        this.autoIntervalMs = autoIntervalMs;
        this.funtimeMode = funtimeMode;
        this.onPriceFound = onPriceFound;
        this.keyProvider = keyProvider;
        this.autoItemsSupplier = autoItemsSupplier;
    }

    public void start(List<ItemBuy> targets) {
        if (running || targets.isEmpty()) return;
        items = new ArrayList<>(targets);
        running = true;
        state = ParserState.IDLE;
        currentIndex = 0;
        intervalTimer.reset();
        ChatUtility.send(Text.literal("AutoParser: Анализ запущен (" + items.size() + " предм.)").withColor(new Color(100, 255, 100).getRGB()));
    }

    public void stop() {
        running = false;
        state = ParserState.IDLE;
        items.clear();
        ChatUtility.send(Text.literal("AutoParser: Остановлен").withColor(new Color(255, 80, 80).getRGB()));
    }

    public void onTick() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (running) { tick(); return; }
        if (autoEnabled && intervalTimer.reached(autoIntervalMs, true)) {
            List<ItemBuy> targets = autoItemsSupplier.get();
            if (!targets.isEmpty()) start(targets);
        }
    }

    private void tick() {
        switch (state) {
            case IDLE -> {
                if (currentIndex >= items.size()) {
                    stop();
                    return;
                }
                if (mc.currentScreen != null) mc.setScreen(null);
                state = ParserState.OPENING_AH;
                parserTimer.reset();
            }

            case OPENING_AH -> {
                if (parserTimer.reached(600, false)) {
                    NetworkUtility.sendCommand("ah");
                    state = ParserState.WAIT_AH_OPEN;
                    parserTimer.reset();
                }
            }

            case WAIT_AH_OPEN -> {
                if (isAhOpen()) {
                    state = ParserState.SEARCHING;
                    parserTimer.reset();
                } else if (parserTimer.reached(3000, false)) {
                    stop();
                }
            }

            case SEARCHING -> {
                if (parserTimer.reached(600, false)) {
                    if (mc.currentScreen != null) mc.setScreen(null);

                    ItemBuy item = items.get(currentIndex);
                    String query = item.getSearchName();

                    ChatUtility.send(Text.literal("AutoParser: [" + (currentIndex + 1) + "/" + items.size() + "] Поиск: " + query)
                            .withColor(new Color(120, 180, 255).getRGB()));

                    NetworkUtility.sendCommand("ah search " + query);
                    state = ParserState.WAIT_SEARCH_OPEN;
                    parserTimer.reset();
                }
            }

            case WAIT_SEARCH_OPEN -> {
                if (isAhOpen() && hasItemsLoaded()) {
                    state = ParserState.WAIT_ANALYZE_DELAY;
                    parserTimer.reset();
                } else if (parserTimer.reached(4000, false)) {
                    state = ParserState.NEXT_ITEM;
                }
            }

            case WAIT_ANALYZE_DELAY -> {
                if (parserTimer.reached(funtimeMode ? 1000 : 500, false)) {
                    state = ParserState.ANALYZING;
                }
            }

            case ANALYZING -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    ItemBuy target = items.get(currentIndex);
                    int minPrice = findMinPrice(screen, target);

                    if (minPrice != Integer.MAX_VALUE) {
                        int finalPrice = (int) (minPrice * (parserPercent / 100f));
                        onPriceFound.accept(keyProvider.apply(target), finalPrice);
                        ChatUtility.send(Text.literal("AutoParser: " + target.getDisplayName() + " | " + minPrice + "$ -> (Установлено: " + finalPrice + "$)")
                                .withColor(new Color(100, 255, 100).getRGB()));
                    } else {
                        ChatUtility.send(Text.literal("AutoParser: " + target.getDisplayName() + " - Не найден").withColor(new Color(255, 180, 80).getRGB()));
                    }
                    state = ParserState.CLOSING_AH;
                } else {
                    state = ParserState.NEXT_ITEM;
                }
            }

            case CLOSING_AH -> {
                if (mc.currentScreen != null) mc.setScreen(null);
                state = ParserState.NEXT_ITEM;
                parserTimer.reset();
            }

            case NEXT_ITEM -> {
                currentIndex++;
                state = ParserState.IDLE;
                parserTimer.reset();
            }
        }
    }
    private int findMinPrice(GenericContainerScreen screen, ItemBuy target) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < 45; i++) {
            ItemStack stack = screen.getScreenHandler().getSlot(i).getStack();
            if (stack.isEmpty()) continue;

            if (target.isBuy(stack)) {
                long total = AutoBuyUtil.getPrice(stack);
                if (total <= 0 || total == Long.MAX_VALUE) continue;

                int count = stack.getCount();
                int pricePerOne = (int) (total / count);

                if (pricePerOne < min) min = pricePerOne;
            }
        }
        return min;
    }

    private boolean isAhOpen() {
        if (!(mc.currentScreen instanceof GenericContainerScreen s)) return false;
        return AutoBuyUtil.isAuction(s.getScreenHandler());
    }

    private boolean hasItemsLoaded() {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return false;
        for (int i = 0; i < 45; i++) {
            ItemStack s = screen.getScreenHandler().getSlot(i).getStack();
            if (s.isEmpty()) continue;
            String name = s.getName().getString().toLowerCase();
            if (!name.contains("панель") && !name.contains("стекло")) return true;
        }
        return false;
    }

    public boolean isRunning() { return running; }
    public String getStatus() { return running ? "Парсинг " + (currentIndex + 1) + "/" + items.size() : "Спит"; }
    public long getTimeLeft() { return Math.max(0, autoIntervalMs - intervalTimer.getTime()); }

}