package gg.topchdlc.vse.shutki.module.modules.impl.farm;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Create by daun kvass
 */
public class AutoSardelki extends Module {
    public static final AutoSardelki INSTANCE = new AutoSardelki();

    private final SliderSetting actionDelay = sliderSetting("Задержка (мс)", 400f, 100f, 2000f)
            .increment(50f);
    private final SliderSetting npcRadius = sliderSetting("Радиус Рыбака", 10f, 3f, 30f)
            .increment(1f);
    private final SliderSetting codCount = sliderSetting("Треска", 40f, 0f, 64f)
            .increment(1f);
    private final SliderSetting salmonCount = sliderSetting("Лосось", 40f, 0f, 64f)
            .increment(1f);
    private final SliderSetting pufferfishCount = sliderSetting("Рыба фугу", 0f, 0f, 64f)
            .increment(1f);
    private final SliderSetting tropicalCount = sliderSetting("Тропическая рыбки ", 0f, 0f, 64f)
            .increment(1f);

    private enum State {
        IDLE,
        RETRACT_ROD,
        WAIT_RETRACT,
        CAST_ROD,
        WAIT_BOBBER,
        FISHING,
        REEL_IN,
        WAIT_REEL,
        FIND_NPC,
        LOOK_NPC,
        INTERACT_NPC,
        PRESS_USE,
        WAIT_SHOP,
        SELL_ITEMS,
        CLOSE_SHOP,
        WAIT_CLOSE
    }

    private State state = State.IDLE;
    private final TimeUtility timer = new TimeUtility();
    private double prevBobberY = 0;
    private int biteCheckTicks = 0;
    private float fishingYaw = 0;

    private AutoSardelki() {
        super("AutoSardelki", Category.PLAYER, "sordelki");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        state = State.IDLE;
        prevBobberY = 0;
        biteCheckTicks = 0;
        timer.reset();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        if (mc.options != null) mc.options.useKey.setPressed(false);
        state = State.IDLE;
    }
    private int findFishingRodSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof FishingRodItem) return i;
        }
        return -1;
    }
    private FishingBobberEntity getBobber() {
        if (mc.player == null) return null;
        return mc.player.fishHook;
    }
    private int findSellableSlot() {
        if (mc.player == null) return -1;
        var inv = mc.player.getInventory();

        Map<Item, Integer> thresholds = new LinkedHashMap<>();
        if ((int) codCount.get() > 0)        thresholds.put(Items.COD,            (int) codCount.get());
        if ((int) salmonCount.get() > 0)     thresholds.put(Items.SALMON,         (int) salmonCount.get());
        if ((int) pufferfishCount.get() > 0) thresholds.put(Items.PUFFERFISH,     (int) pufferfishCount.get());
        if ((int) tropicalCount.get() > 0)   thresholds.put(Items.TROPICAL_FISH,  (int) tropicalCount.get());

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;
            Integer threshold = thresholds.get(stack.getItem());
            if (threshold != null && stack.getCount() >= threshold) return i;
        }
        return -1;
    }
    private boolean hasSellable() {
        return findSellableSlot() != -1;
    }
    private Entity findNPC() {
        if (mc.player == null || mc.world == null) return null;
        double radius = npcRadius.get();
        Box box = mc.player.getBoundingBox().expand(radius);
        List<Entity> candidates = mc.world.getOtherEntities(mc.player, box);
        Entity best = null;
        double bestDist = radius + 1;
        for (Entity e : candidates) {
            String name    = e.getName().getString().toLowerCase();
            String display = e.getDisplayName().getString().toLowerCase();
            String custom  = e.getCustomName() != null ? e.getCustomName().getString().toLowerCase() : "";
            boolean match = name.contains("рыбак") || name.contains("fisherman") || name.contains("fisher")
                    || name.contains("рыбоньку купить") || name.contains("рыбу купить") || name.contains("рыбоньку")
                    || display.contains("рыбак") || display.contains("fisherman") || display.contains("fisher")
                    || display.contains("рыбоньку купить") || display.contains("рыбу купить") || display.contains("рыбоньку")
                    || custom.contains("рыбак") || custom.contains("fisherman") || custom.contains("fisher")
                    || custom.contains("рыбоньку купить") || custom.contains("рыбу купить") || custom.contains("рыбоньку");
            if (!match) continue;
            double dist = mc.player.distanceTo(e);
            if (dist < bestDist) { bestDist = dist; best = e; }
        }
        return best;
    }
    private int invSlotToScreen(int invSlot, int containerSize) {
        if (invSlot >= 9 && invSlot <= 35) return containerSize + (invSlot - 9);
        if (invSlot >= 0 && invSlot <= 8)  return containerSize + 27 + invSlot;
        return invSlot;
    }

    EventBus<Event> events = event -> {
        if (!(event instanceof EventGameTick)) return;
        if (mc.player == null || mc.world == null) return;

        long delay = (long) actionDelay.get();

        switch (state) {
            case IDLE: {
                if (hasSellable()) {
                    state = State.FIND_NPC;
                    timer.reset();
                    return;
                }
                int rod = findFishingRodSlot();
                if (rod == -1) {
                    ChatUtility.send(Text.literal(" §fУдочка не найдена в хотбаре!").withColor(new Color(255, 100, 100).getRGB()));
                    setEnabled(false);
                    return;
                }
                if (getBobber() != null) {
                    state = State.RETRACT_ROD;
                } else {
                    state = State.CAST_ROD;
                }
                timer.reset();
                break;
            }

            case RETRACT_ROD: {
                if (!timer.reached(delay, false)) return;
                int rod = findFishingRodSlot();
                if (rod == -1) { state = State.IDLE; timer.reset(); return; }
                mc.player.getInventory().setSelectedSlot(rod);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                state = State.WAIT_RETRACT;
                timer.reset();
                break;
            }

            case WAIT_RETRACT: {
                if (getBobber() == null) {
                    state = State.CAST_ROD;
                    timer.reset();
                } else if (timer.reached(3000, false)) {
                    state = State.RETRACT_ROD;
                    timer.reset();
                }
                break;
            }

            case CAST_ROD: {
                if (!timer.reached(delay, false)) return;
                int rod = findFishingRodSlot();
                if (rod == -1) { state = State.IDLE; timer.reset(); return; }
                mc.player.getInventory().setSelectedSlot(rod);
                mc.player.setPitch(50f);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                prevBobberY = Double.MAX_VALUE;
                biteCheckTicks = 0;
                state = State.WAIT_BOBBER;
                timer.reset();
                break;
            }

            case WAIT_BOBBER: {
                FishingBobberEntity b = getBobber();
                if (b != null) {
                    prevBobberY = b.getY();
                    biteCheckTicks = 0;
                    state = State.FISHING;
                    timer.reset();
                    return;
                }
                if (timer.reached(5000, false)) {
                    state = State.CAST_ROD;
                    timer.reset();
                }
                break;
            }

            case FISHING: {
                FishingBobberEntity bobber = getBobber();
                if (bobber == null) {
                    state = State.CAST_ROD;
                    timer.reset();
                    return;
                }

                double currentY = bobber.getY();
                biteCheckTicks++;
                if (biteCheckTicks > 20) {
                    double deltaY = currentY - prevBobberY;
                    if (deltaY < -0.1) {
                        state = State.REEL_IN;
                        timer.reset();
                        break;
                    }
                }
                prevBobberY = currentY;
                break;
            }

            case REEL_IN: {
                if (!timer.reached(100, false)) return;
                int rod = findFishingRodSlot();
                if (rod == -1) { state = State.IDLE; timer.reset(); return; }
                mc.player.getInventory().setSelectedSlot(rod);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                state = State.WAIT_REEL;
                timer.reset();
                break;
            }

            case WAIT_REEL: {
                if (getBobber() == null) {
                    if (hasSellable()) {
                        state = State.FIND_NPC;
                    } else {
                        state = State.CAST_ROD;
                    }
                    timer.reset();
                } else if (timer.reached(3000, false)) {
                    state = State.CAST_ROD;
                    timer.reset();
                }
                break;
            }

            case FIND_NPC: {
                if (!timer.reached(300, false)) return;
                Entity npc = findNPC();
                if (npc == null) {
                    state = State.CAST_ROD;
                    timer.reset();
                    return;
                }
                state = State.LOOK_NPC;
                timer.reset();
                break;
            }

            case LOOK_NPC: {
                if (!timer.reached(200, false)) return;
                Entity npc = findNPC();
                if (npc == null) { state = State.CAST_ROD; timer.reset(); return; }
                double dx = npc.getX() - mc.player.getX();
                double dy = (npc.getY() + npc.getHeight() / 2.0)
                          - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
                double dz = npc.getZ() - mc.player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                float yawToNpc = (float) Math.toDegrees(Math.atan2(-dx, dz));
                mc.player.setYaw(yawToNpc);
                mc.player.setPitch((float) -Math.toDegrees(Math.atan2(dy, dist)));
                fishingYaw = yawToNpc + 180f;
                state = State.INTERACT_NPC;
                timer.reset();
                break;
            }

            case INTERACT_NPC: {
                if (!timer.reached(300, false)) return;
                Entity npc = findNPC();
                if (npc == null) { state = State.CAST_ROD; timer.reset(); return; }
                mc.options.useKey.setPressed(true);
                state = State.PRESS_USE;
                timer.reset();
                break;
            }

            case PRESS_USE: {
                mc.options.useKey.setPressed(false);
                state = State.WAIT_SHOP;
                timer.reset();
                break;
            }

            case WAIT_SHOP: {
                if (mc.currentScreen != null) {
                    state = State.SELL_ITEMS;
                    timer.reset();
                    return;
                }
                if (timer.reached(4000, false)) {
                    state = State.CAST_ROD;
                    timer.reset();
                }
                break;
            }

            case SELL_ITEMS: {
                if (!timer.reached(delay, false)) return;
                if (mc.currentScreen == null) { state = State.CAST_ROD; timer.reset(); return; }

                int slot = findSellableSlot();
                if (slot == -1) {
                    state = State.CLOSE_SHOP;
                    timer.reset();
                    return;
                }
                var handler = mc.player.currentScreenHandler;
                int containerSize = handler.slots.size() - 36;
                int screenSlot = invSlotToScreen(slot, containerSize);
                mc.interactionManager.clickSlot(handler.syncId, screenSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                timer.reset();
                break;
            }

            case CLOSE_SHOP: {
                if (!timer.reached(delay, false)) return;
                if (mc.currentScreen != null) mc.currentScreen.close();
                state = State.WAIT_CLOSE;
                timer.reset();
                break;
            }

            case WAIT_CLOSE: {
                if (mc.currentScreen != null) {
                    if (timer.reached(500, false)) { mc.currentScreen.close(); timer.reset(); }
                    return;
                }
                if (!timer.reached(300, false)) return;
                mc.player.setYaw(fishingYaw);
                mc.player.setPitch(50f);
                state = State.CAST_ROD;
                timer.reset();
                break;
            }
        }
    };
}
