package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.screen.screens.ingame.RadialMenuScreen;
import gg.topchdlc.vse.shutki.screen.screens.ingame.RadialMenuWidget;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Скрытая встроенная функция (не отключается и не видна в меню модулей):
 * - зажать клавишу меню предметов -> круговое меню слотов хотбара, отпустить чтобы взять в руку;
 * - зажать G (клавиша зелий) -> круговое меню зелий из инвентаря, отпустить чтобы кинуть не беря в руку.
 */
public class RadialMenu extends Module {
    public static final RadialMenu INSTANCE = new RadialMenu();

    private final RadialMenuWidget potionWidget = new RadialMenuWidget();
    private RadialMenuScreen potionScreen = null;
    private boolean potionOpen = false;

    private final RadialMenuWidget itemWidget = new RadialMenuWidget();
    private RadialMenuScreen itemScreen = null;
    private boolean itemOpen = false;

    private final List<ItemStack> potionStacks = new ArrayList<>();
    private final List<String> potionLabels = new ArrayList<>();
    private final List<Integer> potionSlots = new ArrayList<>();

    private final List<ItemStack> itemStacks = new ArrayList<>();
    private final List<String> itemLabels = new ArrayList<>();

    private RadialMenu() {
        super("RadialMenu", Category.Misc, "Скрытые радиальные меню зелий и предметов");
        setHidden(true);
        nonActivatable();
    }

    public void onEvent(Event event) {
        if (event instanceof EventKey e) onKey(e);
        else if (event instanceof EventGameTick) onTick();
    }

    private void onKey(EventKey e) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        int itemKey = ClientSettings.INSTANCE.radialItemKey.getBind();
        int potKey = ClientSettings.INSTANCE.radialPotionKey.getBind();

        if (potKey != -1 && e.getKey() == potKey) {
            if (e.getAction() == 1 && mc.currentScreen == null && !potionOpen && !itemOpen) {
                openPotion();
            } else if (e.getAction() == 0 && potionOpen) {
                potionWidget.triggerRelease();
                closePotion();
            }
            return;
        }
        if (itemKey != -1 && e.getKey() == itemKey) {
            if (e.getAction() == 1 && mc.currentScreen == null && !itemOpen && !potionOpen) {
                openItem();
            } else if (e.getAction() == 0 && itemOpen) {
                itemWidget.triggerRelease();
                closeItem();
            }
        }
    }

    private void onTick() {
        if (potionOpen) {
            if (mc.currentScreen != potionScreen) {
                potionOpen = false;
                potionScreen = null;
                return;
            }
            if (!isBindHeld(ClientSettings.INSTANCE.radialPotionKey.getBind())) {
                potionWidget.triggerRelease();
                closePotion();
            }
        }
        if (itemOpen) {
            if (mc.currentScreen != itemScreen) {
                itemOpen = false;
                itemScreen = null;
                return;
            }
            if (!isBindHeld(ClientSettings.INSTANCE.radialItemKey.getBind())) {
                itemWidget.triggerRelease();
                closeItem();
            }
        }
    }

    private boolean isBindHeld(int bind) {
        if (bind == -1) return false;
        if (bind <= -100) {
            return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), -100 - bind) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(window, bind);
    }

    private void openPotion() {
        buildPotionEntries();
        if (potionSlots.isEmpty()) {
            return;
        }
        potionWidget.setup(potionStacks, potionLabels, idx -> throwPotion(potionSlots.get(idx)), "Отпустите клавишу, чтобы бросить");
        if (potionWidget.size() == 0) return;
        potionOpen = true;
        potionScreen = new RadialMenuScreen(potionWidget);
        final RadialMenuScreen scr = potionScreen;
        mc.execute(() -> {
            if (mc.player != null) mc.setScreen(scr);
        });
    }

    private void openItem() {
        if (mc.player == null) return;
        buildItemEntries();
        itemWidget.setup(itemStacks, itemLabels, idx -> selectItemSlot(idx), "Отпустите клавишу, чтобы взять в руку");
        itemOpen = true;
        itemScreen = new RadialMenuScreen(itemWidget);
        final RadialMenuScreen scr = itemScreen;
        mc.execute(() -> {
            if (mc.player != null) mc.setScreen(scr);
        });
    }

    private void buildPotionEntries() {
        potionStacks.clear();
        potionLabels.clear();
        potionSlots.clear();
        Set<RegistryEntry<StatusEffect>> seen = new HashSet<>();

        for (int i = 0; i < 36 && potionSlots.size() < RadialMenuWidget.MAX_SLOTS; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !stack.isOf(Items.SPLASH_POTION)) continue;
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) continue;
            RegistryEntry<StatusEffect> eff = firstEffect(contents);
            if (eff == null || !seen.add(eff)) continue;
            potionStacks.add(stack);
            potionSlots.add(i);
            potionLabels.add(effectName(eff));
        }
    }

    private void buildItemEntries() {
        itemStacks.clear();
        itemLabels.clear();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            itemStacks.add(stack == null ? ItemStack.EMPTY : stack);
            itemLabels.add("Слот " + (i + 1));
        }
    }

    private RegistryEntry<StatusEffect> firstEffect(PotionContentsComponent contents) {
        for (StatusEffectInstance inst : contents.getEffects()) {
            return inst.getEffectType();
        }
        return null;
    }

    private String effectName(RegistryEntry<StatusEffect> effect) {
        return effect.getKey().map(key -> key.getValue().getPath()).orElse("potion");
    }

    private void throwPotion(int slot) {
        if (mc.player == null || mc.player.networkHandler == null) return;

        int oldSlot = mc.player.getInventory().getSelectedSlot();
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                currentYaw, 90.0f, mc.player.isOnGround(), mc.player.horizontalCollision
        ));

        int throwSlot = slot;
        int usedHotbar = -1;
        if (slot >= 9) {
            usedHotbar = findFreeHotbarSlot();
            if (usedHotbar == -1) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
                return;
            }
            swapSlots(slot, usedHotbar);
            throwSlot = usedHotbar;
        }

        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(throwSlot));
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND, 0, currentYaw, 90.0f
        ));
        if (usedHotbar != -1) {
            swapSlots(slot, usedHotbar);
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

    private void selectItemSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.player.networkHandler != null) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private void closePotion() {
        if (!potionOpen) return;
        potionOpen = false;
        RadialMenuScreen scr = potionScreen;
        potionScreen = null;
        if (scr != null && mc.currentScreen == scr) {
            mc.execute(() -> {
                if (mc.currentScreen == scr) mc.setScreen(null);
            });
        }
    }

    private void closeItem() {
        if (!itemOpen) return;
        itemOpen = false;
        RadialMenuScreen scr = itemScreen;
        itemScreen = null;
        if (scr != null && mc.currentScreen == scr) {
            mc.execute(() -> {
                if (mc.currentScreen == scr) mc.setScreen(null);
            });
        }
    }
}