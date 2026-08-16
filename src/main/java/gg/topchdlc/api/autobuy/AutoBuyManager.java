package gg.topchdlc.api.autobuy;

import java.util.ArrayList;
import java.util.List;

import gg.topchdlc.api.autobuy.item.ItemBuy;
import lombok.Generated;
import net.minecraft.item.Items;
import gg.topchdlc.api.autobuy.enchantes.minecraft.EnchantVanilla;
import gg.topchdlc.api.autobuy.item.Lore.LoreEnchantItemBuy;
import gg.topchdlc.api.autobuy.item.Lore.LoreItemBuy;
import gg.topchdlc.api.autobuy.item.Name.NameLoreItemBuy;
import gg.topchdlc.api.autobuy.item.donate.DonateItemBuy;
import gg.topchdlc.api.autobuy.item.donate.SphereData;
import gg.topchdlc.api.autobuy.item.hollyworld.HolyWorldData;
import gg.topchdlc.api.autobuy.item.hollyworld.HolyWorldItemBuy;
import gg.topchdlc.api.autobuy.item.other.AttributeNbtItemBuy;
import gg.topchdlc.api.autobuy.item.other.PotionEffectItemBuy;

/**
 * Create by daun kvass
 */
public class AutoBuyManager {
   private ArrayList<ItemBuy> vanilla = new ArrayList();
   private ArrayList<ItemBuy> funtime = new ArrayList();
   private ArrayList<ItemBuy> hollyworld = new ArrayList();

   public AutoBuyManager() {
      LoreEnchantItemBuy crusherHELMET = new LoreEnchantItemBuy(Items.NETHERITE_HELMET.getDefaultStack(), "Шлем Крушителя", "Шлем Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addEnchant(new EnchantVanilla("Protection", "protection", 5))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 5))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1))
         .addEnchant(new EnchantVanilla("Blast Protection", "blast_protection", 5))
         .addEnchant(new EnchantVanilla("Respiration", "respiration", 3))
         .addEnchant(new EnchantVanilla("Aqua Affinity", "aqua_affinity", 1))
         .addEnchant(new EnchantVanilla("Fire Protection", "fire_protection", 5))
         .addEnchant(new EnchantVanilla("Projectile Protection", "projectile_protection", 5));
      LoreEnchantItemBuy crusherCHESTPLATE = new LoreEnchantItemBuy(Items.NETHERITE_CHESTPLATE.getDefaultStack(), "Нагрудник Крушителя", "Нагрудник Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addEnchant(new EnchantVanilla("Protection", "protection", 5))
         .addEnchant(new EnchantVanilla("Blast Protection", "blast_protection", 5))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 5))
         .addEnchant(new EnchantVanilla("Fire Protection", "fire_protection", 5))
         .addEnchant(new EnchantVanilla("Projectile Protection", "projectile_protection", 5));
      LoreEnchantItemBuy crusherLEGGINGS = new LoreEnchantItemBuy(Items.NETHERITE_LEGGINGS.getDefaultStack(), "Поножи Крушителя", "Поножи Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addEnchant(new EnchantVanilla("Protection", "protection", 5))
         .addEnchant(new EnchantVanilla("Blast Protection", "blast_protection", 5))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 5))
         .addEnchant(new EnchantVanilla("Fire Protection", "fire_protection", 5))
         .addEnchant(new EnchantVanilla("Projectile Protection", "projectile_protection", 5));
      LoreEnchantItemBuy crusherBoots = new LoreEnchantItemBuy(Items.NETHERITE_BOOTS.getDefaultStack(), "Ботинки Крушителя", "Ботинки Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addEnchant(new EnchantVanilla("Protection", "protection", 5))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 5))
         .addEnchant(new EnchantVanilla("Blast Protection", "blast_protection", 5))
         .addEnchant(new EnchantVanilla("Depth Strider", "depth_strider", 3))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1))
         .addEnchant(new EnchantVanilla("Fire Protection", "fire_protection", 5))
         .addEnchant(new EnchantVanilla("Feather Falling", "feather_falling", 4))
         .addEnchant(new EnchantVanilla("Soul Speed", "soul_speed", 3))
         .addEnchant(new EnchantVanilla("Projectile Protection", "projectile_protection", 5));
      LoreEnchantItemBuy crusherSword = new LoreEnchantItemBuy(Items.NETHERITE_SWORD.getDefaultStack(), "Меч Крушителя", "Меч Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addLoreLine("Опытный III")
         .addLoreLine("Вампиризм II")
         .addLoreLine("Окисление II")
         .addLoreLine("Яд III")
         .addLoreLine("Детекция III")
         .addEnchant(new EnchantVanilla("Sweeping Edge", "sweeping_edge", 3))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1))
         .addEnchant(new EnchantVanilla("Fire Aspect", "fire_aspect", 2))
         .addEnchant(new EnchantVanilla("Bane of Arthropods", "bane_of_arthropods", 7))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 5))
         .addEnchant(new EnchantVanilla("Looting", "looting", 5))
         .addEnchant(new EnchantVanilla("Smite", "smite", 7))
         .addEnchant(new EnchantVanilla("Sharpness", "sharpness", 7));
      LoreEnchantItemBuy crusherPickaxe = new LoreEnchantItemBuy(Items.NETHERITE_PICKAXE.getDefaultStack(), "Кирка Крушителя", "Кирка Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addLoreLine("Бульдозер II")
         .addLoreLine("Опытный III")
         .addLoreLine("Магнит")
         .addLoreLine("Авто-Плавка")
         .addLoreLine("Паутина")
         .addLoreLine("Пингер")
         .addEnchant(new EnchantVanilla("Fortune", "fortune", 5))
         .addEnchant(new EnchantVanilla("Efficiency", "efficiency", 10))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 5));
      LoreEnchantItemBuy crusherCrossbow = new LoreEnchantItemBuy(Items.CROSSBOW.getDefaultStack(), "Арбалет Крушителя", "Арбалет Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addEnchant(new EnchantVanilla("Multishot", "multishot", 1))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 3))
         .addEnchant(new EnchantVanilla("Piercing", "piercing", 5))
         .addEnchant(new EnchantVanilla("Quick Charge", "quick_charge", 3));
      LoreEnchantItemBuy crusherTrident = new LoreEnchantItemBuy(Items.TRIDENT.getDefaultStack(), "Трезубец Крушителя", "Трезубец Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addLoreLine("Скаут III")
         .addLoreLine("Опытный III")
         .addLoreLine("Вампиризм II")
         .addLoreLine("Ступор III")
         .addLoreLine("Притяжение II")
         .addLoreLine("Окисление II")
         .addLoreLine("Возвращение")
         .addLoreLine("Подрывник")
         .addLoreLine("Яд III")
         .addLoreLine("Детекция III")
         .addEnchant(new EnchantVanilla("Fire Aspect", "fire_aspect", 2))
         .addEnchant(new EnchantVanilla("Channeling", "channeling", 1))
         .addEnchant(new EnchantVanilla("Sharpness", "sharpness", 7))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 5))
         .addEnchant(new EnchantVanilla("Loyalty", "loyalty", 3))
         .addEnchant(new EnchantVanilla("Impaling", "impaling", 5))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1));
      LoreEnchantItemBuy crusherMace = new LoreEnchantItemBuy(Items.MACE.getDefaultStack(), "Булава Крушителя", "Булава Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLine("[★] Оригинальный предмет")
         .addLoreLine("Опытный III")
         .addLoreLine("Вампиризм II")
         .addLoreLine("Окисление II")
         .addLoreLine("Яд III")
         .addLoreLine("Детекция III")
         .addEnchant(new EnchantVanilla("Sharpness", "sharpness", 7))
         .addEnchant(new EnchantVanilla("Smite", "smite", 7))
         .addEnchant(new EnchantVanilla("Bane of Arthropods", "bane_of_arthropods", 7))
         .addEnchant(new EnchantVanilla("Density", "density", 5))
         .addEnchant(new EnchantVanilla("Breach", "breach", 3))
         .addEnchant(new EnchantVanilla("Sweeping Edge", "sweeping_edge", 3))
         .addEnchant(new EnchantVanilla("Fire Aspect", "fire_aspect", 2))
         .addEnchant(new EnchantVanilla("Looting", "looting", 5))
         .addEnchant(new EnchantVanilla("Unbreaking", "unbreaking", 5))
         .addEnchant(new EnchantVanilla("Mending", "mending", 1));
      SphereData.Sphere aresData = SphereData.get("Ареса");
       LoreItemBuy sphereAres = new LoreItemBuy(aresData.name, aresData.name, ItemBuy.Category.FUNTIME, aresData.skin)
               .addLoreLines(aresData.loreLines);
       SphereData.Sphere hydraData = SphereData.get("Гидры");
       LoreItemBuy sphereHydra = new LoreItemBuy(hydraData.name, hydraData.name, ItemBuy.Category.FUNTIME, hydraData.skin)
               .addLoreLines(hydraData.loreLines);
       SphereData.Sphere ikarusData = SphereData.get("Икара");
       LoreItemBuy sphereIkarus = new LoreItemBuy(ikarusData.name, ikarusData.name, ItemBuy.Category.FUNTIME, ikarusData.skin)
               .addLoreLines(ikarusData.loreLines);
       SphereData.Sphere titanData = SphereData.get("Титана");
       LoreItemBuy sphereTitan = new LoreItemBuy(titanData.name, titanData.name, ItemBuy.Category.FUNTIME, titanData.skin)
               .addLoreLines(titanData.loreLines);
       SphereData.Sphere eridaData = SphereData.get("Эрида");
       LoreItemBuy sphereErida = new LoreItemBuy(eridaData.name, eridaData.name, ItemBuy.Category.FUNTIME, eridaData.skin)
               .addLoreLines(eridaData.loreLines);
       SphereData.Sphere chaosData = SphereData.get("Хаоса");
       LoreItemBuy sphereChaos = new LoreItemBuy(chaosData.name, chaosData.name, ItemBuy.Category.FUNTIME, chaosData.skin)
               .addLoreLines(chaosData.loreLines);
       SphereData.Sphere satyrData = SphereData.get("Сатира");
       LoreItemBuy sphereSatyr = new LoreItemBuy(satyrData.name, satyrData.name, ItemBuy.Category.FUNTIME, satyrData.skin)
               .addLoreLines(satyrData.loreLines);
       SphereData.Sphere beastData = SphereData.get("Бестии");
       LoreItemBuy sphereBeast = new LoreItemBuy(beastData.name, beastData.name, ItemBuy.Category.FUNTIME, beastData.skin)
               .addLoreLines(beastData.loreLines);
       SphereData.Sphere athenaData = SphereData.get("Афины");
       AttributeNbtItemBuy sphereAthena = new AttributeNbtItemBuy(athenaData.name, athenaData.name, ItemBuy.Category.FUNTIME, athenaData.skin)
               .addAttribute("minecraft:generic.attack_speed", 0.15, 1, "offhand")
               .addAttribute("minecraft:generic.movement_speed", 0.15, 1, "offhand")
               .addAttribute("minecraft:generic.attack_damage", 3.0, 0, "offhand")
               .addAttribute("minecraft:generic.max_health", -2.0, 0, "offhand");
      LoreItemBuy krush = new LoreItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Крушителя", ItemBuy.Category.FUNTIME)
         .addLoreLines("Легендарный символ", "Несокрушимая мощь", "Ломающая преграды");
      LoreItemBuy karatel = new LoreItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Карателя", ItemBuy.Category.FUNTIME)
         .addLoreLines("Несёт строгий приговор", "Карая всех врагов", "Но ослабляя тело");
      LoreItemBuy yarost = new LoreItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Ярости", ItemBuy.Category.FUNTIME)
         .addLoreLines("Чистая, дикая агрессия", "Граничит с безумием", "Меняя жизнь на урон");
      LoreItemBuy demon = new LoreItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Демона", ItemBuy.Category.FUNTIME)
         .addLoreLines("Печать разжигает ярость", "Ускоряя удары сердца", "И силу каждой атаки");
      LoreItemBuy mrak = new LoreItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Мрака", ItemBuy.Category.FUNTIME)
         .addLoreLines("Мрак сгущается рядом", "Укрывая владельца", "И питая его силы");
      LoreItemBuy vihr = new LoreItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Вихря", ItemBuy.Category.FUNTIME)
         .addLoreLines("Вихрь не знает покоя", "Ускоряя владельца", "И закаляя его дух");
      LoreItemBuy razdor = new LoreItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Раздора", ItemBuy.Category.FUNTIME)
         .addLoreLines("Раздор жаждает хаоса", "Даруя безумный темп", "Но разрушая броню");
      LoreItemBuy tiran = new LoreItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Тирана", ItemBuy.Category.FUNTIME)
         .addLoreLines("Тиран подавляет слабых", "Дает защиту и силу", "Взимая кровавый налог");
      this.funtime.add(crusherHELMET);
      this.funtime.add(crusherCHESTPLATE);
      this.funtime.add(crusherLEGGINGS);
      this.funtime.add(crusherBoots);
      this.funtime.add(crusherSword);
      this.funtime.add(crusherPickaxe);
      this.funtime.add(crusherCrossbow);
      this.funtime.add(crusherTrident);
      this.funtime.add(crusherMace);
      this.funtime.add(sphereAres);
      this.funtime.add(sphereChaos);
      this.funtime.add(sphereBeast);
      this.funtime.add(sphereErida);
      this.funtime.add(sphereHydra);
      this.funtime.add(sphereSatyr);
      this.funtime.add(sphereAthena);
      this.funtime.add(sphereTitan);
      this.funtime.add(sphereIkarus);
      this.funtime.add(krush);
      this.funtime.add(karatel);
      this.funtime.add(yarost);
      this.funtime.add(demon);
      this.funtime.add(mrak);
      this.funtime.add(vihr);
      this.funtime.add(razdor);
      this.funtime.add(tiran);
       PotionEffectItemBuy svyatayaVoda = new PotionEffectItemBuy(Items.SPLASH_POTION.getDefaultStack(), "Святая вода", ItemBuy.Category.FUNTIME)
               .addEffectExact(PotionEffectItemBuy.Effects.REGENERATION, 2)
               .addEffectExact(PotionEffectItemBuy.Effects.INVISIBILITY, 1)
               .addEffectExact(PotionEffectItemBuy.Effects.INSTANT_HEALTH, 1);
       PotionEffectItemBuy zelieGneva = new PotionEffectItemBuy(Items.SPLASH_POTION.getDefaultStack(), "Зелье Гнева", ItemBuy.Category.FUNTIME)
               .addEffectExact(PotionEffectItemBuy.Effects.STRENGTH, 4)
               .addEffectExact(PotionEffectItemBuy.Effects.SLOWNESS, 3);
       PotionEffectItemBuy zeliePalladina = new PotionEffectItemBuy(Items.SPLASH_POTION.getDefaultStack(), "Зелье Палладина", ItemBuy.Category.FUNTIME)
               .addEffect(PotionEffectItemBuy.Effects.RESISTANCE)
               .addEffect(PotionEffectItemBuy.Effects.FIRE_RESISTANCE)
               .addEffectExact(PotionEffectItemBuy.Effects.HEALTH_BOOST, 2)
               .addEffectExact(PotionEffectItemBuy.Effects.INVISIBILITY, 2);
       PotionEffectItemBuy zelieAssasina = new PotionEffectItemBuy(Items.SPLASH_POTION.getDefaultStack(), "Зелье Ассасина", ItemBuy.Category.FUNTIME)
               .addEffectExact(PotionEffectItemBuy.Effects.STRENGTH, 3)
               .addEffectExact(PotionEffectItemBuy.Effects.SPEED, 2)
               .addEffect(PotionEffectItemBuy.Effects.HASTE)
               .addEffectExact(PotionEffectItemBuy.Effects.INSTANT_DAMAGE, 1);
       PotionEffectItemBuy zelieRadiacii = new PotionEffectItemBuy(Items.SPLASH_POTION.getDefaultStack(), "Зелье Радиации", ItemBuy.Category.FUNTIME)
               .addEffect(PotionEffectItemBuy.Effects.POISON)
               .addEffect(PotionEffectItemBuy.Effects.WITHER)
               .addEffectExact(PotionEffectItemBuy.Effects.SLOWNESS, 2)
               .addEffectExact(PotionEffectItemBuy.Effects.HUNGER, 4)
               .addEffect(PotionEffectItemBuy.Effects.GLOWING);
       PotionEffectItemBuy snotvornoe = new PotionEffectItemBuy(Items.SPLASH_POTION.getDefaultStack(), "Снотворное", ItemBuy.Category.FUNTIME)
               .addEffectExact(PotionEffectItemBuy.Effects.WEAKNESS, 1)
               .addEffect(PotionEffectItemBuy.Effects.MINING_FATIGUE)
               .addEffectExact(PotionEffectItemBuy.Effects.WITHER, 2)
               .addEffect(PotionEffectItemBuy.Effects.BLINDNESS);
       this.funtime.add(svyatayaVoda);
       this.funtime.add(snotvornoe);
       this.funtime.add(zelieAssasina);
       this.funtime.add(zeliePalladina);
       this.funtime.add(zelieRadiacii);
       this.funtime.add(zelieGneva);
      DonateItemBuy tntTierWhite = new DonateItemBuy(Items.TNT.getDefaultStack(), "TNT - TIER WHITE", "таер вайт", ItemBuy.Category.FUNTIME)
         .addLoreLine("в 10 раз сильнее обычного");
      DonateItemBuy tntTaerBlack = new DonateItemBuy(Items.TNT.getDefaultStack(), "TNT - TIER BLACK", "таер блэк", ItemBuy.Category.FUNTIME)
               .addLoreLine("способен взорвать обсидиан");
      this.funtime.add(tntTierWhite);
      this.funtime.add(tntTaerBlack);


      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера Цербера", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjA5NWE3ZmQ5MGRhYTFiYmU3MDY5MDg5NzQwZTA1ZDBiZmM2NjI5NmVlM2M0MGVlNzFhNGUwYTY2MTZiMmJiYyJ9fX0=", "Cerber", "hms-damage:5,hms-rush:1"),
         "Сфера Цербера", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Cerber"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера Флеша", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzc0MDBlYTE5ZGJkODRmNzVjMzlhZDY4MjNhYzRlZjc4NmYzOWY0OGZjNmY4NDYwMjM2NmFjMjliODM3NDIyIn19fQ==", "Flash", "hms-speed:3,hms-armor:1"),
         "Сфера Флеша", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Flash"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNlZDRjZTIzOTMzZTY2ZTA0ZGYxNjA3MDY0NGY3NTk5ZWViNTUzMDdmN2VhZmU4ZDkyZjQwZmIzNTIwODYzYyJ9fX0=", "Immortal", "hms-speed:2,hms-damage:3"),
         "Сфера Имморталити", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Immortal"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE2MmI5ZGU2YTI2Yjg2ODY5Y2EyMmVhNDBmMWJkZTgwYTA0MzBhNTQ1NDdiZWNjZThmZGE4NzA3Nzc3MjU4ZiJ9fX0=", "Armortality", "hms-armor:2,hms-damage:2,hms-health:2"),
         "Сфера Арморталити", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Armortality"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера на Скорость III", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19", "Speed3", null),
         "Сфера на скорость 3", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Speed3"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера Eternity", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19", "Eternity", "hms-speed:2,hms-damage:2,hms-armor:2"),
         "Сфера Eternity", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Eternity"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера Stinger", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19", "Stinger", "hms-speed:1,hms-armor:2,hms-damage:2"),
         "Сфера Stinger", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Stinger"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера на броня III скорость II", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==", "Mythical3", "hms-armor:3,hms-speed:2"),
         "Сфера на броня 3", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Mythical3"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера на урон II броня III", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==", "Speed", "hms-armor:3,hms-damage:2"),
         "Сфера на броня 3", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Speed"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphere("Сфера на броня II урон III", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFmZjJlYjQ5OGU1YzZhMDQ0ODRmMGM5Zjc4NWI0NDg0NzlhYjIxM2RmOTVlYzkxMTc2YTMwOGExMmFkZDcwIn19fQ==", "Mythical1", "hms-armor:2,hms-damage:3"),
         "Сфера на броня 3", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphere", "sphereName", "Mythical1"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createBackpack("Рюкзак I уровень", Items.PINK_SHULKER_BOX, "mini"),
         "рюкзак 1 уровень", ItemBuy.Category.HOLLYWORLD, "HolyWorldBackpack", "backpackType", "mini"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createBackpack("Рюкзак II уровень", Items.LIGHT_BLUE_SHULKER_BOX, "normal"),
         "рюкзак 2 уровень", ItemBuy.Category.HOLLYWORLD, "HolyWorldBackpack", "backpackType", "normal"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createBackpack("Рюкзак III уровень", Items.RED_SHULKER_BOX, "big"),
         "рюкзак 3 уровень", ItemBuy.Category.HOLLYWORLD, "HolyWorldBackpack", "backpackType", "big"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createBackpack("Рюкзак IV уровень", Items.MAGENTA_SHULKER_BOX, "huge"),
         "рюкзак 4 уровень", ItemBuy.Category.HOLLYWORLD, "HolyWorldBackpack", "backpackType", "huge"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createBackpack("Рюкзак Infinity", Items.LIME_SHULKER_BOX, "infinity"),
         "рюкзак infinity", ItemBuy.Category.HOLLYWORLD, "HolyWorldBackpack", "backpackType", "infinity"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createPyrotechnic("Трапка", Items.POPPED_CHORUS_FRUIT, "ALTERNATIVE_TRAP"),
         "Трапка", ItemBuy.Category.HOLLYWORLD, "HolyWorldPyrotechnic", "pyrotechnicType", "ALTERNATIVE_TRAP"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createPyrotechnic("Взрывная трапка", Items.PRISMARINE_SHARD, "EXPLOSIVE_TRAP"),
         "Взрывная трапка", ItemBuy.Category.HOLLYWORLD, "HolyWorldPyrotechnic", "pyrotechnicType", "EXPLOSIVE_TRAP"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createPyrotechnic("Стан", Items.NETHER_STAR, "STUN_STAR"),
         "Стан", ItemBuy.Category.HOLLYWORLD, "HolyWorldPyrotechnic", "pyrotechnicType", "STUN_STAR"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createPyrotechnic("Взрывчатое вещество", Items.CLAY, "EXPLOSIVE_SUBSTANCE"),
         "Взрывчатое вещество", ItemBuy.Category.HOLLYWORLD, "HolyWorldPyrotechnic", "pyrotechnicType", "EXPLOSIVE_SUBSTANCE"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createPyrotechnic("Динамит А", Items.TNT, "A"),
         "Динамит А", ItemBuy.Category.HOLLYWORLD, "HolyWorldPyrotechnic", "pyrotechnicType", "A"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createPyrotechnic("Динамит B", Items.TNT, "B"),
         "динамит б", ItemBuy.Category.HOLLYWORLD, "HolyWorldPyrotechnic", "pyrotechnicType", "B"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createPyrotechnic("Динамит B2", Items.TNT, "B2"),
         "динамит б2", ItemBuy.Category.HOLLYWORLD, "HolyWorldPyrotechnic", "pyrotechnicType", "B2"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createPyrotechnic("C4 ВзРыВчАтКа", Items.TNT, "C4"),
         "с4 взрывчатка", ItemBuy.Category.HOLLYWORLD, "HolyWorldPyrotechnic", "pyrotechnicType", "C4"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringe("Взрывная штучка", Items.FIRE_CHARGE, "ExplosiveStuff"),
         "Взрывная штучка", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringe", "kringeType", "ExplosiveStuff"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringe("Ком снега", Items.SNOWBALL, "SnowBall"),
         "Ком снега", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringe", "kringeType", "SnowBall"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringe("Артефакт", Items.CONDUIT, "EmptyArtefact"),
         "Артефакт", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringe", "kringeType", "EmptyArtefact"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringe("Золотая кирка Джейка", Items.GOLDEN_PICKAXE, "jake-pickaxe"),
         "Золотая кирка Джейка", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringe", "kringeType", "jake-pickaxe"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createRune("Руна «Бессмертие»", Items.ORANGE_DYE, "immortality"),
         "Бессмертие", ItemBuy.Category.HOLLYWORLD, "HolyWorldRune", "runeId", "immortality"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Охотник", Items.NETHERITE_SWORD, "EXP_DROPPER"),
         "Охотник", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "EXP_DROPPER"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Снеговик", Items.SNOW_BLOCK, "BLINDNESS"),
         "Снеговик", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "BLINDNESS"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Иллюминатор", Items.SEA_LANTERN, "PORTHOLE"),
         "Иллюминатор", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "PORTHOLE"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Эндермен", Items.ENDER_PEARL, "ENDERMAN"),
         "Эндермен", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "ENDERMAN"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Анти Фантом", Items.PHANTOM_MEMBRANE, "ANTI_PHANTOM"),
         "Анти Фантом", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "ANTI_PHANTOM"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Телекинез", Items.HONEY_BLOCK, "TELEKINESIS"),
         "Телекинез", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "TELEKINESIS"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Гравитация", Items.FEATHER, "GRAVITY"),
         "Гравитация", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "GRAVITY"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Вампиризм", Items.WITHER_SKELETON_SKULL, "VAMPIRE"),
         "Вампиризм", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "VAMPIRE"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Справедливость", Items.POTION, "JUSTICE"),
         "Справедливость", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "JUSTICE"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Универсальный ключ", Items.TRIPWIRE_HOOK, "UNIVERSAL_KEY"),
         "Универсальный ключ", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "UNIVERSAL_KEY"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createKringeEffect("Фармер", Items.DIAMOND_SWORD, "FARMER"),
         "Фармер", ItemBuy.Category.HOLLYWORLD, "HolyWorldKringeEffect", "effectType", "FARMER"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createExpBottle("Пузырек с 15 уровнем", 315),
         "15", ItemBuy.Category.HOLLYWORLD, "HolyWorldExpBottle", "holy-exp-bottle-value", "315"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createExpBottle("Пузырек с 50 уровнем", 5345),
         "50", ItemBuy.Category.HOLLYWORLD, "HolyWorldExpBottle", "holy-exp-bottle-value", "5345"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createExpBottle("Пузырек с 100 уровнем", 30971),
         "100", ItemBuy.Category.HOLLYWORLD, "HolyWorldExpBottle", "holy-exp-bottle-value", "30971"));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSphereShard("Осколок сферы", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmY3YmJjZTIzZTgxNjJlNDJkMjA3MDU1YjBjZTkwZjBlZDU3YjAxNWU1MjEyMTM5YWM4ZmM3ZTZkNDVkZGZjYSJ9fX0="),
         "Осколок сферы", ItemBuy.Category.HOLLYWORLD, "HolyWorldSphereShard", null, null));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSimple(Items.ENDER_PEARL),
         "Эндер-жемчуг", ItemBuy.Category.HOLLYWORLD));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSimple(Items.CHORUS_FRUIT),
         "Плод хоруса", ItemBuy.Category.HOLLYWORLD));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSimple(Items.FIREWORK_ROCKET),
         "Фейерверк", ItemBuy.Category.HOLLYWORLD));
      this.hollyworld.add(new HolyWorldItemBuy(
         HolyWorldData.createSimple(Items.PRISMARINE_CRYSTALS),
         "Боевой фрагмент", ItemBuy.Category.HOLLYWORLD));
       NameLoreItemBuy dezorientaciya = new NameLoreItemBuy(
               Items.ENDER_EYE.getDefaultStack(),
               "Дезориентация",
               "дезориентация",
               List.of("звуковая волна"),
               ItemBuy.Category.FUNTIME
       );
       NameLoreItemBuy yavniapil = new NameLoreItemBuy(
               Items.SUGAR.getDefaultStack(),
               "Явная Пыль",
               "явная пыль",
               List.of("световая вспышка"),
               ItemBuy.Category.FUNTIME
       );
       this.funtime.add(yavniapil);
        this.funtime.add(dezorientaciya);

      //this.funtime.add(new DonateItemBuy(Items.SUGAR.getDefaultStack(), "Явная пыль", ItemBuy.Category.FUNTIME)
        // .addLoreLine("Световая вспышка"));
       //  this.funtime.add(new DonateItemBuy(Items.ENDER_EYE.getDefaultStack(), "Дезориентация", ItemBuy.Category.FUNTIME)
      //   .addLoreLine("Чем ближе цель"));
      this.funtime.add(new DonateItemBuy(Items.NETHERITE_SCRAP.getDefaultStack(), "Трапка", ItemBuy.Category.FUNTIME)
         .addLoreLine("Нерушимая клетка"));
      this.funtime.add(new DonateItemBuy(Items.TRIPWIRE_HOOK.getDefaultStack(), "Отмычка к Сферам", ItemBuy.Category.FUNTIME)
         .addLoreLine("С Сферами"));
      this.funtime.add(new DonateItemBuy(Items.DRIED_KELP.getDefaultStack(), "Пласт", ItemBuy.Category.FUNTIME)
         .addLoreLine("Нерушимая стена"));
      this.funtime.add(new DonateItemBuy(Items.JIGSAW.getDefaultStack(), "Блок дамагер", ItemBuy.Category.FUNTIME)
         .addLoreLine("Нанесение урона"));
      this.funtime.add(new DonateItemBuy(Items.STRUCTURE_BLOCK.getDefaultStack(), "Блок чанкер", ItemBuy.Category.FUNTIME)
         .addLoreLine("Прогружает чанк, в котором"));
      this.funtime.add(new DonateItemBuy(Items.BEACON.getDefaultStack(), "Загадочный маяк", ItemBuy.Category.FUNTIME)
         .addLoreLine("временный"));
      this.funtime.add(new DonateItemBuy(Items.SOUL_LANTERN.getDefaultStack(), "Проклятая душа", ItemBuy.Category.FUNTIME)
         .addLoreLine("Обменяй души"));
      this.funtime.add(new DonateItemBuy(Items.PAPER.getDefaultStack(), "Драконий скин", ItemBuy.Category.FUNTIME)
         .addLoreLine("Драконий скин взамен"));
      this.funtime.add(new DonateItemBuy(Items.FIRE_CHARGE.getDefaultStack(), "Огненный смерч", ItemBuy.Category.FUNTIME)
         .addLoreLine("Огненная волна"));
      this.funtime.add(new DonateItemBuy(Items.SNOWBALL.getDefaultStack(), "Снежок заморозка", ItemBuy.Category.FUNTIME)
         .addLoreLine("Ледяная сфера"));
      this.funtime.add(new DonateItemBuy(Items.PHANTOM_MEMBRANE.getDefaultStack(), "Божья аура", ItemBuy.Category.FUNTIME)
         .addLoreLine("Божественная аура"));
      this.funtime.add(new DonateItemBuy(Items.IRON_NUGGET.getDefaultStack(), "Серебро", ItemBuy.Category.FUNTIME)
         .addLoreLine("валюта для покупки"));
      this.funtime.add(new DonateItemBuy(Items.GOLDEN_PICKAXE.getDefaultStack(), "Божье касание", ItemBuy.Category.FUNTIME)
         .addLoreLine("Может добыть спавнер"));
      this.funtime.add(new DonateItemBuy(Items.NETHERITE_PICKAXE.getDefaultStack(), "Молот Тора", ItemBuy.Category.FUNTIME)
         .addLoreLine("Вскапывает территорию"));
       this.vanilla.add(new ItemBuy(Items.ENCHANTED_GOLDEN_APPLE.getDefaultStack(), "Зачарованное золотое яблоко", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.DRAGON_HEAD.getDefaultStack(),"Голова дракона",ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.SKELETON_SKULL.getDefaultStack(),"Голова скелета",ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.WITHER_SKELETON_SKULL.getDefaultStack(),"Череп визер-скелета",ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.GOLDEN_APPLE.getDefaultStack(), "Золотое яблоко", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.SPAWNER.getDefaultStack(), "Спавнер", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.ENDER_PEARL.getDefaultStack(), "Эндер жемчуг", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Тотем бессмертия", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.EXPERIENCE_BOTTLE.getDefaultStack(), "Пузырёк опыта", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.ELYTRA.getDefaultStack(), "Элитры", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.NETHERITE_INGOT.getDefaultStack(), "Незеритовый слиток", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.DIAMOND.getDefaultStack(), "Алмаз", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.MACE.getDefaultStack(),"Булава", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.EMERALD_ORE.getDefaultStack(), "Изумрудная Руда", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.IRON_INGOT.getDefaultStack(), "Железный слиток", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.GOLD_INGOT.getDefaultStack(), "Золотой слиток", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.OBSIDIAN.getDefaultStack(), "Обсидиан", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.CRYING_OBSIDIAN.getDefaultStack(), "Плачущий обсидиан", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.TNT.getDefaultStack(), "Динамит", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.TRIAL_KEY.getDefaultStack(), "Ключ испытаний", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.OMINOUS_TRIAL_KEY.getDefaultStack(), "Зловещий ключ", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.GUNPOWDER.getDefaultStack(), "Порох", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.ZOMBIE_VILLAGER_SPAWN_EGG.getDefaultStack(), "Яйцо призыва зомби-жителя", ItemBuy.Category.ANY));
       this.vanilla.add(new ItemBuy(Items.VILLAGER_SPAWN_EGG.getDefaultStack(), "Яйцо призыва жителя", ItemBuy.Category.ANY));
   }

   @Generated
   public ArrayList<ItemBuy> getVanilla() {
      return this.vanilla;
   }

   @Generated
   public ArrayList<ItemBuy> getFuntime() {
      return this.funtime;
   }

   @Generated
   public ArrayList<ItemBuy> getHollyworld() {
      return this.hollyworld;
   }
}
