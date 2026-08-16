package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Create by daun kvass
 */
public class AutoEat extends Module {
    public static final AutoEat INSTANCE = new AutoEat();

    public boolean isEating = false;

    private final SliderSetting hungerThreshold = sliderSetting("Порог голода", 16f, 1f, 19f).increment(1f);
    private final CheckBox eatGapples = checkbox("Есть золотые яблоки", false);
    private final CheckBox eatEgapples = checkbox("Есть зачар. яблоки", false);
    private final CheckBox stopMovement = checkbox("Стопить движение ", true);

    private final TimeUtility stepTimer = new TimeUtility();
    private int startEatingTicks = 0;

    private enum State { IDLE, SWAP_IN, EATING, SWAP_OUT }
    private State state = State.IDLE;

    private int swapInvSlot    = -1;
    private int swapHotbarSlot = -1;
    private int activeSlot     = -1;
    private int prevSlot       = -1;

    private AutoEat() {
        super("AutoEat", Category.PLAYER, "Автоматически ест еду");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
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
        state          = State.IDLE;
        isEating       = false;
        swapInvSlot    = -1;
        swapHotbarSlot = -1;
        activeSlot     = -1;
        prevSlot       = -1;
        startEatingTicks = 0;
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventInput inputEvent) {
            if (isEating && swapInvSlot != -1 && stopMovement.get() && mc.player != null) {
                inputEvent.setForward(0.0f);
                inputEvent.setStrafe(0.0f);
                inputEvent.setJump(false);
                inputEvent.setSprint(false);
                inputEvent.setSneak(false);
            }
            return;
        }

        if (event instanceof EventGameTick) onTick();
    };

    private void onTick() {
        if (nullCheck()) return;

        if (isEating) {
            switch (state) {
                case SWAP_IN  -> tickSwapIn();
                case EATING   -> tickEating();
                case SWAP_OUT -> tickSwapOut();
            }
            return;
        }

        if (mc.player.isUsingItem()) return;

        int currentHunger = mc.player.getHungerManager().getFoodLevel();
        if (currentHunger <= (int) hungerThreshold.get()) {
            int foodSlot = findBestFoodSlot();
            if (foodSlot == -1) return;

            prevSlot = mc.player.getInventory().getSelectedSlot();
            isEating = true;

            if (foodSlot == 45) {
                swapInvSlot = -1;
                swapHotbarSlot = -1;

                int safeSlot = findNonUsableHotbarSlot();
                selectSlot(safeSlot);

                state = State.EATING;
                startEatingTicks = 0;
            } else if (foodSlot >= 0 && foodSlot < 9) {
                swapInvSlot = -1;
                swapHotbarSlot = -1;
                selectSlot(foodSlot);
                state = State.EATING;
                startEatingTicks = 0;
            } else {
                swapInvSlot = foodSlot;
                swapHotbarSlot = findFreeHotbarSlot();

                if (stopMovement.get()) {
                    var velocity = mc.player.getVelocity();
                    mc.player.setVelocity(0, velocity.y, 0);
                    mc.player.setSprinting(false);
                }

                state = State.SWAP_IN;
                stepTimer.reset();
            }
        }
    }

    private void tickSwapIn() {
        if (!stepTimer.reached(50)) return;

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                swapInvSlot, swapHotbarSlot,
                SlotActionType.SWAP, mc.player);

        selectSlot(swapHotbarSlot);
        state = State.EATING;
        startEatingTicks = 0;
    }

    private void tickEating() {
        mc.options.useKey.setPressed(true);
        startEatingTicks++;

        if (startEatingTicks > 6) {
            if (!mc.player.isUsingItem()) {
                finishEating();
            }
        }

        if (startEatingTicks > 45) {
            finishEating();
        }
    }

    private void finishEating() {
        stopHolding();
        if (swapInvSlot != -1) {
            state = State.SWAP_OUT;
            stepTimer.reset();
        } else {
            restoreSlot();
            reset();
        }
    }

    private void tickSwapOut() {
        if (!stepTimer.reached(50)) return;

        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                swapInvSlot, swapHotbarSlot,
                SlotActionType.SWAP, mc.player);

        restoreSlot();
        reset();
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

    private int findBestFoodSlot() {
        int bestSlot = -1;
        int bestPriority = 999;

        ItemStack offhand = mc.player.getOffHandStack();
        if (isFood(offhand)) {
            int priority = getFoodPriority(offhand);
            bestSlot = 45;
            bestPriority = priority;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isFood(stack)) {
                int priority = getFoodPriority(stack);
                if (priority < bestPriority) {
                    bestSlot = i;
                    bestPriority = priority;
                }
            }
        }

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isFood(stack)) {
                int priority = getFoodPriority(stack);
                if (priority < bestPriority) {
                    bestSlot = i;
                    bestPriority = priority;
                }
            }
        }

        return bestSlot;
    }

    private boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getComponents().get(DataComponentTypes.FOOD) == null) return false;

        Item item = stack.getItem();
        if (item == Items.GOLDEN_APPLE) {
            return eatGapples.get();
        }
        if (item == Items.ENCHANTED_GOLDEN_APPLE) {
            return eatEgapples.get();
        }
        return true;
    }

    private int getFoodPriority(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.ENCHANTED_GOLDEN_APPLE) return 3;
        if (item == Items.GOLDEN_APPLE) return 2;
        return 1;
    }

    private int findFreeHotbarSlot() {
        int cur = mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            if (i != cur && mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return cur == 0 ? 1 : 0;
    }


    private int findNonUsableHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) return i;
        }
        return mc.player.getInventory().getSelectedSlot();
    }
}