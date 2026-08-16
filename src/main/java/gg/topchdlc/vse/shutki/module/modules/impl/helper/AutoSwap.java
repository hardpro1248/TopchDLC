package gg.topchdlc.vse.shutki.module.modules.impl.helper;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventClickSlot;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.autoswap.TripleSwapMenuWidget;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.autoswap.TripleSwapScreen;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.utils.player.swap.SwapUtility;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class AutoSwap extends Module {
    public static final AutoSwap INSTANCE = new AutoSwap();

    public enum SwapMode { Default, Triple }
    public enum ItemType { Totem, EnchantedTotem, Shield, Head, GoldenApple }
    public enum BypassMode { Matrix, GrimPacket, Universial }

    public enum ExtraItemType {
        KarateTalisman("Талисман Карателя"),
        RageTalisman("Талисман Ярости"),
        CerberusSphere("Сфера Цербера");

        private final String name;
        ExtraItemType(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public EnumSetting<SwapMode> mode    = enumSetting("Режим", SwapMode.Default);
    public KeybindSetting        swapKey = keybindSetting("Бинд", -1);
    public EnumSetting<BypassMode> bypass = enumSetting("Обход", BypassMode.Matrix);
    private Group defswap = group("DefItem");
    public EnumSetting<ItemType> item1 = defswap.enumSetting("Предмет 1", ItemType.Totem)
            .visible(() -> mode.is(SwapMode.Default));
    public EnumSetting<ItemType> item2 = defswap.enumSetting("Предмет 2", ItemType.Head)
            .visible(() -> mode.is(SwapMode.Default));

    public CheckBox extraSwap = checkbox("Доп Свап", false).visible(()->mode.is(SwapMode.Default));
    public KeybindSetting extraSwapKey = keybindSetting("Бинд доп свапа", -1)
            .visible(() -> extraSwap.get());
    public EnumSetting<ExtraItemType> extraItem = enumSetting("Доп Предмет", ExtraItemType.KarateTalisman)
            .visible(() -> extraSwap.get());
    public EnumSetting<ItemType> extraReturnItem = enumSetting("Возвратный предмет", ItemType.Totem)
            .visible(() -> extraSwap.get());



    private final ItemStack[] tripleSlots = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
    private final TripleSwapMenuWidget menuWidget = new TripleSwapMenuWidget();
    private TripleSwapScreen tripleScreen;
    private boolean menuOpen = false;
    int pendingPickSlot = -1;

    private AutoSwap() {
        super("Auto Swap", Category.PLAYER, "Переключает предметы в левой руке");
    }

    public boolean isMenuOpen()              { return menuOpen; }
    public int getPendingPickSlot()          { return pendingPickSlot; }
    public ItemStack getTripleSlot(int i)    { return (i < 0 || i >= 3 || tripleSlots[i] == null) ? ItemStack.EMPTY : tripleSlots[i]; }
    public void setTripleSlot(int i, ItemStack s) { if (i >= 0 && i < 3) tripleSlots[i] = s == null ? ItemStack.EMPTY : s; }

    EventBus<Event> events = event -> {
        if (mc.player == null || mc.world == null) return;

        if (event instanceof EventGameTick) {
            if (pendingPickSlot >= 0 && !(mc.currentScreen instanceof InventoryScreen)) {
                pendingPickSlot = -1;
            }

            if (menuOpen && mode.is(SwapMode.Triple)) {
                int bind = swapKey.getBind();
                if (bind != -1 && pendingPickSlot == -1) {
                    boolean held = isBindHeld(bind);
                    if (!held) closeMenu();
                }
                return;
            }
        }

        if (event instanceof EventKey e) {
            if (extraSwap.get() && e.action == 1 && mode.is(SwapMode.Default)) {
                int extraBind = extraSwapKey.getBind();
                if (extraBind != -1 && e.key == extraBind) {
                    performExtraSwap();
                    return;
                }
            }

            int bind = swapKey.getBind();
            if (bind == -1 || e.key != bind) return;

            if (mode.is(SwapMode.Default)) {
                if (e.action == 1) performDefaultSwap();
            } else if (mode.is(SwapMode.Triple)) {
                if (e.action == 1 && !menuOpen && pendingPickSlot == -1 && mc.currentScreen == null) {
                    openMenu();
                } else if (e.action == 0 && menuOpen && pendingPickSlot == -1) {
                    closeMenu();
                }
            }
            return;
        }

        if (event instanceof EventClickSlot e && pendingPickSlot >= 0) {
            if (mc.currentScreen instanceof InventoryScreen
                    && e.actionType == SlotActionType.PICKUP
                    && e.button == 0
                    && EventClickSlot.postedFromScreen) {
                int slot = e.slotId;
                if (slot >= 5 && slot <= 45) {
                    ItemStack s = mc.player.currentScreenHandler.getSlot(slot).getStack();
                    if (!s.isEmpty()) {
                        setTripleSlot(pendingPickSlot, s.copy());
                    }
                    pendingPickSlot = -1;
                    e.cancel();

                    if (mc.player.currentScreenHandler != null) {
                        mc.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                    }

                    mc.execute(() -> {
                        if (mc.player != null && mc.player.currentScreenHandler != null) {
                            mc.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                        }
                        mc.setScreen(null);
                    });
                }
            }
        }
    };

    private boolean isBindHeld(int bind) {
        long handle = mc.getWindow().getHandle();
        if (bind <= -100) {
            int mouseButton = -100 - bind;
            return GLFW.glfwGetMouseButton(handle, mouseButton) == GLFW.GLFW_PRESS;
        }
        return InputUtil.isKeyPressed(window, bind);
    }

    public void openMenu() {
        if (pendingPickSlot >= 0) return;
        menuOpen = true;
        if (tripleScreen == null) {
            tripleScreen = new TripleSwapScreen(menuWidget);
        }
        mc.execute(() -> {
            if (mc.player != null) {
                mc.setScreen(tripleScreen);
            }
        });
    }

    public void closeMenu() {
        menuOpen = false;
        pendingPickSlot = -1;
        if (mc.currentScreen instanceof TripleSwapScreen) {
            mc.execute(() -> mc.setScreen(null));
        }
    }

    public void openInventoryForSlot(int slotIdx) {
        this.pendingPickSlot = slotIdx;
        this.menuOpen = false;

        mc.execute(() -> {
            if (mc.player != null) {
                mc.setScreen(new InventoryScreen(mc.player));
            }
        });
    }

    public void performTripleSwap(int slot) {
        if (mc.player == null) return;

        if (bypass.is(BypassMode.GrimPacket)) {
            SwapUtility.GrimPacketSwapOffHand(slot);
        } else if (bypass.is(BypassMode.Matrix)) {
            SwapUtility.matrixswaptooffhend(slot);
        } else {
            SwapUtility.safeSwapToOffhand(slot);
        }
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        pendingPickSlot = -1;
        if (menuOpen) closeMenu();
    }

    private void performDefaultSwap() {
        if (mc.player == null || mc.interactionManager == null) return;

        ItemType target = getTargetType();
        if (target == null) return;

        int slot = findItem(target);
        if (slot == -1) return;

        performTripleSwap(slot);

        int inv = slot >= 36 ? slot - 36 : slot;
        ItemStack ns = mc.player.getInventory().getStack(inv);
        showNotification(target, ns.isEmpty() ? ItemStack.EMPTY : ns);
    }

    private void performExtraSwap() {
        if (mc.player == null) return;
        ExtraItemType selectedExtra = extraItem.get();
        ItemStack offhand = mc.player.getOffHandStack();
        boolean isExtraInOffhand = switch (selectedExtra) {
            case KarateTalisman -> isKarateTalisman(offhand);
            case RageTalisman   -> isRageTalisman(offhand);
            case CerberusSphere -> isCerberusSphere(offhand);
        };

        if (!isExtraInOffhand) {
            int slot = findExtraItem(selectedExtra);
            if (slot == -1) return;

            performTripleSwap(slot);

            int inv = slot >= 36 ? slot - 36 : slot;
            ItemStack stack = mc.player.getInventory().getStack(inv);

            Text msg = Text.of("AutoSwap: ").copy().append(Text.of(selectedExtra.getName()).copy().withColor(0xFF5555));
            if (!stack.isEmpty()) {
                Client.NOTIFIES.addItem(msg, stack, 3000);
            } else {
                Client.NOTIFIES.add(msg, null, 3000);
            }
        } else {
            ItemType returnTarget = extraReturnItem.get();
            int slot = findItem(returnTarget);
            if (slot == -1) return;

            performTripleSwap(slot);

            int inv = slot >= 36 ? slot - 36 : slot;
            ItemStack ns = mc.player.getInventory().getStack(inv);
            showNotification(returnTarget, ns.isEmpty() ? ItemStack.EMPTY : ns);
        }
    }

    private int findExtraItem(ExtraItemType type) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            boolean match = switch (type) {
                case KarateTalisman -> isKarateTalisman(stack);
                case RageTalisman   -> isRageTalisman(stack);
                case CerberusSphere -> isCerberusSphere(stack);
            };

            if (match) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    private boolean isKarateTalisman(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String name = stack.getName().getString().toLowerCase();
        if (name.contains("карател") || name.contains("punisher")) {
            return true;
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.contains("AttributeModifiers")) {
                NbtList attributeList = nbt.getList("AttributeModifiers").orElse(null);
                if (attributeList != null && !attributeList.isEmpty()) {
                    boolean hasDamage = false;
                    boolean hasSpeed = false;
                    boolean hasHealth = false;

                    for (int i = 0; i < attributeList.size(); i++) {
                        NbtCompound attrNbt = attributeList.getCompound(i).orElse(null);
                        if (attrNbt == null) continue;

                        String attrName = attrNbt.getString("AttributeName").orElse("");
                        double amount = attrNbt.getDouble("Amount").orElse(0.0);
                        String slot = attrNbt.getString("Slot").orElse("");

                        if ("offhand".equalsIgnoreCase(slot)) {
                            if ("minecraft:generic.attack_damage".equals(attrName) && Math.abs(amount - 7.0) < 0.001) {
                                hasDamage = true;
                            }
                            if ("minecraft:generic.movement_speed".equals(attrName) && Math.abs(amount - 0.10) < 0.001) {
                                hasSpeed = true;
                            }
                            if ("minecraft:generic.max_health".equals(attrName) && Math.abs(amount - (-4.0)) < 0.001) {
                                hasHealth = true;
                            }
                        }
                    }

                    if (hasDamage && hasSpeed && hasHealth) return true;
                }
            }
        }

        return false;
    }
    private boolean isRageTalisman(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String name = stack.getName().getString().toLowerCase();
        if (name.contains("ярост") || name.contains("rage")) {
            return true;
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            if (nbt.contains("AttributeModifiers")) {
                NbtList attributeList = nbt.getList("AttributeModifiers").orElse(null);
                if (attributeList != null && !attributeList.isEmpty()) {
                    boolean hasDamage = false;
                    boolean hasHealth = false;

                    for (int i = 0; i < attributeList.size(); i++) {
                        NbtCompound attrNbt = attributeList.getCompound(i).orElse(null);
                        if (attrNbt == null) continue;

                        String attrName = attrNbt.getString("AttributeName").orElse("");
                        double amount = attrNbt.getDouble("Amount").orElse(0.0);
                        String slot = attrNbt.getString("Slot").orElse("");

                        if ("offhand".equalsIgnoreCase(slot)) {
                            if ("minecraft:generic.attack_damage".equals(attrName) && Math.abs(amount - 5.0) < 0.001) {
                                hasDamage = true;
                            }
                            if ("minecraft:generic.max_health".equals(attrName) && Math.abs(amount - (-4.0)) < 0.001) {
                                hasHealth = true;
                            }
                        }
                    }

                    if (hasDamage && hasHealth) return true;
                }
            }
        }

        return false;
    }

    private boolean isCerberusSphere(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        if (stack.getItem() != Items.PLAYER_HEAD) return false;

        String name = stack.getName().getString();
        if (name.contains("Сфера Цербера") || name.contains("Цербер") || name.contains("Cerber")) {
            return true;
        }
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            String nbtStr = customData.copyNbt().toString().toLowerCase();
            if (nbtStr.contains("cerber") || nbtStr.contains("цербер") || (nbtStr.contains("hms-damage") && nbtStr.contains("hms-rush"))) {
                return true;
            }
        }

        return false;
    }

    private void showNotification(ItemType type, ItemStack stack) {
        String name = stack.isEmpty() ? switch (type) {
            case Totem -> "Тотем"; case EnchantedTotem -> "Чар Тотем";
            case Shield -> "Щит"; case Head -> "Голова"; case GoldenApple -> "Золотое яблоко";
        } : stack.getName().getString();
        int color = switch (type) {
            case Totem, EnchantedTotem -> 0xFF5555; case Head -> 0xFFAA00;
            case Shield -> 0xAAAAAA; case GoldenApple -> 0xFFD700;
        };
        Text msg = Text.of("AutoSwap: ").copy().append(Text.of(name).copy().withColor(color));
        if (!stack.isEmpty()) Client.NOTIFIES.addItem(msg, stack, 3000);
        else Client.NOTIFIES.add(msg, null, 3000);
    }

    private ItemType getTargetType() {
        ItemType p = item1.get(), s = item2.get();
        return matchesItemType(mc.player.getOffHandStack(), p) ? s : p;
    }

    private int findItem(ItemType t) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            if (matchesItemType(mc.player.getInventory().getStack(i), t))
                return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    private boolean matchesItemType(ItemStack s, ItemType t) {
        if (s.isEmpty()) return false;
        return switch (t) {
            case Totem          -> s.getItem() == Items.TOTEM_OF_UNDYING;
            case EnchantedTotem -> s.getItem() == Items.TOTEM_OF_UNDYING && s.hasEnchantments();
            case Shield         -> s.getItem() == Items.SHIELD;
            case Head           -> s.getItem() == Items.PLAYER_HEAD;
            case GoldenApple    -> s.getItem() == Items.GOLDEN_APPLE;
        };
    }
}