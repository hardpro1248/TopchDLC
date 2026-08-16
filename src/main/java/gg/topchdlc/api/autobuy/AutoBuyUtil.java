package gg.topchdlc.api.autobuy;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import gg.topchdlc.api.autobuy.enchantes.Enchant;
import gg.topchdlc.api.autobuy.enchantes.custom.EnchantCustom;
import gg.topchdlc.api.autobuy.enchantes.minecraft.EnchantVanilla;
import gg.topchdlc.mixin.accessor.ItemStackAccessor;


public final class AutoBuyUtil {
    private static Pattern patternFuntime = Pattern.compile("\\$\\s*([0-9][\\d,]*)");
    private static Pattern patternFuntimeCena = Pattern.compile("\\$[^0-9]{0,20}([0-9][\\d,]*)");
    private static Pattern patternHollyWorld = Pattern.compile("Цена:(?:.*?\\{\"text\":\"([\\d ]+)\")");
    private static final Map<ItemStack, NbtCompound> nbtCompoundMap = new HashMap();
    public static List<String> testBypass = new ArrayList();
    private static int cacheAccessCounter = 0;
    private static final int CACHE_CLEANUP_THRESHOLD = 100;

    public static long getPrice(String nbt) {
        Matcher matcher = patternFuntime.matcher(nbt);
        String amount;
        if (matcher.find()) {
            amount = matcher.group(1);
            String price = amount.replace(",", "");
            try { return Long.parseLong(price); } catch (NumberFormatException ignored) {}
        }
        matcher = patternFuntimeCena.matcher(nbt);
        if (matcher.find()) {
            amount = matcher.group(1).replace(",", "").replace(" ", "");
            try { return Long.parseLong(amount); } catch (NumberFormatException ignored) {}
        }
        matcher = patternHollyWorld.matcher(nbt);
        if (matcher.find()) {
            amount = matcher.group(1).replaceAll(" ", "");
            try { return Long.parseLong(amount); } catch (NumberFormatException ignored) {}
        }
        return Long.MAX_VALUE;
    }

    public static boolean checkDon(ItemStack itemStack) {
        return itemStack.getCustomName().getString().contains("★");
    }

    public static long getPrice(ItemStack itemStack) {
        String nbt = copyNbt(itemStack);
        return getPrice(nbt);
    }

    public static String getSeller(ItemStack itemStack) {
        try {
            net.minecraft.component.type.LoreComponent lore = itemStack.get(DataComponentTypes.LORE);
            if (lore == null) return null;
            for (Text line : lore.lines()) {
                String text = line.getString();
                if (text.contains("Продавец:") || text.contains("Seller:")) {
                    String[] parts = text.split(":");
                    if (parts.length >= 2) {
                        return parts[1].trim().replaceAll("§[0-9a-fk-or]", "");
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String copyNbt(ItemStack itemStack) {
        return getTag(itemStack).toString();
    }

    public static String getKey(ItemStack itemStack) {
        System.out.println(copyNbt(itemStack));
        NbtComponent customData = (NbtComponent)itemStack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            System.out.println(customData.copyNbt().getKeys());
            System.out.println(itemStack.getItem());
            if (customData.copyNbt().contains("kringeItems")) {
                NbtElement customEnchants = customData.copyNbt().get("kringeItems");
                MinecraftClient.getInstance().keyboard.setClipboard(customEnchants.toString());
                return customEnchants.toString();
            }
        }

        return "";
    }

    public static NbtCompound getTag(ItemStack stack) {
        cacheAccessCounter++;
        if (cacheAccessCounter >= CACHE_CLEANUP_THRESHOLD) {
            nbtCompoundMap.clear();
            cacheAccessCounter = 0;
        }
        MergedComponentMap components = ((ItemStackAccessor)(Object)stack).getComponents();
        ComponentChanges changes = components.getChanges();
        World world = MinecraftClient.getInstance().world;
        return world == null ? new NbtCompound() : (NbtCompound)nbtCompoundMap.computeIfAbsent(stack, (itemStack) -> {
            return (NbtCompound)ComponentChanges.CODEC.encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), changes).getOrThrow();
        });
    }

    public static String getTagFuntimeNotTempElements(ItemStack stack) {
        cacheAccessCounter++;
        if (cacheAccessCounter >= CACHE_CLEANUP_THRESHOLD) {
            nbtCompoundMap.clear();
            cacheAccessCounter = 0;
        }
        MergedComponentMap components = ((ItemStackAccessor)(Object)stack).getComponents();
        ComponentChanges changes = components.getChanges();
        World world = MinecraftClient.getInstance().world;
        return world == null ? "" : ((NbtCompound)nbtCompoundMap.computeIfAbsent(stack, (itemStack) -> {
            return (NbtCompound)ComponentChanges.CODEC.encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), changes).getOrThrow();
        })).toString().replaceAll(",?\\s*PublicBukkitValues:\\{[^}]*\\}", "").replaceAll("'\\{[^']*Истeкaeт:[^']*\\}',?", "").replaceAll(",?UUID:\\[I;[-0-9]+,[-0-9]+,[-0-9]+,[-0-9]+]", "").replaceAll("minecraft:[0-9a-f\\-]{36}", "minecraft:UUID");
    }

    public static ArrayList<Enchant> getEnchants(ItemStack stack) {
        ArrayList<Enchant> enchantsBuy = new ArrayList();
        NbtComponent customData = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
        String type;
        if (customData != null && customData.copyNbt().contains("Enchantments")) {
            NbtList customEnchants = customData.copyNbt().getList("Enchantments").orElse(null);
            if (customEnchants == null) return enchantsBuy;
            for(int i = 0; i < customEnchants.size(); ++i) {
                NbtCompound ench = customEnchants.getCompound(i).orElse(null);
                if (ench == null) continue;
                type = ench.getString("id").orElse("");
                int level = ench.getInt("lvl").orElse(0);
                enchantsBuy.add(new EnchantCustom(type, type, level));
            }
        }

        ItemEnchantmentsComponent enchants = stack.getEnchantments();
        Iterator var9 = enchants.getEnchantmentEntries().iterator();
        while(var9.hasNext()) {
            Entry<RegistryEntry<Enchantment>> entry = (Entry)var9.next();
            type = ((RegistryKey)((RegistryEntry)entry.getKey()).getKey().get()).getValue().toString();
            enchantsBuy.add(new EnchantVanilla(type, type, entry.getIntValue()));
        }
        return enchantsBuy;
    }

    public static boolean isAuction(ScreenHandler handledScreen) {
        return handledScreen.slots.size() == 90 && handledScreen.getSlot(49).getStack().getItem() == Items.NETHER_STAR;
    }

    public static boolean isWaitBuy(ScreenHandler handledScreen) {
        return handledScreen.slots.size() == 63 && handledScreen.getSlot(0).getStack().getItem() == Items.LIME_STAINED_GLASS_PANE;
    }

    public static void test(int slotId) {
    }

    public static void clearCache() {
        nbtCompoundMap.clear();
        cacheAccessCounter = 0;
    }

    @Generated
    private AutoBuyUtil() {
        throw new UnsupportedOperationException("1");
    }
}
