package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import gg.topchdlc.vse.utils.player.swap.FastSwapUtil;
import gg.topchdlc.vse.utils.player.swap.SwapUtility;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class ElytraHelper extends Module {
    public static final ElytraHelper INSTANCE = new ElytraHelper();
    TimeUtility fireworkTime = new TimeUtility();
    TimeUtility manualFireworkTime = new TimeUtility();

    public enum SwapSpeed {
        Fast("Fast"),
        GrimPacket("GrimPacket"),
        Safe("Safe");

        private final String name;
        SwapSpeed(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    public Group vzlet = group("Авто взлет");
    private ElytraHelper() {
        super("Elytra Helper", Category.PLAYER, "помогает со свапами элитров");
    }

    public KeybindSetting swap = keybindSetting("Кнопка свапа", -1);
    public EnumSetting<SwapSpeed> swapSpeed = enumSetting("Режим свапов", SwapSpeed.Fast);
    public KeybindSetting firework = keybindSetting("Кнопка фейерверка", -1);
    public CheckBox autoTakeoff = vzlet.checkbox("Auto-jump", false);
    public CheckBox legittakeoff = vzlet.checkbox("legitAJ", false).visible(autoTakeoff::get);
    public CheckBox startFly = vzlet.checkbox("Start firework", false);
    public CheckBox autoFirework = vzlet.checkbox("Auto firework", false);
    public CheckBox smartfeer = vzlet.checkbox("smart", true).visible(autoFirework::get);
    public SliderSetting delayUseFireWorkSetting = vzlet.sliderSetting("Delay Use", 400, 200, 800).increment(50)
            .visible(() -> !smartfeer.get() && autoFirework.get());

    private ItemStack currentChest = ItemStack.EMPTY;
    private boolean wasGliding = false;
    private int takeoffTicks = 0;
    private boolean waitingToGlide = false;

    EventBus<Event> events = event -> {

        if (event instanceof EventGameTick) {
            if (mc.player == null) return;
            currentChest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
            if (autoTakeoff.get() && !legittakeoff.get() && currentChest.isOf(Items.ELYTRA)){
                tryTakeoff(currentChest);
                if (currentChest.getItem() != Items.ELYTRA){
                    MoveUtility.stopGliding();
                }
            }
            if (autoTakeoff.get() && legittakeoff.get()) handleAutoTakeoff();
            autofirework();
        }

        if (event instanceof EventKey e) {
            if (mc.player == null || e.action != 1) return;
            handleKeyPress(e);
        }

        if (event instanceof EventInput e) {
            if (autoTakeoff.get() && legittakeoff.get() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                e.setJump(mc.player.age % 3 == 0);
            }
        }
    };

    private void handleAutoTakeoff() {
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() != Items.ELYTRA) {
            waitingToGlide = false;
            takeoffTicks = 0;
            return;
        }
        if (mc.player.isGliding()) {
            waitingToGlide = false;
            takeoffTicks = 0;
            return;
        }
        if (mc.player.isOnGround() && !waitingToGlide) {
            mc.player.jump();
            waitingToGlide = true;
            takeoffTicks = 0;
            return;
        }
        if (waitingToGlide) {
            takeoffTicks++;
            if (takeoffTicks >= 2 && mc.player.getVelocity().y < -0.08 && !mc.player.isGliding()) {
                mc.player.startGliding();
                waitingToGlide = false;
                takeoffTicks = 0;
            }
            if (takeoffTicks > 10) {
                waitingToGlide = false;
                takeoffTicks = 0;
            }
        }
    }

    private void handleKeyPress(EventKey e) {
        if (mc.player == null) return;
        ItemStack equipped = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (e.key == swap.getBind()) {
            if (FastSwapUtil.isBusy()) return;

            int chestPlateSlot = findChestplate();
            int elytraSlot = findItemSlot(Items.ELYTRA);

            if (equipped.getItem() == Items.ELYTRA) {
                if (chestPlateSlot != -1) {
                    performSwap(chestPlateSlot);
                } else {
                    performSwap(elytraSlot == -1 ? 6 : elytraSlot);
                }
                showNotification(false);
            } else {
                if (elytraSlot != -1) {
                    performSwap(elytraSlot);
                    showNotification(true);
                } else {
                    Client.NOTIFIES.add(Text.of("Элитра не найдена!"), IconUse.CROSS, 2000);
                }
            }
        }
        if (e.key == firework.getBind() && equipped.getItem() == Items.ELYTRA && mc.player.isGliding()) {
            if (!manualFireworkTime.reached(200)) return;
            useFirework();
            manualFireworkTime.reset();
        }
    }

    private void autofirework() {
        if (mc.player == null) return;
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() != Items.ELYTRA) return;
        int slot = findItemSlot(Items.FIREWORK_ROCKET);
        if (slot == -1) return;

        boolean gliding = mc.player.isGliding();
        if (!gliding) {
            wasGliding = false;
            return;
        }

        if (FastSwapUtil.isBusy()) return;

        boolean justStartedGlide = !wasGliding;
        wasGliding = true;

        if (justStartedGlide && startFly.get()) {
            if (!fireworkTime.reached(150)) return;
            useFirework();
            fireworkTime.reset();
            return;
        }

        if (!autoFirework.get()) return;
        if (!smartfeer.get() && autoFirework.get()) {
            long delay = (long) delayUseFireWorkSetting.get();
            if (!fireworkTime.reached(delay)) return;
        }
        if (smartfeer.get() && autoFirework.get()) {
            if (mc.player.isUsingItem()) {
                return;
            }

            long delay = 850;
            if (!fireworkTime.reached(delay)) return;
        }
        useFirework();
        fireworkTime.reset();
    }

    private void showNotification(boolean toElytra) {
        Text item = toElytra
                ? Text.of("Элитру").copy().withColor(ClientColors.MAIN_COLOR.getRGB())
                : Text.of("Нагрудник").copy().withColor(ClientColors.RED.getRGB());
        Text msg = Text.of("Свапнул на ").copy().append(item);
        Client.NOTIFIES.add(msg, IconUse.INFO, 3000);
    }

    private void performSwap(int slot) {
        if (FastSwapUtil.isBusy()) return;

        int fromSlot = (slot < 9) ? slot + 36 : slot;
        int armorSlot = 6;

        switch (swapSpeed.get()) {
            case Fast,Safe -> FastSwapUtil.swapElytra(fromSlot, armorSlot);
            case GrimPacket -> SwapUtility.grimSwapArmorInv(fromSlot, armorSlot);
        }
    }

    private void tryTakeoff(ItemStack chest) {
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return;
        if (mc.player.isOnGround()) {
            mc.player.jump();
        } else if (isElytraUsable(chest) && !mc.player.isGliding() && !mc.player.getAbilities().flying) {
            MoveUtility.startGliding();
        }
    }

    private boolean isElytraUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    private void useFirework() {
        if (Client.IS_PANIC || FastSwapUtil.isBusy()) return;

        int slot = findItemSlot(Items.FIREWORK_ROCKET);
        if (slot == -1) return;

        int fromSlot = (slot < 9) ? slot + 36 : slot;
        switch (swapSpeed.get()) {
            case Safe-> SwapUtility.safeUseItem(fromSlot);
            case GrimPacket,Fast -> FastSwapUtil.silentUseMatrix(Items.FIREWORK_ROCKET);
        }
    }

    private int findItemSlot(Item item) {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }

    private int findChestplate() {
        Item[] chestplates = {
                Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE
        };
        for (Item item : chestplates) {
            int slot = findItemSlot(item);
            if (slot != -1) return slot;
        }
        return -1;
    }

    @Override
    protected void onDisable() {
        FastSwapUtil.reset();
        waitingToGlide = false;
        takeoffTicks = 0;
        wasGliding = false;
    }
}