package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class AutoPotion extends Module {
    public static final AutoPotion INSTANCE = new AutoPotion();

    private final MultiEnumSetting<Baff> potion = multiEnumSetting("Potion", Baff.Strength);
    private final CheckBox heal = checkbox("Heal", false);
    private final SliderSetting healhp = sliderSetting("hp to throw", 10, 1, 20).increment(0.5f);
    private final SliderSetting delay = sliderSetting("Next Batch Delay", 1000, 100, 3000).increment(100);

    private long lastThrowTime = 0;

    private enum Baff {
        Strength(StatusEffects.STRENGTH),
        Speed(StatusEffects.SPEED),
        Fire_Resistance(StatusEffects.FIRE_RESISTANCE);

        final RegistryEntry<StatusEffect> effect;
        Baff(RegistryEntry<StatusEffect> effect) { this.effect = effect; }
    }

    private AutoPotion() {
        super("AutoPotion", Category.COMBAT, "xxx");
    }

    public EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick e) onTick(e);
    };

    private void onTick(EventGameTick event) {
        if (mc.world == null || mc.player == null) return;

        if (System.currentTimeMillis() - lastThrowTime < delay.get()) return;

        List<Integer> slotsToThrow = new ArrayList<>();

        if (heal.get() && mc.player.getHealth() <= healhp.get()) {
            int slot = findPotionSlot(StatusEffects.INSTANT_HEALTH);
            if (slot != -1) {
                slotsToThrow.add(slot);
            }
        }

        for (Baff b : Baff.values()) {
            if (potion.get(b) && !mc.player.hasStatusEffect(b.effect)) {
                int slot = findPotionSlot(b.effect);
                if (slot != -1 && !slotsToThrow.contains(slot)) {
                    slotsToThrow.add(slot);
                }
            }
        }

        if (!slotsToThrow.isEmpty()) {
            performBatchThrow(slotsToThrow);
            lastThrowTime = System.currentTimeMillis();
        }
    }

    private void performBatchThrow(List<Integer> slots) {
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                currentYaw, 90.0f, mc.player.isOnGround(), mc.player.horizontalCollision
        ));

        for (int slot : slots) {
            int throwSlot = slot;
            int usedHotbar = -1;

            if (slot >= 9) {
                usedHotbar = findFreeHotbarSlot();
                if (usedHotbar == -1) continue;
                swapSlots(slot, usedHotbar);
                throwSlot = usedHotbar;
            }

            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(throwSlot));

            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                    Hand.MAIN_HAND,
                    0,
                    currentYaw,
                    90.0f
            ));

            if (usedHotbar != -1) {
                swapSlots(slot, usedHotbar);
            }
        }

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                currentYaw, currentPitch, mc.player.isOnGround(), mc.player.horizontalCollision
        ));
    }

    private void swapSlots(int fromSlot, int toSlot) {
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                fromSlot,
                toSlot,
                net.minecraft.screen.slot.SlotActionType.SWAP,
                mc.player
        );
    }

    private int findFreeHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int findPotionSlot(RegistryEntry<StatusEffect> effect) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.SPLASH_POTION)) {
                PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
                if (contents != null) {
                    for (StatusEffectInstance inst : contents.getEffects()) {
                        if (inst.getEffectType().equals(effect)) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }
}