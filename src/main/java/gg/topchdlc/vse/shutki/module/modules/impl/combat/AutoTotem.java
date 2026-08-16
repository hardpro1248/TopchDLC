package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import gg.topchdlc.vse.utils.player.swap.SwapUtility;

public class AutoTotem extends Module {
    public static final AutoTotem INSTANCE = new AutoTotem();

    private AutoTotem() {
        super("Auto totem", Category.COMBAT, "берет тотем при опасностях");
    }

    public enum SwapHand {
        OFFHAND,
        MAINHAND
    }
    public enum BypassMode {
        Universial,
        Matrix,
        GrimPacket,
        Vanilla
    }
    public enum Mode {
        Health,
        Force
    }
    public final EnumSetting<BypassMode> bypassmode = enumSetting("Bypass Mode", BypassMode.Universial);
    public final EnumSetting<SwapHand> swapHand = enumSetting("Swap Hand", SwapHand.OFFHAND);
    public final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Health);
    public final CheckBox notInScreen = checkbox("Not in screen", true);
    public final CheckBox stopEating = checkbox("Stop eating", true);
    public CheckBox smartCalc = checkbox("Calculate trigger health", false).visible(() -> mode.is(Mode.Health));
    public SliderSetting trigger = sliderSetting("Trigger health", 3, 1, 19).increment(0.1f).visible(() -> mode.is(Mode.Health) && !smartCalc.get());
    public CheckBox saveEnchanted = checkbox("Save enchanted", false);

    public final CheckBox returnItem = checkbox("Return item", true);
    public final SliderSetting returnDelay = sliderSetting("Return Delay", 150, 0, 2000).increment(1).visible(returnItem::get);

    public final Group mainHandGroup = group("MainHand Settings").visible(() -> swapHand.is(SwapHand.MAINHAND));
    public final CheckBox switchBack = mainHandGroup.checkbox("Switch Back", true).visible(returnItem::get);
    public final SliderSetting safeDelay = mainHandGroup.sliderSetting("Safe Delay", 500, 100, 2000).increment(50)
            .visible(() -> switchBack.get() && returnItem.get());

    public final Group checksGroup = group("Проверки");
    public final CheckBox elytraCheck = checksGroup.checkbox("Elytra", false);
    public final SliderSetting elytraHealth = checksGroup.sliderSetting("Elytra health", 5, 1, 19).increment(0.1f).visible(elytraCheck::get);
    public final CheckBox crystalCheck = checksGroup.checkbox("Crystal", false);
    public final SliderSetting crystalRange = checksGroup.sliderSetting("Crystal range", 6, 1, 10).increment(0.5f).visible(crystalCheck::get);
    public final CheckBox fallCheck = checksGroup.checkbox("Fall", false);
    public final SliderSetting fallDistance = checksGroup.sliderSetting("Fall distance", 15, 10, 20).increment(0.5f).visible(fallCheck::get);
    public final CheckBox tntCheck = checksGroup.checkbox("TNT", false);
    public final SliderSetting tntRange = checksGroup.sliderSetting("TNT range", 6, 1, 15).increment(0.5f).visible(tntCheck::get);
    public final CheckBox tridentcheck = checksGroup.checkbox("Trident", false);
    public final SliderSetting tridentRange = checksGroup.sliderSetting("Trident range", 6, 1, 15).increment(0.5f).visible(tridentcheck::get);
    public final CheckBox minecart = checksGroup.checkbox("MineCart", false);
    public final SliderSetting minecartRange = checksGroup.sliderSetting("MineCart range", 6, 1, 15).increment(0.5f).visible(minecart::get);

    int lastSlot = -1;
    int prevSelectedSlot = -1;
    float prevHealth = -1;
    int lastDamage = 0;
    boolean threatActive = false;

    boolean hadTotemInOffhand = false;
    boolean wasUsingBefore = false;

    TimeUtility swapDelay = new TimeUtility();
    TimeUtility safeTimer = new TimeUtility();


    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null) return;


            if (notInScreen.get() && mc.currentScreen != null) return;
            float health = mc.player.getHealth();
            float triggerHealth = this.trigger.get();
            boolean hasElytraEquipped = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
            boolean elytraTriggered = elytraCheck.get() && hasElytraEquipped && health < elytraHealth.get();
            boolean crystalNearby = false;
            if (crystalCheck.get()) {
                double range = crystalRange.get();
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof EndCrystalEntity && mc.player.distanceTo(entity) <= range) {
                        crystalNearby = true;
                        break;
                    }
                }
            }
            boolean fallTriggered = fallCheck.get() && mc.player.fallDistance >= fallDistance.get();
            boolean tntNearby = false;
            if (tntCheck.get()) {
                double range = tntRange.get();
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof TntEntity && mc.player.distanceTo(entity) <= range) {
                        tntNearby = true;
                        break;
                    }
                }
            }
            boolean tridentNearby = false;
            if (tridentcheck.get()) {
                double range = tridentRange.get();
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof TridentEntity && mc.player.distanceTo(entity) <= range) {
                        tridentNearby = true;
                        break;
                    }
                }
            }
            if (minecart.get()) {
                double range = minecartRange.get();
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof MinecartEntity && mc.player.distanceTo(entity) <= range) {
                        tridentNearby = true;
                        break;
                    }
                }
            }
            boolean lowHealth = (!smartCalc.get() && health < triggerHealth) ||
                    (smartCalc.get() && health - prevHealth <= 1);
            boolean currentThreat = mode.is(Mode.Force) || lowHealth || elytraTriggered || crystalNearby || fallTriggered || tntNearby || tridentNearby;

            if (currentThreat) {
                if (!threatActive) {
                    hadTotemInOffhand = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
                    threatActive = true;
                }
                safeTimer.reset();
                takeTotem();
            } else {
                if (threatActive) {
                    if (safeTimer.reached(safeDelay.getLong())) {
                        threatActive = false;
                        swapBack();
                        hadTotemInOffhand = false;
                    } else {
                        if (swapHand.is(SwapHand.MAINHAND)) {
                            keepTotemSelected();
                        }
                    }
                } else {
                    if (!smartCalc.get() || lastDamage > 20) {
                        swapBack();
                        hadTotemInOffhand = false;
                    }
                }
            }

            boolean hasTotemEquipped = swapHand.is(SwapHand.OFFHAND) ? hasTotemOffhand() : hasTotemMainhand();
            if (wasUsingBefore) {
                if (!isUseKeyPhysicallyPressed()) {
                    wasUsingBefore = false;
                } else if (hasTotemEquipped) {
                    mc.options.useKey.setPressed(true);
                    wasUsingBefore = false;
                }
            }

            lastDamage++;
            if (mc.player.hurtTime > 0) {
                lastDamage = 0;
            }
        }
        if (event instanceof EventReceivePacket p) {
            if (mc.world == null || mc.player == null || !smartCalc.get() || (notInScreen.get() && mc.currentScreen != null)) return;

            if (p.getPacket() instanceof HealthUpdateS2CPacket hp) {
                if (hp.getHealth() < mc.player.getHealth()) {
                    prevHealth = MathUtility.delta(hp.getHealth(), mc.player.getHealth());
                }
            }
        }
    };

    @Override
    protected void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        resetState();
    }

    private void resetState() {
        lastSlot = -1;
        prevSelectedSlot = -1;
        threatActive = false;
        hadTotemInOffhand = false;
        wasUsingBefore = false;
        safeTimer.reset();
    }

    private void takeTotem() {
        boolean needSwap = swapHand.is(SwapHand.OFFHAND) ? !hasTotemOffhand() : !hasTotemMainhand();

        if (stopEating.get() && mc.player.isUsingItem() && needSwap) {
            wasUsingBefore = true;
            mc.options.useKey.setPressed(false);
            mc.interactionManager.stopUsingItem(mc.player);
        }

        if (swapHand.is(SwapHand.OFFHAND)) {
            takeTotemOffhand();
        } else {
            takeTotemMainhand();
        }
    }

    private void takeTotemOffhand() {
        if (hasTotemOffhand()) return;
        if (!swapDelay.reached(100, true)) return;

        int slot = InventoryUtility.find(Items.TOTEM_OF_UNDYING, true);
        if (slot == -1 && saveEnchanted.get()) {
            ItemStack offhand = mc.player.getOffHandStack();
            if (!offhand.isEmpty() && offhand.getItem() == Items.TOTEM_OF_UNDYING) return;
            slot = InventoryUtility.find(Items.TOTEM_OF_UNDYING, false);
        }
        if (slot == -1) {
            slot = InventoryUtility.find(Items.TOTEM_OF_UNDYING, false);
        }
        if (slot == -1) return;

        if (lastSlot == -1) {
            lastSlot = slot;
        }

        int fromSlot = InventoryUtility.wrapHotbar(slot);
        if(bypassmode.is(BypassMode.Matrix)){
            SwapUtility.matrixswaptooffhend(fromSlot);
        } else if (bypassmode.is(BypassMode.Universial)) {
            SwapUtility.safeSwapToOffhand(fromSlot);
        } else if (bypassmode.is(BypassMode.Vanilla)) {
            SwapUtility.vanilaswap(fromSlot);
        } else if (bypassmode.is(BypassMode.GrimPacket)) {
            SwapUtility.GrimPacketSwapOffHand(fromSlot);
        }
    }

    private void takeTotemMainhand() {
        if (hasTotemMainhand()) {
            return;
        }
        int hotbarSlot = findTotemInHotbar();

        if (hotbarSlot != -1) {
            if (prevSelectedSlot == -1) {
                prevSelectedSlot = getSelectedSlot();
            }
            if (getSelectedSlot() != hotbarSlot) {
                selectSlot(hotbarSlot);
            }
        } else {
            if (!swapDelay.reached(100, true)) return;
            int invSlot = InventoryUtility.find(Items.TOTEM_OF_UNDYING, true);
            if (invSlot == -1 && saveEnchanted.get()) {
                invSlot = InventoryUtility.find(Items.TOTEM_OF_UNDYING, false);
            }
            if (invSlot == -1) {
                invSlot = InventoryUtility.find(Items.TOTEM_OF_UNDYING, false);
            }
            if (invSlot == -1) return;
            if (prevSelectedSlot == -1) {
                prevSelectedSlot = getSelectedSlot();
            }
            int currentHotbarSlot = getSelectedSlot();

            {
                safeSwapToMainhand(invSlot, currentHotbarSlot);
            }
        }
    }

    private void keepTotemSelected() {
        int totemSlot = findTotemInHotbar();
        if (totemSlot != -1 && getSelectedSlot() != totemSlot) {
            selectSlot(totemSlot);
        }
    }

    private void swapBack() {
        if (swapHand.is(SwapHand.OFFHAND)) {
            swapBackOffhand();
        } else {
            swapBackMainhand();
        }
    }

    private void swapBackOffhand() {
        if (!returnItem.get()) {
            lastSlot = -1;
            return;
        }
        if (hadTotemInOffhand) {
            lastSlot = -1;
            return;
        }

        if (lastSlot != -1 && hasTotemOffhand()) {
            if (!swapDelay.reached(returnDelay.getLong(), true)) return;
            int fromSlot = InventoryUtility.wrapHotbar(lastSlot);

             {
                 if(bypassmode.is(BypassMode.Matrix)){
                     SwapUtility.matrixswaptooffhend(fromSlot);
                 } else if (bypassmode.is(BypassMode.Universial)) {
                    SwapUtility.safeSwapToOffhand(fromSlot);
                } else if (bypassmode.is(BypassMode.Vanilla)) {
                    SwapUtility.vanilaswap(fromSlot);
                } else if (bypassmode.is(BypassMode.GrimPacket)) {
                    SwapUtility.GrimPacketSwapOffHand(fromSlot);
                }
                lastSlot = -1;
                swapDelay.reset();
            }
        }
    }

    private void swapBackMainhand() {
        if (!returnItem.get() || !switchBack.get()) {
            prevSelectedSlot = -1;
            return;
        }
        if (threatActive) {
            return;
        }
        if (prevSelectedSlot != -1) {
            if (!swapDelay.reached(returnDelay.getLong(), true)) return;

            selectSlot(prevSelectedSlot);
            prevSelectedSlot = -1;
            swapDelay.reset();
        }
    }

    private boolean hasTotemOffhand() {
        ItemStack stack = mc.player.getOffHandStack();
        if (stack.isEmpty() || stack.getItem() != Items.TOTEM_OF_UNDYING) return false;
        if (saveEnchanted.get() && stack.hasEnchantments()) {
            boolean hasRegular = InventoryUtility.find(Items.TOTEM_OF_UNDYING, true) != -1;
            return !hasRegular;
        }
        return true;
    }

    private boolean hasTotemMainhand() {
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty() || stack.getItem() != Items.TOTEM_OF_UNDYING) return false;
        if (saveEnchanted.get() && stack.hasEnchantments()) {
            return InventoryUtility.find(Items.TOTEM_OF_UNDYING, true) == -1;
        }
        return true;
    }

    private int getSelectedSlot() {
        return mc.player.getInventory().getSelectedSlot();
    }

    private void selectSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        mc.options.hotbarKeys[slot].setPressed(true);
        mc.options.hotbarKeys[slot].setPressed(false);
    }

    private int findTotemInHotbar() {
        int enchantedFallback = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                if (!saveEnchanted.get() || !stack.hasEnchantments()) {
                    return i;
                } else if (enchantedFallback == -1) {
                    enchantedFallback = i;
                }
            }
        }
        if (saveEnchanted.get() && enchantedFallback != -1) {
            if (InventoryUtility.find(Items.TOTEM_OF_UNDYING, true) == -1) {
                return enchantedFallback;
            }
        }
        return -1;
    }

    private void safeSwapToMainhand(int inventorySlot, int hotbarSlot) {
        int syncId = mc.player.currentScreenHandler.syncId;
        int fromSlot;
        if (inventorySlot < 9) {
            fromSlot = inventorySlot + 36;
        } else {
            fromSlot = inventorySlot;
        }
        mc.interactionManager.clickSlot(syncId, fromSlot, hotbarSlot, SlotActionType.SWAP, mc.player);
    }


    private boolean isUseKeyPhysicallyPressed() {
        try {
            Object boundKey = null;
            try {
                java.lang.reflect.Method m = mc.options.useKey.getClass().getMethod("getBoundKey");
                boundKey = m.invoke(mc.options.useKey);
            } catch (NoSuchMethodException e) {
                for (java.lang.reflect.Field f : mc.options.useKey.getClass().getDeclaredFields()) {
                    if (f.getType().getSimpleName().equals("Key") || f.getType().getName().contains("InputUtil$Key")) {
                        f.setAccessible(true);
                        boundKey = f.get(mc.options.useKey);
                        break;
                    }
                }
            }

            if (boundKey != null) {
                java.lang.reflect.Method getCodeMethod = boundKey.getClass().getMethod("getCode");
                int code = (int) getCodeMethod.invoke(boundKey);

                java.lang.reflect.Method getCategoryMethod = boundKey.getClass().getMethod("getCategory");
                Object category = getCategoryMethod.invoke(boundKey);

                long handle = mc.getWindow().getHandle();
                if (category.toString().contains("MOUSE")) {
                    return org.lwjgl.glfw.GLFW.glfwGetMouseButton(handle, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                } else {
                    return org.lwjgl.glfw.GLFW.glfwGetKey(handle, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                }
            }
        } catch (Exception ignored) {}
        return true;
    }
}