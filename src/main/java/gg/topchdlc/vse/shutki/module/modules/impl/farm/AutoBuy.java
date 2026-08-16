package gg.topchdlc.vse.shutki.module.modules.impl.farm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.module.settings.impl.button.ButtonSetting;
import gg.topchdlc.Client;
import gg.topchdlc.api.autobuy.AutoBuyManager;
import gg.topchdlc.api.autobuy.AutoBuyUtil;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventAddMessage;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.PendingPurchase;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autoparser.AutoParserManager;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autosell.AutoSellManager;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autosell.AutoSellMode;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.relist.RelistManager;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.shulker.ShulkerManager;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Create by daun kvass
 */
public class AutoBuy extends Module {
    public static final AutoBuy INSTANCE = new AutoBuy();
    private final AutoSellManager   sellManager   = new AutoSellManager();
    private final AutoParserManager parserManager = new AutoParserManager();
    private final ShulkerManager    shulkerManager = new ShulkerManager();
    private final RelistManager     relistManager  = new RelistManager();
   // private final TelegramManager   tgManager      = new TelegramManager();
    private final AutoBuyManager manager = new AutoBuyManager();
    private final Map<String, Boolean> itemEnabled       = new HashMap<>();
    private final Map<String, Integer> itemMaxPrice      = new HashMap<>();
    private final Map<String, Integer> itemMinDurability = new HashMap<>();
    private final Map<String, Boolean> itemAutoSellEnabled  = new HashMap<>();
    private final Map<String, Boolean> itemSellModeCustom   = new HashMap<>();
    private final Map<String, Integer> itemCustomSellPrice  = new HashMap<>();
    private final List<String>         ignoredPlayers       = new ArrayList<>();
    private final Map<String, Boolean> itemAutoParserEnabled = new HashMap<>();
    private final Set<Integer>         clickedSlots         = new HashSet<>();
    private PendingPurchase pendingPurchase = null;
    private boolean autoBuyPaused = false;
    private final TimeUtility buyTimer     = new TimeUtility();
    private final TimeUtility cleanupTimer = new TimeUtility();
    private final TimeUtility defaultTimer = new TimeUtility();
    private final TimeUtility mode1Timer   = new TimeUtility();
    private long currentUpdateDelay = 500;
    private long currentBuyDelay    = 100;
    private Mode1State mode1State   = Mode1State.UPDATING;
    private int  mode1UpdateCount   = 0;
    private int  trickUpdateCount   = 0;
    private int  trickUpdateTarget  = 0;
    private boolean isTrickPaused   = false;
    private final TimeUtility trickPauseTimer = new TimeUtility();
    private long trickPauseDuration = 0;
    private final EnumSetting<UpdateMode> updateMode = enumSetting("Режим", UpdateMode.DEFAULT);
    private final SliderSetting updateDelay = sliderSetting("Задержка обновления", 300f, 100f, 2000f).increment(50f);
    private final SliderSetting buyDelay = sliderSetting("Задержка покупки", 1f, 1f, 500f).increment(10f);
    private final SliderSetting updateCount = sliderSetting("Обновлений за цикл", 9f, 8f, 30f).increment(1f).visible(() -> updateMode.is(UpdateMode.FunTime));
    private final SliderSetting pauseBetweenCycles = sliderSetting("Пауза между циклами", 750f, 500f, 10000f).increment(100f).visible(() -> updateMode.is(UpdateMode.FunTime));
    private final SliderSetting parserPercent = sliderSetting("Процент от мин. цены", 95f, 1f, 100f).increment(1f);
    private final CheckBox autoParserEnabled = checkbox("Авто-парсер по таймеру", false);
    private final SliderSetting autoParserInterval = sliderSetting("Интервал парсера (мин)", 5f, 1f, 60f).increment(1f).visible(() -> autoParserEnabled.get());
    private final KeybindSetting menuBind = keybindSetting("Открыть меню предметов", -1);
    private final CheckBox autoSellEnabled = checkbox("AutoSell", false);
    private final EnumSetting<AutoSellMode> autoSellMode = enumSetting("AutoSell режим", AutoSellMode.берет_цену_с_ах).visible(() -> autoSellEnabled.get());
    private final SliderSetting autoSellMarkup = sliderSetting("Наценка %", 10f, 0f, 50f).increment(1f).visible(() -> autoSellEnabled.get());
    private final SliderSetting autoSellDelay = sliderSetting("Задержка продажи", 1500f, 500f, 5000f).increment(100f).visible(() -> autoSellEnabled.get());


    private AutoBuy() {
        super("Auto Buy", Category.Misc, "Автоматически покупает выбранные предметы");
        initItemSettings();
        shulkerManager.configure(this::onShulkerDone);
        relistManager.configure(this::onRelistDone);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        AutoBuyUtil.clearCache();
        currentUpdateDelay = (long) updateDelay.get();
        currentBuyDelay    = (long) buyDelay.get();
        mode1State         = Mode1State.UPDATING;
        mode1UpdateCount   = 0;
        trickUpdateCount   = 0;
        trickUpdateTarget  = 7 + new Random().nextInt(4);
        isTrickPaused      = false;
        pendingPurchase    = null;
        clickedSlots.clear();
        defaultTimer.reset();
        mode1Timer.reset();

        sellManager.setEnabled(autoSellEnabled.get());
        sellManager.setMode(autoSellMode.get());
        sellManager.setMarkupPercent(autoSellMarkup.get());
        sellManager.setDelay((long) autoSellDelay.get());
        sellManager.reset();

        shulkerManager.reset();
        relistManager.reset();

        parserManager.configure(
                parserPercent.get(),
                autoParserEnabled.get(),
                (long)(autoParserInterval.get() * 60_000L),
                updateMode.is(UpdateMode.FunTime),
                (key, price) -> itemMaxPrice.put(key, price),
                this::getItemKey,
                this::getAutoParserItems
        );

      //  tgManager.start(this::handleTgCommand);
    }

    @Override
    public void onDisable() {
        super.onDisable();
  //      tgManager.stop();
        AutoBuyUtil.clearCache();
        clickedSlots.clear();
        pendingPurchase = null;
        if (parserManager.isRunning()) parserManager.stop();
        mode1State     = Mode1State.UPDATING;
        mode1UpdateCount = 0;
        isTrickPaused  = false;
        sellManager.reset();
        shulkerManager.reset();
        relistManager.reset();
    }

    EventBus<Event> events = event -> {
        if (mc.player == null || mc.world == null) return;

        if (event instanceof EventGameTick) {
            if (cleanupTimer.reached(5 * 60_000, true)) performMemoryCleanup();
            if (pendingPurchase != null && pendingPurchase.isExpired()) pendingPurchase = null;
            sellManager.setEnabled(autoSellEnabled.get());
            sellManager.setMode(autoSellMode.get());
            sellManager.setMarkupPercent(autoSellMarkup.get());
            sellManager.setDelay((long) autoSellDelay.get());

            parserManager.configure(
                    parserPercent.get(),
                    autoParserEnabled.get(),
                    (long)(autoParserInterval.get() * 60_000L),
                    updateMode.is(UpdateMode.FunTime),
                    (key, price) -> itemMaxPrice.put(key, price),
                    this::getItemKey,
                    this::getAutoParserItems
            );
            if (parserManager.isRunning()) {
                parserManager.onTick();
                return;
            }
            parserManager.onTick();
            sellManager.onTick();
            if (sellManager.isSelling()) return;
            if (autoBuyPaused) return;
            shulkerManager.onTick();
            if (shulkerManager.isActive()) return;
            relistManager.onTick();
            if (relistManager.isActive()) return;
            relistManager.tryStart();
            if (!shulkerManager.isCooldownActive()) {
                if (shulkerManager.checkAndStart()) return;
            }
            switch (updateMode.get()) {
                case DEFAULT -> handleDefault();
                case FunTime -> handleFunTime();
            }
        }

        if (event instanceof EventKey e) {
            if (menuBind.getBind() != -1 && e.getKey() == menuBind.getBind()) {
                mc.execute(this::openItemsMenu);
            }
        }

        if (event instanceof EventAddMessage e && pendingPurchase != null) {
            String msg = e.text.getString().replaceAll("§[0-9a-fk-orA-FK-OR]", "").toLowerCase();
            boolean confirmed = msg.contains("вы успешно купили") || msg.contains("you purchased")
                    || msg.contains("you bought") || msg.contains("куплен")
                    || msg.contains("покупка") || msg.contains("вы купили");
            if (confirmed) {
                if (Client.AUTOBUY_ITEMS != null)
                    Client.AUTOBUY_ITEMS.addPurchaseHistoryItem(pendingPurchase.stack, pendingPurchase.price);
           //     tgManager.notifyBought(pendingPurchase.stack, pendingPurchase.price);
                if (autoSellEnabled.get() && pendingPurchase.itemBuy != null
                        && isItemAutoSellEnabled(pendingPurchase.itemBuy)) {
                    sellManager.enqueueSell(pendingPurchase.stack, pendingPurchase.price, pendingPurchase.itemBuy);
                }
                pendingPurchase = null;
            }
        }
    };
    private void handleDefault() {
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
        if (!isAhScreen(screen)) return;

        if (defaultTimer.reached(currentUpdateDelay, true)) {
            updateAuction(screen);
            onAfterUpdate();
            currentUpdateDelay = (long) updateDelay.get();
            buyTimer.reset();
        } else if (buyTimer.reached(currentBuyDelay, true)) {
            boolean clicked = buyItemsOnce(screen);
            currentBuyDelay = (long) buyDelay.get();
            if (clicked) defaultTimer.reset();
        }
    }

    private void handleFunTime() {
        switch (mode1State) {
            case UPDATING -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen) || !isAhScreen(screen)) break;
                if (mode1Timer.reached(currentUpdateDelay, false)) {
                    updateAuction(screen);
                    onAfterUpdate();
                    buyItems(screen);
                    mode1UpdateCount++;
                    currentUpdateDelay = (long) updateDelay.get();
                    mode1Timer.reset();
                    if (mode1UpdateCount >= (int) updateCount.get()) {
                        mode1State = Mode1State.CLOSING;
                        mode1UpdateCount = 0;
                        mode1Timer.reset();
                    }
                } else if (buyTimer.reached(currentBuyDelay, true)) {
                    buyItems(screen);
                    currentBuyDelay = (long) buyDelay.get();
                }
            }
            case CLOSING -> {
                if (mc.currentScreen != null) mc.currentScreen.close();
                mode1State = Mode1State.PAUSING;
                mode1Timer.reset();
            }
            case PAUSING -> {
                if (mode1Timer.reached((long) pauseBetweenCycles.get(), false)) {
                    mode1State = Mode1State.REOPENING;
                    mode1Timer.reset();
                }
            }
            case REOPENING -> {
               NetworkUtility.sendCommand("ah");
                mode1State = Mode1State.WAIT_AH_LOADED;
                mode1Timer.reset();
            }
            case WAIT_AH_LOADED -> {
                if (mc.currentScreen instanceof GenericContainerScreen s && isAhScreen(s)) {
                    mode1State = Mode1State.UPDATING;
                    mode1UpdateCount = 0;
                    mode1Timer.reset();
                } else if (mode1Timer.reached(5000, false)) {
                    mode1State = Mode1State.REOPENING;
                    mode1Timer.reset();
                }
            }
        }
    }

    private void updateAuction(GenericContainerScreen screen) {
        clickedSlots.clear();
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            String name = stack.getName().getString().toLowerCase();
            if (name.contains("обновить") || name.contains("refresh") || name.contains("update")) {
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                break;
            }
        }
    }

    private void buyItems(GenericContainerScreen screen) {
        buyItemsOnce(screen);
    }

    private boolean buyItemsOnce(GenericContainerScreen screen) {
        if (pendingPurchase != null) return false;
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || clickedSlots.contains(slot.id)) continue;
            String seller = AutoBuyUtil.getSeller(stack);
            if (seller != null && isPlayerIgnored(seller)) continue;
            for (ItemBuy itemBuy : getEnabledItems()) {
                if (!itemBuy.isBuy(stack)) continue;
                int totalPrice = (int) Math.min(AutoBuyUtil.getPrice(stack), Integer.MAX_VALUE);
                if (totalPrice == Integer.MAX_VALUE) continue;
                int count = stack.getCount();
                int pricePerItem = count > 0 ? totalPrice / count : totalPrice;
                Integer maxPrice = itemMaxPrice.get(getItemKey(itemBuy));
                if (maxPrice == null || pricePerItem > maxPrice) continue;
                if (stack.isDamageable()) {
                    Integer minDur = itemMinDurability.get(getItemKey(itemBuy));
                    if (minDur != null && minDur > 0 && (stack.getMaxDamage() - stack.getDamage()) < minDur) continue;
                }
                ItemStack copy = stack.copy();
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                clickedSlots.add(slot.id);
                if (!copy.isEmpty()) pendingPurchase = new PendingPurchase(copy, totalPrice, itemBuy);
                return true;
            }
        }
        return false;
    }

    private void onAfterUpdate() {
        trickUpdateCount++;
        if (trickUpdateCount >= trickUpdateTarget) {
            isTrickPaused = true;
            trickPauseDuration = 1000 + new Random().nextInt(2001);
            trickPauseTimer.reset();
        }
    }
    private void onShulkerDone() {
        mode1State = Mode1State.UPDATING;
        mode1UpdateCount = 0;
        mode1Timer.reset();
        defaultTimer.reset();
    }

    private void onRelistDone() {
        mode1State = Mode1State.UPDATING;
        mode1UpdateCount = 0;
        mode1Timer.reset();
        defaultTimer.reset();
    }
    public void openConfig() {
        if (mc.currentScreen != null) mc.currentScreen = null;
        Client.AUTOBUY_CONFIG.setOpened(true);
        mc.mouse.unlockCursor();
    }

    public void openItemsMenu() { Client.AUTOBUY_ITEMS.setOpened(true); }

    public AutoBuyManager getManager() { return manager; }
    public List<ItemBuy> getAllItems() {
        List<ItemBuy> all = new ArrayList<>();
        all.addAll(manager.getVanilla());
        all.addAll(manager.getFuntime());
        all.addAll(manager.getHollyworld());
        return all;
    }

    public List<ItemBuy> getEnabledItems() {
        List<ItemBuy> result = new ArrayList<>();
        for (ItemBuy item : getAllItems()) {
            if (Boolean.TRUE.equals(itemEnabled.get(getItemKey(item)))) result.add(item);
        }
        return result;
    }

    public String getItemKey(ItemBuy item) {
        return switch (item.getCategory()) {
            case FUNTIME    -> "[FT] " + item.getDisplayName();
            case HOLLYWORLD -> "[HW] " + item.getDisplayName();
            default         -> item.getDisplayName();
        };
    }

    public String getItemDisplayKey(ItemBuy item) { return getItemKey(item); }

    public boolean isItemEnabled(String displayName) {
        return Boolean.TRUE.equals(itemEnabled.get(displayName));
    }

    public boolean isItemEnabled(ItemBuy item) {
        return isItemEnabled(getItemKey(item));
    }

    public void setItemEnabled(String displayName, boolean enabled) {
        itemEnabled.put(displayName, enabled);
    }

    public void setItemEnabled(ItemBuy item, boolean enabled) {
        setItemEnabled(getItemKey(item), enabled);
    }

    public int getItemMaxPrice(String displayName) {
        return itemMaxPrice.getOrDefault(displayName, 1000);
    }

    public void setItemMaxPrice(String key, int price) {
        itemMaxPrice.put(key, price);
    }

    public boolean itemHasDurability(ItemBuy item) {
        if (item == null) return false;
        try {
            ItemStack stack = item.getItemStack();
            return stack != null && stack.isDamageable();
        } catch (Exception e) {
            return false;
        }
    }

    public int getItemMinDurability(String key) {
        return itemMinDurability.getOrDefault(key, 0);
    }

    public int getItemMinDurability(ItemBuy item) {
        return getItemMinDurability(getItemKey(item));
    }

    public void setItemMinDurability(String key, int dur) {
        itemMinDurability.put(key, dur);
    }

    public void setItemMinDurability(ItemBuy item, int dur) {
        setItemMinDurability(getItemKey(item), dur);
    }

    public int getItemCustomSellPrice(String key) {
        return itemCustomSellPrice.getOrDefault(key, 0);
    }

    public int getItemCustomSellPrice(ItemBuy item) {
        return getItemCustomSellPrice(getItemKey(item));
    }

    public void setItemCustomSellPrice(String key, int price) {
        itemCustomSellPrice.put(key, price);
    }

    public void setItemCustomSellPrice(ItemBuy item, int price) {
        setItemCustomSellPrice(getItemKey(item), price);
    }

    public boolean isItemAutoSellEnabled(ItemBuy item) {
        return Boolean.TRUE.equals(itemAutoSellEnabled.getOrDefault(getItemKey(item), true));
    }

    public void setItemAutoSellEnabled(ItemBuy item, boolean enabled) {
        itemAutoSellEnabled.put(getItemKey(item), enabled);
    }

    public boolean isItemSellModeCustom(ItemBuy item) {
        return Boolean.TRUE.equals(itemSellModeCustom.getOrDefault(getItemKey(item), false));
    }

    public void setItemSellModeCustom(ItemBuy item, boolean custom) {
        itemSellModeCustom.put(getItemKey(item), custom);
    }

    public List<String> getIgnoredPlayers() { return ignoredPlayers; }

    public boolean isPlayerIgnored(String name) {
        return ignoredPlayers.stream().anyMatch(p -> p.equalsIgnoreCase(name));
    }

    public void addIgnoredPlayer(String name) { ignoredPlayers.add(name); }

    public void removeIgnoredPlayer(String name) {
        ignoredPlayers.removeIf(p -> p.equalsIgnoreCase(name));
    }
    public boolean isSelling()       { return sellManager.isSelling(); }
    public String  getSellStatus()   { return sellManager.getStatus(); }
    public int     getPendingCount() { return sellManager.getPendingCount(); }
    public int     getReadyCount()   { return sellManager.getReadyCount(); }

    public void startParser() {
        List<ItemBuy> targets = getEnabledItems();
        if (targets.isEmpty()) {
            ChatUtility.send(Text.literal("AutoParser: Нет включённых предметов").withColor(new Color(255, 200, 100).getRGB()));
            return;
        }
        parserManager.start(targets);
    }
    public void stopParser()                          { parserManager.stop(); }
    public boolean isParserRunning()                  { return parserManager.isRunning(); }
    public String  getParserStatus()                  { return parserManager.getStatus(); }
    public boolean isAutoParserEnabled()              { return autoParserEnabled.get(); }
    public int     getAutoParserIntervalMinutes()     { return (int) autoParserInterval.get(); }
    public long    getAutoParserTimeLeft()            { return parserManager.getTimeLeft(); }
    public boolean isAutoParserItem(ItemBuy item) {
        return itemAutoParserEnabled.getOrDefault(getItemKey(item), true);
    }
    public void toggleAutoParserItem(ItemBuy item) {
        String key = getItemKey(item);
        itemAutoParserEnabled.put(key, !isAutoParserItem(item));
    }

    public List<ItemBuy> getAutoParserItems() {
        List<ItemBuy> result = new ArrayList<>();
        for (ItemBuy item : getAllItems()) {
            if (isAutoParserItem(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public void startParserForItems(List<ItemBuy> items) { parserManager.start(items); }
    public void    setAutoBuyPaused(boolean paused) { this.autoBuyPaused = paused; }
    public boolean isAutoBuyPaused()                { return autoBuyPaused; }


    @Override
    public void save(JsonObject root) {
        super.save(root);
        JsonObject items = new JsonObject();
        for (Map.Entry<String, Boolean> e : itemAutoParserEnabled.entrySet()) items.addProperty("autoparser_" + e.getKey(), e.getValue());
        for (Map.Entry<String, Boolean> e : itemEnabled.entrySet())         items.addProperty("enabled_" + e.getKey(), e.getValue());
        for (Map.Entry<String, Integer> e : itemMaxPrice.entrySet())        items.addProperty("maxprice_" + e.getKey(), e.getValue());
        for (Map.Entry<String, Integer> e : itemMinDurability.entrySet())   items.addProperty("mindur_" + e.getKey(), e.getValue());
        for (Map.Entry<String, Boolean> e : itemAutoSellEnabled.entrySet()) items.addProperty("autosell_" + e.getKey(), e.getValue());
        for (Map.Entry<String, Boolean> e : itemSellModeCustom.entrySet())  items.addProperty("sellcustom_" + e.getKey(), e.getValue());
        for (Map.Entry<String, Integer> e : itemCustomSellPrice.entrySet()) items.addProperty("sellprice_" + e.getKey(), e.getValue());
        root.add("autobuyItems", items);
        JsonArray ignored = new JsonArray();
        ignoredPlayers.forEach(ignored::add);
        root.add("autobuyIgnored", ignored);
    }

    @Override
    public void load(JsonObject root) {
        super.load(root);
        if (root.has("autobuyItems")) {
            JsonObject items = root.getAsJsonObject("autobuyItems");
            for (Map.Entry<String, JsonElement> e : items.entrySet()) {
                String k = e.getKey();
                if      (k.startsWith("enabled_"))    itemEnabled.put(k.substring(8), e.getValue().getAsBoolean());
                else if (k.startsWith("maxprice_"))   itemMaxPrice.put(k.substring(9), e.getValue().getAsInt());
                else if (k.startsWith("mindur_"))     itemMinDurability.put(k.substring(7), e.getValue().getAsInt());
                else if (k.startsWith("autoparser_")) itemAutoParserEnabled.put(k.substring(11), e.getValue().getAsBoolean());
                else if (k.startsWith("autosell_"))   itemAutoSellEnabled.put(k.substring(9), e.getValue().getAsBoolean());
                else if (k.startsWith("sellcustom_")) itemSellModeCustom.put(k.substring(11), e.getValue().getAsBoolean());
                else if (k.startsWith("sellprice_"))  itemCustomSellPrice.put(k.substring(10), e.getValue().getAsInt());
            }
        }
        if (root.has("itemSettings")) {
            JsonObject items = root.getAsJsonObject("itemSettings");
            for (Map.Entry<String, JsonElement> e : items.entrySet()) {
                String k = e.getKey();
                if      (k.startsWith("enabled_"))    itemEnabled.put(k.substring(8), e.getValue().getAsBoolean());
                else if (k.startsWith("maxprice_"))   itemMaxPrice.put(k.substring(9), e.getValue().getAsInt());
                else if (k.startsWith("mindur_"))     itemMinDurability.put(k.substring(7), e.getValue().getAsInt());
                else if (k.startsWith("autosell_"))   itemAutoSellEnabled.put(k.substring(9), e.getValue().getAsBoolean());
                else if (k.startsWith("sellcustom_")) itemSellModeCustom.put(k.substring(11), e.getValue().getAsBoolean());
                else if (k.startsWith("sellprice_"))  itemCustomSellPrice.put(k.substring(10), e.getValue().getAsInt());
            }
        }
        if (root.has("autobuyIgnored")) {
            ignoredPlayers.clear();
            root.getAsJsonArray("autobuyIgnored").forEach(e -> ignoredPlayers.add(e.getAsString()));
        }
    }
    public void saveConfig(JsonObject root) { save(root); }
    public void loadConfig(JsonObject root) { load(root); }

    private void initItemSettings() {
        for (ItemBuy item : getAllItems()) {
            String key = getItemKey(item);
            itemEnabled.putIfAbsent(key, false);
            itemMaxPrice.putIfAbsent(key, 1000);
            itemMinDurability.putIfAbsent(key, 0);
            itemAutoSellEnabled.putIfAbsent(key, true);
            itemSellModeCustom.putIfAbsent(key, false);
            itemCustomSellPrice.putIfAbsent(key, 0);
        }
    }

    private boolean isAhScreen(GenericContainerScreen screen) {
        String t = screen.getTitle().getString().toLowerCase();
        return t.contains("аукцион") || t.contains("auction");
    }

    private void performMemoryCleanup() {
        AutoBuyUtil.clearCache();
    }

  //  private void handleTgCommand(String cmd) {
  //      if (cmd.equalsIgnoreCase("/status")) {
   //         tgManager.notifyBought(new net.minecraft.item.ItemStack(net.minecraft.item.Items.AIR), 0);
   //     }
   // }

    private String formatPrice(int price) {
        StringBuilder f = new StringBuilder();
        String s = String.valueOf(price);
        int start = s.length() % 3;
        if (start > 0) f.append(s, 0, start);
        for (int i = start; i < s.length(); i += 3) {
            if (f.length() > 0) f.append('.');
            f.append(s, i, i + 3);
        }
        return f.toString();
    }

    private enum Mode1State { UPDATING, CLOSING, PAUSING, REOPENING, WAIT_AH_LOADED }

    public enum UpdateMode {
        DEFAULT("По умолчанию"),
        FunTime("Переоткрытие");

        private final String displayName;
        UpdateMode(String d) { this.displayName = d; }
        @Override public String toString() { return displayName; }
    }
}
