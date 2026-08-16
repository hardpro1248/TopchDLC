package gg.topchdlc.vse.shutki.module.modules.impl.player;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;

public class AutoInvis extends Module {
    public static final AutoInvis INSTANCE = new AutoInvis();

    public boolean isDrinking = false;

    private AutoInvis() {
        super("AutoInvis", Category.PLAYER, "пьет инвизку причем криво пиздец");
    }
    private final CheckBox autoMode = checkbox("Auto (по эффекту)", false);
    private final SliderSetting drinkTimer = sliderSetting("Таймер (сек)", 170f, 5f, 300f).increment(5f).visible(() -> !autoMode.get());

    private final TimeUtility timer     = new TimeUtility();
    private final TimeUtility stepTimer = new TimeUtility();
    private static final int DRINK_TICKS = 35;

    private enum State { IDLE, SWAP_IN, HOLD_USE, SWAP_OUT }
    private State state = State.IDLE;
    private int drinkSlot = -1;
    private int invSlot   = -1;
    private int prevSlot  = -1;
    private int holdTicks = 0;

    @Override
    protected void onEnable() {
        super.onEnable();
        timer.reset();
        reset();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        stopHolding();
        if (state != State.IDLE) restoreSlot();
        reset();
    }

    private void reset() {
        state     = State.IDLE;
        isDrinking = false;
        drinkSlot = -1;
        invSlot   = -1;
        prevSlot  = -1;
        holdTicks = 0;
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) onTick();
    };

    private void onTick() {
        if (nullCheck()) return;

        switch (state) {
            case IDLE      -> tickIdle();
            case SWAP_IN   -> tickSwapIn();
            case HOLD_USE  -> tickHoldUse();
            case SWAP_OUT  -> tickSwapOut();
        }
    }

    private void tickIdle() {
        boolean should = autoMode.get()
                ? !hasInvisEffect()
                : timer.reached((long)(drinkTimer.get() * 1000f));
        if (!should) return;

        int hotbar = findInvisInHotbar();
        prevSlot = mc.player.getInventory().getSelectedSlot();

        if (hotbar != -1) {
            drinkSlot = hotbar;
            invSlot   = -1;
            isDrinking = true;
            holdTicks  = 0;
            selectSlot(drinkSlot);
            state = State.HOLD_USE;
            stepTimer.reset();
        } else {
            int inv = findInvisInInventory();
            if (inv == -1) return;
            drinkSlot = findFreeHotbarSlot();
            invSlot   = inv;
            isDrinking = true;
            state = State.SWAP_IN;
            stepTimer.reset();
        }
    }

    private void tickSwapIn() {
        if (!stepTimer.reached(50)) return;

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                invSlot, drinkSlot,
                SlotActionType.SWAP, mc.player);

        selectSlot(drinkSlot);
        holdTicks = 0;
        state = State.HOLD_USE;
        stepTimer.reset();
    }

    private void tickHoldUse() {
        if (mc.player.getInventory().getSelectedSlot() != drinkSlot) {
            selectSlot(drinkSlot);
        }

        mc.options.useKey.setPressed(true);
        holdTicks++;
        if (holdTicks >= DRINK_TICKS) {
            stopHolding();
            timer.reset();

            if (invSlot != -1) {
                state = State.SWAP_OUT;
                stepTimer.reset();
            } else {
                restoreSlot();
                isDrinking = false;
                state = State.IDLE;
            }
        }
    }
    private void tickSwapOut() {
        if (!stepTimer.reached(100)) return;

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                invSlot, drinkSlot,
                SlotActionType.SWAP, mc.player);

        restoreSlot();
        isDrinking = false;
        state = State.IDLE;
    }


    private void selectSlot(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void stopHolding() {
        if (mc.options != null) mc.options.useKey.setPressed(false);
    }

    private void restoreSlot() {
        if (prevSlot != -1 && mc.player != null) {
            selectSlot(prevSlot);
        }
        prevSlot = -1;
    }

    private int findInvisInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (isInvisPotion(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private int findInvisInInventory() {
        for (int i = 9; i < 36; i++) {
            if (isInvisPotion(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private int findFreeHotbarSlot() {
        int cur = mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            if (i != cur && mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return cur == 0 ? 1 : 0;
    }

    private boolean isInvisPotion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() != Items.POTION && stack.getItem() != Items.SPLASH_POTION) return false;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;
        for (StatusEffectInstance effect : contents.getEffects()) {
            if (effect.getEffectType() == StatusEffects.INVISIBILITY) return true;
        }
        return false;
    }

    private boolean hasInvisEffect() {
        for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
            if (effect.getEffectType() == StatusEffects.INVISIBILITY) return true;
        }
        return false;
    }
}
