package gg.topchdlc.vse.shutki.module.modules.impl.helper;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.*;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.mixin.accessor.IItemCooldownEntry;
import gg.topchdlc.mixin.accessor.IItemCooldownManager;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.Tag;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.binder.BinderSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.swap.FastSwapUtil;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;
import gg.topchdlc.vse.utils.player.swap.LegitItemUseUtil;
import gg.topchdlc.vse.utils.player.swap.SwapUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AssistModule extends Module {
    public static final AssistModule INSTANCE = new AssistModule();
    private static final List<Item> EXCLUDED_ITEMS = Arrays.asList(Items.SHIELD, Items.GOAT_HORN);

    private final LegitItemUseUtil legitItemUse = new LegitItemUseUtil();
    private final TimeUtility cbtimer = new TimeUtility();

    public enum UseMode { Grim("Grim"), Matrix("Matrix"), Vanilla("Vanilla");
        private final String name; UseMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    public enum ServerMode { Binder("Binder"), FunTime("FunTime"), HollyWorld("HollyWorld");
        private final String name; ServerMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private final Group helper = group("OtherItem");
    private CheckBox wind = helper.checkbox("Wind", false);
    private final KeybindSetting bindwd = helper.keybindSetting("WindBind", -1).visible(wind::get);
    private final CheckBox windback = helper.checkbox("WindBack", true).visible(wind::get);
    private final CheckBox crossBow = helper.checkbox("CrossBow", true);
    private final KeybindSetting bindcs = helper.keybindSetting("CrosBowBind", -1).visible(crossBow::get);
    private final CheckBox cbswapBack = helper.checkbox("CBSwapBack", true).visible(crossBow::get);

    private AssistModule() { super("Assist/Binder Item", Category.Misc, "помощник с некоторыми вещами", Tag.Sosiski); }

    Group items = group("Binder");
    EnumSetting<UseMode> useMode = items.enumSetting("Use Mode", UseMode.Grim);
    EnumSetting<ServerMode> serverMode = items.enumSetting("Server", ServerMode.FunTime);
    BinderSetting binder = items.binderSetting("Item bind", Map.of(Items.ENDER_PEARL, -1, Items.ENDER_EYE, -1))
            .visible(() -> serverMode.is(ServerMode.Binder));

    KeybindSetting ftdisorient   = items.keybindSetting("Дезориентация", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftyavnyapil  = items.keybindSetting("Явная пыль", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftplast       = items.keybindSetting("Пласт", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftsnow        = items.keybindSetting("Снежок", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftbozhka      = items.keybindSetting("Божья Аура ", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting fttrap        = items.keybindSetting("Трапка", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftogni_s      = items.keybindSetting("Огненый смерч", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftPotionHoly  = items.keybindSetting("Святая вода", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftPotionAnger = items.keybindSetting("Зелье Гнева", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftPotionPaladin= items.keybindSetting("Зелье Палладина", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftPotionAssasin= items.keybindSetting("Зелье Ассасина", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftPotionRadio = items.keybindSetting("Зелье Радиации", -1).visible(() -> serverMode.is(ServerMode.FunTime));
    KeybindSetting ftPotionSleep = items.keybindSetting("Снотворное", -1).visible(() -> serverMode.is(ServerMode.FunTime));

    KeybindSetting hwsneg  = items.keybindSetting("СнежокК", -1).visible(() -> serverMode.is(ServerMode.HollyWorld));
    KeybindSetting hwTrapo = items.keybindSetting("ОТрапка", -1).visible(() -> serverMode.is(ServerMode.HollyWorld));
    KeybindSetting hwTrap  = items.keybindSetting("Взрывная трапка", -1).visible(() -> serverMode.is(ServerMode.HollyWorld));
    KeybindSetting hwstan  = items.keybindSetting("Стан", -1).visible(() -> serverMode.is(ServerMode.HollyWorld));
    KeybindSetting hwvzriv = items.keybindSetting("Взрывная штучка", -1).visible(() -> serverMode.is(ServerMode.HollyWorld));

    Group friends = group("Friends");
    KeybindSetting addFriendBind = friends.keybindSetting("Add friend bind", -1);

    Group cancelActions = group("Other Help");
    public MultiEnumSetting<CancelMode> cancelSettings = cancelActions.multiEnumSetting("Settings", CancelMode.class);

    @AllArgsConstructor @Getter
    public enum CancelMode implements EnumChoice {
        FriendDamage("Damage for Friends", true),
        ServerRotate("No Server Rotate", false),
        ServerResourcePack("Server Resource Pack", false),
        AutoRespawn("Auto respawn", false),
        NoFluidSlowdown("No slowdown breaking in liquids", false),
        NoBallPlace("NoballPlace", false);

        final String renderName;
        final boolean defaultEnabled;
    }

    private float getPreciseCooldown(Item item) {
        if (mc.player == null) return 0;
        IItemCooldownManager manager = (IItemCooldownManager) mc.player.getItemCooldownManager();
        Map<Object, ?> entries = manager.getEntries();
        Object entryObj = entries.get(item);
        if (entryObj == null) entryObj = entries.get(Registries.ITEM.getId(item));
        if (entryObj != null) {
            IItemCooldownEntry entry = (IItemCooldownEntry) entryObj;
            return Math.max(0, (entry.getEndTick() - manager.getTick()) / 20.0f);
        }
        return 0;
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventKey e) {
            if (e.action != 1 || mc.player == null) return;
            int friendKey = addFriendBind.getBind();
            if (friendKey != -1 && e.key == friendKey) {
                if (!(mc.targetedEntity instanceof PlayerEntity target) || target == mc.player) {
                    Client.NOTIFIES.add(Text.of("Нет цели"), IconUse.CROSS, 1500); return;
                }
                String name = target.getGameProfile().name();
                if (Client.FRIENDS.isFriend(name)) {
                    Client.FRIENDS.removeFriend(name);
                    Client.NOTIFIES.add(Text.of("Удален из друзей: ").copy().append(target.getName().copy()), IconUse.REMOVEFRIEND, 2000);
                } else {
                    Client.FRIENDS.addFriend(target);
                    Client.NOTIFIES.add(Text.of("Добавлен в друзья: ").copy().append(target.getName().copy()), IconUse.ADDFRIEND, 2000);
                }
                return;
            }

            if (serverMode.is(ServerMode.FunTime)) {
                if (isFtPotionKey(e.key)) { resolveFtPotion(e.key); return; }
                Item ftItem = resolveFtKey(e.key);
                if (ftItem != null) { useServerItem(ftItem, resolveFtKeyName(e.key)); return; }
            }

            if (crossBow.get() && e.key == bindcs.getBind() && cbtimer.reached(300)) {
                int cbSlot = InventoryUtility.find(Items.CROSSBOW);
                if (cbSlot != -1) {
                    if (cbSlot < 9) {
                        int oldSlot = mc.player.getInventory().getSelectedSlot();
                        mc.player.getInventory().setSelectedSlot(cbSlot);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        if (cbswapBack.get()) {
                            mc.player.getInventory().setSelectedSlot(oldSlot);
                        }
                        cbtimer.reset();
                    } else {
                        legitItemUse.startLegitUse(cbSlot);
                        cbtimer.reset();
                    }
                }
            }

            if (wind.get() && e.key == bindwd.getBind()) {
                int wdSlot = InventoryUtility.find(Items.WIND_CHARGE);
                if (wdSlot != -1) {
                    if (wdSlot < 9) {
                        int oldSlot = mc.player.getInventory().getSelectedSlot();
                        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(wdSlot));
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        if (windback.get()) {
                            NetworkUtility.send(new UpdateSelectedSlotC2SPacket(oldSlot));
                        }
                    } else {
                        legitItemUse.startLegitUse(wdSlot);
                    }
                }
            }

            if (serverMode.is(ServerMode.HollyWorld)) {
                Item hwItem = resolveHwKey(e.key);
                if (hwItem != null) { useServerItem(hwItem, resolveHwKeyName(e.key)); return; }
            }

            if (serverMode.is(ServerMode.Binder)) {
                for (Map.Entry<Identifier, Integer> entry : binder.getEntry()) {
                    Item item = Registries.ITEM.get(entry.getKey());
                    if (entry.getValue() == -1 || entry.getValue() != e.key) continue;
                    useServerItem(item);
                }
            }
        }

        if (event instanceof EventGameTick) {
            legitItemUse.onTick();
        }

        if (event instanceof EventAttack eventAttack && eventAttack.target instanceof PlayerEntity target) {
            if (mc.player != null && Client.FRIENDS.isFriend(target.getGameProfile().name()) && cancelSettings.get(CancelMode.FriendDamage)) {
                eventAttack.cancel();
            }
        }

        if (event instanceof EventReceivePacket eventReceivePacket && cancelSettings.get(CancelMode.ServerResourcePack)) {
            if (eventReceivePacket.getPacket() instanceof ResourcePackStatusC2SPacket packet) {
                event.cancel();
                mc.getNetworkHandler().sendPacket(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
                mc.getNetworkHandler().sendPacket(new ResourcePackStatusC2SPacket(packet.id(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            }
        }

        if (event instanceof EventSetRotation e && cancelSettings.get(CancelMode.ServerRotate)) e.cancel();
        if (cancelSettings.get(CancelMode.AutoRespawn) && mc.currentScreen instanceof DeathScreen) {
            NetworkUtility.sendWithoutEvent(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
        }
    };

    public List<Object[]> getBindEntries() {
        List<Object[]> result = new ArrayList<>();
        if (serverMode.is(ServerMode.FunTime)) {
            addIfBound(result, Items.ENDER_EYE, ftdisorient); addIfBound(result, Items.SUGAR, ftyavnyapil);
            addIfBound(result, Items.DRIED_KELP, ftplast); addIfBound(result, Items.SNOWBALL, ftsnow);
            addIfBound(result, Items.PHANTOM_MEMBRANE, ftbozhka); addIfBound(result, Items.NETHERITE_SCRAP, fttrap);
            addIfBound(result, Items.FIRE_CHARGE, ftogni_s); addIfBound(result, Items.SPLASH_POTION, ftPotionHoly);
            addIfBound(result, Items.SPLASH_POTION, ftPotionAnger); addIfBound(result, Items.SPLASH_POTION, ftPotionPaladin);
            addIfBound(result, Items.SPLASH_POTION, ftPotionAssasin); addIfBound(result, Items.SPLASH_POTION, ftPotionRadio);
            addIfBound(result, Items.SPLASH_POTION, ftPotionSleep);
        } else if (serverMode.is(ServerMode.HollyWorld)) {
            addIfBound(result, Items.PRISMARINE_SHARD, hwTrap); addIfBound(result, Items.NETHER_STAR, hwstan);
            addIfBound(result, Items.FIRE_CHARGE, hwvzriv);
        } else if (serverMode.is(ServerMode.Binder)) {
            for (Map.Entry<Identifier, Integer> entry : binder.getEntry()) {
                if (entry.getValue() != -1) result.add(new Object[]{Registries.ITEM.get(entry.getKey()), entry.getValue()});
            }
        }
        return result;
    }

    private void addIfBound(List<Object[]> list, Item item, KeybindSetting bind) {
        if (bind.getBind() != -1) list.add(new Object[]{item, bind.getBind()});
    }

    private Item resolveFtKey(int key) {
        if (ftdisorient.getBind() == key) return Items.ENDER_EYE; if (ftyavnyapil.getBind() == key) return Items.SUGAR;
        if (ftplast.getBind() == key) return Items.DRIED_KELP; if (ftsnow.getBind() == key) return Items.SNOWBALL;
        if (ftbozhka.getBind() == key) return Items.PHANTOM_MEMBRANE; if (fttrap.getBind() == key) return Items.NETHERITE_SCRAP;
        if (ftogni_s.getBind() == key) return Items.FIRE_CHARGE; return null;
    }

    private String resolveFtKeyName(int key) {
        if (ftdisorient.getBind() == key) return "Дезориентация"; if (ftyavnyapil.getBind() == key) return "Явная пыль";
        if (ftplast.getBind() == key) return "Пласт"; if (ftsnow.getBind() == key) return "Снежок";
        if (ftbozhka.getBind() == key) return "Божья Аура"; if (fttrap.getBind() == key) return "Трапка";
        if (ftogni_s.getBind() == key) return "Огненый смерч"; return null;
    }

    private String resolveHwKeyName(int key) {
        if (hwTrap.getBind() == key) return "Взрывная трапка"; if (hwstan.getBind() == key) return "Стан"; if (hwTrapo.getBind() == key) return "ОТрапка"; if (hwsneg.getBind() == key) return "СнежокК";
        if (hwvzriv.getBind() == key) return "Взрывная штучка"; return null;
    }

    private int findPotionByEffects(String... requiredEffectIds) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 44; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || stack.getItem() != Items.SPLASH_POTION) continue;
            net.minecraft.component.type.PotionContentsComponent contents = stack.get(net.minecraft.component.DataComponentTypes.POTION_CONTENTS);
            if (contents == null) continue;
            List<StatusEffectInstance> effects = contents.customEffects();
            if (effects.isEmpty()) continue;
            boolean allFound = true;
            for (String needed : requiredEffectIds) {
                boolean found = false;
                for (StatusEffectInstance eff : effects) {
                    String id = eff.getEffectType().getKey().get().getValue().toString();
                    if (id.equals(needed) || id.equals(needed.replace("minecraft:", ""))) { found = true; break; }
                }
                if (!found) { allFound = false; break; }
            }
            if (allFound) return i;
        }
        return -1;
    }

    private void resolveFtPotion(int key) {
        int slot = -1; String name = null;
        if (ftPotionHoly.getBind() == key) { slot = findPotionByEffects("minecraft:regeneration", "minecraft:invisibility"); name = "Святая вода"; }
        else if (ftPotionAnger.getBind() == key) { slot = findPotionByEffects("minecraft:strength", "minecraft:slowness"); name = "Зелье Гнева"; }
        else if (ftPotionPaladin.getBind() == key) { slot = findPotionByEffects("minecraft:resistance", "minecraft:fire_resistance"); name = "Зелье Палладина"; }
        else if (ftPotionAssasin.getBind() == key) { slot = findPotionByEffects("minecraft:strength", "minecraft:speed"); name = "Зелье Ассасина"; }
        else if (ftPotionRadio.getBind() == key) { slot = findPotionByEffects("minecraft:poison", "minecraft:wither"); name = "Зелье Радиации"; }
        else if (ftPotionSleep.getBind() == key) { slot = findPotionByEffects("minecraft:weakness", "minecraft:blindness"); name = "Снотворное"; }
        if (slot == -1 && name != null) { Client.NOTIFIES.add(Text.of("Нет: ").copy().append(Text.of(name).copy().withColor(ClientColors.RED.getRGB())), IconUse.CROSS, 2000); return; }
        if (slot != -1) useServerItem(mc.player.getInventory().getStack(slot).getItem(), name);
    }

    private boolean isFtPotionKey(int key) {
        return key != -1 && (ftPotionHoly.getBind() == key || ftPotionAnger.getBind() == key || ftPotionPaladin.getBind() == key || ftPotionAssasin.getBind() == key || ftPotionRadio.getBind() == key || ftPotionSleep.getBind() == key);
    }

    private Item resolveHwKey(int key) {
        if (hwTrap.getBind() == key) return Items.PRISMARINE_SHARD; if (hwstan.getBind() == key) return Items.NETHER_STAR; if (hwTrapo.getBind() == key) return Items.POPPED_CHORUS_FRUIT; if (hwsneg.getBind() == key) return Items.SNOWBALL;
        if (hwvzriv.getBind() == key) return Items.FIRE_CHARGE; return null;
    }

    public void useServerItem(Item item) { useServerItem(item, null); }

    public void useServerItem(Item item, String displayName) {
        if (mc.player == null) return;
        int slot = InventoryUtility.find(item);
        if (slot == -1) { Client.NOTIFIES.add(Text.of("Нет предмета ").copy().append(item.getName().copy().withColor(ClientColors.RED.getRGB())), IconUse.CROSS, 2000); return; }
        float seconds = getPreciseCooldown(item);
        if (seconds > 0 && !EXCLUDED_ITEMS.contains(item)) {
            String timeStr = seconds < 1f ? String.format("%.1fс", seconds) : String.format("%.0fс", seconds);
            Client.NOTIFIES.add(Text.of("КД: ").copy().append(item.getName().copy().withColor(ClientColors.MAIN_COLOR.getRGB())).append(Text.of(" ещё " + timeStr).copy().withColor(ClientColors.RED.getRGB())), IconUse.WARN, 1500);
            return;
        }

        if (useMode.is(UseMode.Grim)) {
            if (!legitItemUse.isBusy()) legitItemUse.startLegitUse(slot);
        }
        else if (useMode.is(UseMode.Matrix)) {
            if (!FastSwapUtil.isBusy()) useTestMode(slot);
        }
        else if (useMode.is(UseMode.Vanilla)) {
            useVanilla(slot);
        }

        String label = displayName != null ? displayName : item.getName().getString();
        Client.NOTIFIES.addItem(Text.of(label).copy().withColor(0xEEEEEE).append(Text.of(" использован").copy().withColor(0xAAAAAA)), mc.player.getInventory().getStack(slot), 2000);
    }

    private void useTestMode(int slot) { FastSwapUtil.silentUseMatrix(mc.player.getInventory().getStack(slot).getItem()); }
    private void useVanilla(int slot) { SwapUtility.vanilaswap(slot); }

    @Override
    protected void onDisable() {
        super.onDisable();
        legitItemUse.reset();
        FastSwapUtil.reset();
    }
}