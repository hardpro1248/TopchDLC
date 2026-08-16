package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.Client;
import gg.topchdlc.api.autobuy.AutoBuyManager;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.AutoBuy;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.nottification.SpecialPotionType;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.main.NotificationElement;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Nottification extends Module {
    public static final Nottification INSTANCE = new Nottification();
    public final NotificationElement notificationElement = new NotificationElement(new Drag("Notify", () -> true).bound(5, 170, 40, 13));

    private static class PotionData {
        final ItemStack stack;
        double lastX, lastY, lastZ;
        PotionData(ItemStack stack, double x, double y, double z) {
            this.stack = stack; this.lastX = x; this.lastY = y; this.lastZ = z;
        }
    }
    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();

    private final Map<EquipmentSlot, Integer> armorWarnState = new HashMap<>();

    private Nottification() {
        super("Nottification", Category.Misc, "Уведомляет о разном");
        Client.HUD.register(notificationElement);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot()) armorWarnState.put(slot, 100);
        }
        setEnabled(true, false);
    }

    MultiEnumSetting<TrackedEvents> track = add(new MultiEnumSetting<>("События", TrackedEvents.class));
    private final CheckBox onlyDonateItems = checkbox("Только донатные предметы", false);

    EventBus<Event> events = event -> {
        if (mc.world == null || mc.player == null) return;

        if (event instanceof EventReceivePacket p) {
            if (p.packet instanceof EntityStatusS2CPacket status && track.get(TrackedEvents.TOTEM)) {
                Entity entity = status.getEntity(mc.world);
                if (entity instanceof LivingEntity living && status.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
                    ItemStack protect = living.getMainHandStack();
                    if (protect.getItem() != Items.TOTEM_OF_UNDYING) protect = living.getOffHandStack();

                    boolean enchanted = !protect.getEnchantments().isEmpty();
                    String enchantSuffix = enchanted ? " §d[Зачарован]" : " §7[Обычный]";

                    int nameColor = Client.FRIENDS.isFriend(living.getName().getString()) ? ClientColors.FRIEND_COLOR.getRGB() : Color.WHITE.getRGB();

                    Client.NOTIFIES.addItem(Text.empty()
                                    .append(Text.of(living.getName().getString()).copy().withColor(nameColor))
                                    .append(Text.of(" потерял тотем").copy())
                                    .append(Text.of(enchantSuffix).copy()),
                            "totem_of_undying", 4000);

                    Text chatMsg = Text.empty()
                            .append(Text.literal("§8[§cTotem§8] ").copy())
                            .append(Text.of(living.getName().getString()).copy().withColor(nameColor))
                            .append(Text.literal(" §7потерял тотем").copy())
                            .append(Text.literal(enchantSuffix).copy());
                    mc.inGameHud.getChatHud().addMessage(chatMsg);
                }
            }

            if (p.packet instanceof ScreenHandlerSlotUpdateS2CPacket d && track.get(TrackedEvents.ITEMPICKUP)) {
                if (d.getSyncId() == 0
                        && d.getSlot() >= 0
                        && d.getSlot() < mc.player.currentScreenHandler.slots.size()
                        && mc.player.currentScreenHandler.getSlot(d.getSlot()).getStack().isEmpty()
                        && !d.getStack().isEmpty()) {
                    if (!onlyDonateItems.get() || isDonateItem(d.getStack())) {
                        Client.NOTIFIES.add(Text.of("Подобран предмет ").copy().append(d.getStack().getFormattedName()), IconUse.INFO, 4000);
                    }
                }
            }
        }
        if (event instanceof EventGameTick) {

            if (track.get(TrackedEvents.ARMOR_DURABILITY)) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (!slot.isArmorSlot()) continue;

                    ItemStack stack = mc.player.getEquippedStack(slot);
                    if (stack.isEmpty() || !stack.isDamageable()) {
                        armorWarnState.put(slot, 100);
                        continue;
                    }

                    double durabilityPercent = ((double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) * 100.0;
                    int lastState = armorWarnState.getOrDefault(slot, 100);

                    if (durabilityPercent <= 5.0 && lastState > 5) {
                        sendArmorAlert(stack, 5);
                        armorWarnState.put(slot, 5);
                    } else if (durabilityPercent <= 10.0 && lastState > 10) {
                        sendArmorAlert(stack, 10);
                        armorWarnState.put(slot, 10);
                    } else if (durabilityPercent > 10.0) {
                        armorWarnState.put(slot, 100);
                    }
                }
            }

            if (track.get(TrackedEvents.POTION_TRACKER)) {
                Set<Integer> current = new HashSet<>();
                for (Entity entity : mc.world.getEntities()) {
                    if (!(entity instanceof PotionEntity potion)) continue;
                    int eid = potion.getId();
                    current.add(eid);
                    PotionData d = trackedPotions.get(eid);
                    if (d == null) {
                        trackedPotions.put(eid, new PotionData(potion.getStack().copy(), potion.getX(), potion.getY(), potion.getZ()));
                    } else {
                        d.lastX = potion.getX(); d.lastY = potion.getY(); d.lastZ = potion.getZ();
                    }
                }

                Set<Integer> removed = new HashSet<>(trackedPotions.keySet());
                removed.removeAll(current);
                for (int eid : removed) {
                    PotionData data = trackedPotions.remove(eid);
                    if (data == null) continue;

                    PotionContentsComponent contents = data.stack.get(DataComponentTypes.POTION_CONTENTS);
                    if (contents == null) continue;
                    Iterable<StatusEffectInstance> effects = contents.getEffects();
                    if (!effects.iterator().hasNext()) continue;

                    Box hitBox = new Box(data.lastX - 4, data.lastY - 2, data.lastZ - 4, data.lastX + 4, data.lastY + 2, data.lastZ + 4);
                    for (Entity e2 : mc.world.getEntitiesByClass(PlayerEntity.class, hitBox, p -> true)) {
                        PlayerEntity pl = (PlayerEntity) e2;
                        double dx = pl.getX() - data.lastX;
                        double dz = pl.getZ() - data.lastZ;
                        if (Math.sqrt(dx * dx + dz * dz) > 4.0) continue;

                        boolean isSelf = pl == mc.player;
                        int nameColor = isSelf ? 0x55FF55 : Client.FRIENDS.isFriend(pl.getName().getString()) ? ClientColors.FRIEND_COLOR.getRGB() : Color.WHITE.getRGB();
                        Text playerName = Text.of(pl.getName().getString()).copy().withColor(nameColor);

                        double dist = Math.sqrt(dx * dx + dz * dz);
                        double hitChance = Math.max(0, 1.0 - dist / 4.0) * 100.0;
                        int hitColor = hitChance >= 65 ? 0x55FF55 : hitChance >= 35 ? 0xFFFF55 : 0xFF5555;

                        SpecialPotionType special = matchSpecial(effects);
                        if (special != null) {
                            String cleanName = special.getDisplayName().replaceAll("§.", "");
                            Client.NOTIFIES.add(Text.empty().append(playerName).append(Text.of(" получил ").copy().withColor(0xAAAAAA)).append(Text.of(cleanName).copy().withColor(special.getBaseColor())).append(Text.of(String.format(" (%.0f%%)", hitChance)).copy().withColor(hitColor)), IconUse.POTION, 5000);
                        } else {
                            for (StatusEffectInstance eff : effects) {
                                String level = eff.getAmplifier() > 0 ? " " + toRoman(eff.getAmplifier() + 1) : "";
                                String duration = formatDuration(eff.getDuration());
                                Client.NOTIFIES.add(Text.empty().append(playerName).append(Text.of(" получил ").copy().withColor(0xAAAAAA)).append(eff.getEffectType().value().getName().copy().withColor(eff.getEffectType().value().getColor())).append(Text.of(level).copy().withColor(0xFFFFAA)).append(Text.of(" [" + duration + "]").copy().withColor(0x888888)), IconUse.POTION, 5000);
                            }
                        }
                    }
                }
            }
        }
    };

    private void sendArmorAlert(ItemStack stack, int percent) {
        String itemName = stack.getName().getString();
        String iconKey = Registries.ITEM.getId(stack.getItem()).getPath();

        Text message = Text.empty()
                .append(Text.of(itemName).copy().withColor(0xFFFFFF))
                .append(Text.of(" скоро сломается! ").copy().withColor(0xAAAAAA))
                .append(Text.of(percent + "%").copy().withColor(percent <= 5 ? 0xFF5555 : 0xFFFF55));

        Client.NOTIFIES.addItem(message, iconKey, 5000);

     //   mc.inGameHud.getChatHud().addMessage(Text.literal("§8[§c!§8] §f").append(message));
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        notificationElement.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        trackedPotions.clear();
        armorWarnState.clear();
        notificationElement.setEnabled(false);
    }

    private static String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (minutes > 0) return minutes + "м " + secs + "с";
        return secs + "с";
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    private SpecialPotionType matchSpecial(Iterable<StatusEffectInstance> effects) {
        java.util.Set<String> incoming = new java.util.HashSet<>();
        for (StatusEffectInstance eff : effects) {
            incoming.add(eff.getEffectType().value().getTranslationKey() + ":" + eff.getAmplifier());
        }
        if (incoming.isEmpty()) return null;

        for (SpecialPotionType special : SpecialPotionType.values()) {
            java.util.Set<String> expected = new java.util.HashSet<>();
            for (SpecialPotionType.PotionEffectData d : special.getEffects()) {
                expected.add(d.getEffect().getTranslationKey() + ":" + d.getAmplifier());
            }
            long matched = incoming.stream().filter(expected::contains).count();
            if (matched >= Math.ceil(expected.size() / 2.0)) return special;
        }
        return null;
    }

    private boolean isDonateItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        AutoBuyManager manager = AutoBuy.INSTANCE.getManager();
        if (manager == null) return false;
        for (ItemBuy item : manager.getFuntime()) {
            if (item.isBuy(stack)) return true;
        }
        for (ItemBuy item : manager.getHollyworld()) {
            if (item.isBuy(stack)) return true;
        }
        return false;
    }

    @AllArgsConstructor @Getter
    public enum TrackedEvents implements EnumChoice {
        TOTEM("Тотем поп", true),
        POTION_TRACKER("Трекер зелий", true),
        ITEMPICKUP("Подбор предметов", true),
        ARMOR_DURABILITY("Прочность брони", true),
        ; final String renderName; final boolean defaultEnabled;
    }
}