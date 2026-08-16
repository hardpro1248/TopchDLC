package gg.topchdlc.vse.shutki.module.modules.impl.helper;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import gg.topchdlc.api.autobuy.AutoBuyManager;
import gg.topchdlc.api.autobuy.enchantes.Enchant;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import gg.topchdlc.api.autobuy.item.Lore.LoreEnchantItemBuy;
import gg.topchdlc.api.autobuy.item.Lore.LoreItemBuy;
import gg.topchdlc.api.events.list.EventChatMessage;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.AutoBuy;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.utils.network.CommandSendHelper;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class AHHelper extends Module {
    public static final AHHelper INSTANCE = new AHHelper();

    private final ColorSetting cheapSlotColor = colorSetting("Цвет дешевого", new Color(64, 255, 64, 140));
    private final ColorSetting goodSlotColor = colorSetting("Цвет выгодного", new Color(255, 255, 64, 140));
    private final CheckBox pricePerUnit = checkbox("Цена за единицу", true);
    private final CheckBox showPricePerUnit = checkbox("Показывать цену за единицу", true);
    private final KeybindSetting searchItemBind = keybindSetting("Поиск предмета", -1);
    private AHHelper() {
        super("AH Helper", Category.Misc, "Помощник C ауком причем умный");
    }

    public boolean isPricePerUnit() {
        return pricePerUnit.get();
    }

    public boolean isShowPricePerUnit() {
        return showPricePerUnit.get();
    }
    EventBus<Event> events = event -> {
        if (mc.player == null || !isEnabled()) return;

        if (event instanceof EventKey e) {
            if (searchItemBind.getBind() != -1 && e.getKey() == searchItemBind.getBind()) {
                searchItemInHand();
            }
        }

        if (event instanceof EventChatMessage e) {
            onChat(e);
        }
    };

    private static final Map<String, String> RUSSIAN_NAMES = new HashMap<>();
 // хахах сука какой я тупой не  не я не верю что решил сделать через такое это пиздец P.s после одной хуйни заебался и попросил чптун
    static {
        RUSSIAN_NAMES.put("stone", "Камень");
        RUSSIAN_NAMES.put("granite", "Гранит");
        RUSSIAN_NAMES.put("polished_granite", "Полированный гранит");
        RUSSIAN_NAMES.put("diorite", "Диорит");
        RUSSIAN_NAMES.put("polished_diorite", "Полированный диорит");
        RUSSIAN_NAMES.put("andesite", "Андезит");
        RUSSIAN_NAMES.put("polished_andesite", "Полированный андезит");
        RUSSIAN_NAMES.put("deepslate", "Глубинный сланец");
        RUSSIAN_NAMES.put("cobbled_deepslate", "Глубинный сланец булыжник");
        RUSSIAN_NAMES.put("polished_deepslate", "Полированный глубинный сланец");
        RUSSIAN_NAMES.put("calcite", "Кальцит");
        RUSSIAN_NAMES.put("tuff", "Туф");
        RUSSIAN_NAMES.put("dripstone_block", "Натёчный блок");
        RUSSIAN_NAMES.put("grass_block", "Дёрн");
        RUSSIAN_NAMES.put("dirt", "Земля");
        RUSSIAN_NAMES.put("coarse_dirt", "Грубая земля");
        RUSSIAN_NAMES.put("podzol", "Подзол");
        RUSSIAN_NAMES.put("rooted_dirt", "Земля с корнями");
        RUSSIAN_NAMES.put("mud", "Грязь");
        RUSSIAN_NAMES.put("cobblestone", "Булыжник");
        RUSSIAN_NAMES.put("oak_planks", "Дубовые доски");
        RUSSIAN_NAMES.put("spruce_planks", "Еловые доски");
        RUSSIAN_NAMES.put("birch_planks", "Берёзовые доски");
        RUSSIAN_NAMES.put("jungle_planks", "Тропические доски");
        RUSSIAN_NAMES.put("acacia_planks", "Акациевые доски");
        RUSSIAN_NAMES.put("dark_oak_planks", "Доски из тёмного дуба");
        RUSSIAN_NAMES.put("mangrove_planks", "Мангровые доски");
        RUSSIAN_NAMES.put("cherry_planks", "Вишнёвые доски");
        RUSSIAN_NAMES.put("bamboo_planks", "Бамбуковые доски");
        RUSSIAN_NAMES.put("crimson_planks", "Багровые доски");
        RUSSIAN_NAMES.put("warped_planks", "Искажённые доски");
        RUSSIAN_NAMES.put("sand", "Песок");
        RUSSIAN_NAMES.put("red_sand", "Красный песок");
        RUSSIAN_NAMES.put("gravel", "Гравий");
        RUSSIAN_NAMES.put("sandstone", "Песчаник");
        RUSSIAN_NAMES.put("red_sandstone", "Красный песчаник");
        RUSSIAN_NAMES.put("clay", "Глина");
        RUSSIAN_NAMES.put("bricks", "Кирпичи");
        RUSSIAN_NAMES.put("stone_bricks", "Каменные кирпичи");
        RUSSIAN_NAMES.put("mossy_stone_bricks", "Замшелые каменные кирпичи");
        RUSSIAN_NAMES.put("cracked_stone_bricks", "Потрескавшиеся каменные кирпичи");
        RUSSIAN_NAMES.put("chiseled_stone_bricks", "Резные каменные кирпичи");
        RUSSIAN_NAMES.put("mossy_cobblestone", "Замшелый булыжник");
        RUSSIAN_NAMES.put("obsidian", "Обсидиан");
        RUSSIAN_NAMES.put("crying_obsidian", "Плачущий обсидиан");
        RUSSIAN_NAMES.put("netherrack", "Адский камень");
        RUSSIAN_NAMES.put("soul_sand", "Песок душ");
        RUSSIAN_NAMES.put("soul_soil", "Почва душ");
        RUSSIAN_NAMES.put("basalt", "Базальт");
        RUSSIAN_NAMES.put("polished_basalt", "Полированный базальт");
        RUSSIAN_NAMES.put("smooth_basalt", "Гладкий базальт");
        RUSSIAN_NAMES.put("glowstone", "Светокамень");
        RUSSIAN_NAMES.put("nether_bricks", "Адские кирпичи");
        RUSSIAN_NAMES.put("red_nether_bricks", "Красные адские кирпичи");
        RUSSIAN_NAMES.put("end_stone", "Камень Края");
        RUSSIAN_NAMES.put("end_stone_bricks", "Кирпичи Края");
        RUSSIAN_NAMES.put("purpur_block", "Пурпурный блок");
        RUSSIAN_NAMES.put("purpur_pillar", "Пурпурный пилон");
        RUSSIAN_NAMES.put("prismarine", "Призмарин");
        RUSSIAN_NAMES.put("prismarine_bricks", "Призмариновые кирпичи");
        RUSSIAN_NAMES.put("dark_prismarine", "Тёмный призмарин");
        RUSSIAN_NAMES.put("sea_lantern", "Морской фонарь");
        RUSSIAN_NAMES.put("terracotta", "Терракота");
        RUSSIAN_NAMES.put("packed_mud", "Утрамбованная грязь");
        RUSSIAN_NAMES.put("mud_bricks", "Грязевые кирпичи");
        RUSSIAN_NAMES.put("quartz_block", "Кварцевый блок");
        RUSSIAN_NAMES.put("smooth_quartz", "Гладкий кварц");
        RUSSIAN_NAMES.put("chiseled_quartz_block", "Резной кварцевый блок");
        RUSSIAN_NAMES.put("quartz_bricks", "Кварцевые кирпичи");
        RUSSIAN_NAMES.put("quartz_pillar", "Кварцевый пилон");
        RUSSIAN_NAMES.put("hay_block", "Блок сена");
        RUSSIAN_NAMES.put("bone_block", "Костяной блок");
        RUSSIAN_NAMES.put("blackstone", "Чернит");
        RUSSIAN_NAMES.put("polished_blackstone", "Полированный чернит");
        RUSSIAN_NAMES.put("polished_blackstone_bricks", "Полированные чернитовые кирпичи");
        RUSSIAN_NAMES.put("chiseled_polished_blackstone", "Резной полированный чернит");
        RUSSIAN_NAMES.put("gilded_blackstone", "Позолоченный чернит");
        RUSSIAN_NAMES.put("amethyst_block", "Аметистовый блок");
        RUSSIAN_NAMES.put("copper_block", "Медный блок");
        RUSSIAN_NAMES.put("cut_copper", "Резная медь");
        RUSSIAN_NAMES.put("oak_log", "Дубовое бревно");
        RUSSIAN_NAMES.put("spruce_log", "Еловое бревно");
        RUSSIAN_NAMES.put("birch_log", "Берёзовое бревно");
        RUSSIAN_NAMES.put("jungle_log", "Тропическое бревно");
        RUSSIAN_NAMES.put("acacia_log", "Акациевое бревно");
        RUSSIAN_NAMES.put("dark_oak_log", "Бревно тёмного дуба");
        RUSSIAN_NAMES.put("mangrove_log", "Мангровое бревно");
        RUSSIAN_NAMES.put("cherry_log", "Вишнёвое бревно");
        RUSSIAN_NAMES.put("crimson_stem", "Багровый стебель");
        RUSSIAN_NAMES.put("warped_stem", "Искажённый стебель");
        RUSSIAN_NAMES.put("bamboo_block", "Бамбуковый блок");
        RUSSIAN_NAMES.put("stripped_oak_log", "Обтёсанное дубовое бревно");
        RUSSIAN_NAMES.put("stripped_spruce_log", "Обтёсанное еловое бревно");
        RUSSIAN_NAMES.put("stripped_birch_log", "Обтёсанное берёзовое бревно");
        RUSSIAN_NAMES.put("stripped_jungle_log", "Обтёсанное тропическое бревно");
        RUSSIAN_NAMES.put("stripped_acacia_log", "Обтёсанное акациевое бревно");
        RUSSIAN_NAMES.put("stripped_dark_oak_log", "Обтёсанное бревно тёмного дуба");
        RUSSIAN_NAMES.put("stripped_mangrove_log", "Обтёсанное мангровое бревно");
        RUSSIAN_NAMES.put("stripped_cherry_log", "Обтёсанное вишнёвое бревно");
        RUSSIAN_NAMES.put("stripped_crimson_stem", "Обтёсанный багровый стебель");
        RUSSIAN_NAMES.put("stripped_warped_stem", "Обтёсанный искажённый стебель");
        RUSSIAN_NAMES.put("coal_ore", "Угольная руда");
        RUSSIAN_NAMES.put("deepslate_coal_ore", "Глубинная угольная руда");
        RUSSIAN_NAMES.put("iron_ore", "Железная руда");
        RUSSIAN_NAMES.put("deepslate_iron_ore", "Глубинная железная руда");
        RUSSIAN_NAMES.put("copper_ore", "Медная руда");
        RUSSIAN_NAMES.put("deepslate_copper_ore", "Глубинная медная руда");
        RUSSIAN_NAMES.put("gold_ore", "Золотая руда");
        RUSSIAN_NAMES.put("deepslate_gold_ore", "Глубинная золотая руда");
        RUSSIAN_NAMES.put("nether_gold_ore", "Адская золотая руда");
        RUSSIAN_NAMES.put("redstone_ore", "Руда красного камня");
        RUSSIAN_NAMES.put("deepslate_redstone_ore", "Глубинная руда красного камня");
        RUSSIAN_NAMES.put("emerald_ore", "Изумрудная руда");
        RUSSIAN_NAMES.put("deepslate_emerald_ore", "Глубинная изумрудная руда");
        RUSSIAN_NAMES.put("lapis_ore", "Руда лазурита");
        RUSSIAN_NAMES.put("deepslate_lapis_ore", "Глубинная руда лазурита");
        RUSSIAN_NAMES.put("diamond_ore", "Алмазная руда");
        RUSSIAN_NAMES.put("deepslate_diamond_ore", "Глубинная алмазная руда");
        RUSSIAN_NAMES.put("nether_quartz_ore", "Кварцевая руда Нижнего мира");
        RUSSIAN_NAMES.put("ancient_debris", "Древние обломки");
        RUSSIAN_NAMES.put("coal", "Уголь");
        RUSSIAN_NAMES.put("charcoal", "Древесный уголь");
        RUSSIAN_NAMES.put("raw_iron", "Необработанное железо");
        RUSSIAN_NAMES.put("raw_copper", "Необработанная медь");
        RUSSIAN_NAMES.put("raw_gold", "Необработанное золото");
        RUSSIAN_NAMES.put("iron_ingot", "Железный слиток");
        RUSSIAN_NAMES.put("copper_ingot", "Медный слиток");
        RUSSIAN_NAMES.put("gold_ingot", "Золотой слиток");
        RUSSIAN_NAMES.put("netherite_ingot", "Незеритовый слиток");
        RUSSIAN_NAMES.put("netherite_scrap", "Незеритовый скрап");
        RUSSIAN_NAMES.put("diamond", "Алмаз");
        RUSSIAN_NAMES.put("emerald", "Изумруд");
        RUSSIAN_NAMES.put("lapis_lazuli", "Лазурит");
        RUSSIAN_NAMES.put("quartz", "Кварц Нижнего мира");
        RUSSIAN_NAMES.put("amethyst_shard", "Осколок аметиста");
        RUSSIAN_NAMES.put("iron_nugget", "Кусочек железа");
        RUSSIAN_NAMES.put("gold_nugget", "Кусочек золота");
        RUSSIAN_NAMES.put("redstone", "Красный камень");
        RUSSIAN_NAMES.put("flint", "Кремень");
        RUSSIAN_NAMES.put("stick", "Палка");
        RUSSIAN_NAMES.put("string", "Нить");
        RUSSIAN_NAMES.put("feather", "Перо");
        RUSSIAN_NAMES.put("gunpowder", "Порох");
        RUSSIAN_NAMES.put("leather", "Кожа");
        RUSSIAN_NAMES.put("rabbit_hide", "Кроличья шкурка");
        RUSSIAN_NAMES.put("slime_ball", "Слизь");
        RUSSIAN_NAMES.put("magma_cream", "Магмовый крем");
        RUSSIAN_NAMES.put("blaze_rod", "Огненный стержень");
        RUSSIAN_NAMES.put("blaze_powder", "Огненный порошок");
        RUSSIAN_NAMES.put("ender_pearl", "Эндер-жемчуг");
        RUSSIAN_NAMES.put("eye_of_ender", "Око Края");
        RUSSIAN_NAMES.put("nether_star", "Звезда Нижнего мира");
        RUSSIAN_NAMES.put("ghast_tear", "Слеза гаста");
        RUSSIAN_NAMES.put("phantom_membrane", "Мембрана фантома");
        RUSSIAN_NAMES.put("shulker_shell", "Панцирь шалкера");
        RUSSIAN_NAMES.put("heart_of_the_sea", "Сердце моря");
        RUSSIAN_NAMES.put("nautilus_shell", "Раковина наутилуса");
        RUSSIAN_NAMES.put("echo_shard", "Осколок эха");
        RUSSIAN_NAMES.put("disc_fragment_5", "Фрагмент диска");
        RUSSIAN_NAMES.put("honeycomb", "Медовые соты");
        RUSSIAN_NAMES.put("ink_sac", "Чернильный мешок");
        RUSSIAN_NAMES.put("glow_ink_sac", "Светящийся чернильный мешок");
        RUSSIAN_NAMES.put("bone_meal", "Костная мука");
        RUSSIAN_NAMES.put("bone", "Кость");
        RUSSIAN_NAMES.put("sugar", "Сахар");
        RUSSIAN_NAMES.put("paper", "Бумага");
        RUSSIAN_NAMES.put("book", "Книга");
        RUSSIAN_NAMES.put("experience_bottle", "Пузырёк опыта");
        RUSSIAN_NAMES.put("wooden_sword", "Деревянный меч");
        RUSSIAN_NAMES.put("stone_sword", "Каменный меч");
        RUSSIAN_NAMES.put("iron_sword", "Железный меч");
        RUSSIAN_NAMES.put("golden_sword", "Золотой меч");
        RUSSIAN_NAMES.put("diamond_sword", "Алмазный меч");
        RUSSIAN_NAMES.put("netherite_sword", "Незеритовый меч");
        RUSSIAN_NAMES.put("bow", "Лук");
        RUSSIAN_NAMES.put("crossbow", "Арбалет");
        RUSSIAN_NAMES.put("trident", "Трезубец");
        RUSSIAN_NAMES.put("mace", "Булава");
        RUSSIAN_NAMES.put("arrow", "Стрела");
        RUSSIAN_NAMES.put("spectral_arrow", "Спектральная стрела");
        RUSSIAN_NAMES.put("tipped_arrow", "Стрела с эффектом");
        RUSSIAN_NAMES.put("wooden_pickaxe", "Деревянная кирка");
        RUSSIAN_NAMES.put("stone_pickaxe", "Каменная кирка");
        RUSSIAN_NAMES.put("iron_pickaxe", "Железная кирка");
        RUSSIAN_NAMES.put("golden_pickaxe", "Золотая кирка");
        RUSSIAN_NAMES.put("diamond_pickaxe", "Алмазная кирка");
        RUSSIAN_NAMES.put("netherite_pickaxe", "Незеритовая кирка");
        RUSSIAN_NAMES.put("wooden_axe", "Деревянный топор");
        RUSSIAN_NAMES.put("stone_axe", "Каменный топор");
        RUSSIAN_NAMES.put("iron_axe", "Железный топор");
        RUSSIAN_NAMES.put("golden_axe", "Золотой топор");
        RUSSIAN_NAMES.put("diamond_axe", "Алмазный топор");
        RUSSIAN_NAMES.put("netherite_axe", "Незеритовый топор");
        RUSSIAN_NAMES.put("wooden_shovel", "Деревянная лопата");
        RUSSIAN_NAMES.put("stone_shovel", "Каменная лопата");
        RUSSIAN_NAMES.put("iron_shovel", "Железная лопата");
        RUSSIAN_NAMES.put("golden_shovel", "Золотая лопата");
        RUSSIAN_NAMES.put("diamond_shovel", "Алмазная лопата");
        RUSSIAN_NAMES.put("netherite_shovel", "Незеритовая лопата");
        RUSSIAN_NAMES.put("wooden_hoe", "Деревянная мотыга");
        RUSSIAN_NAMES.put("stone_hoe", "Каменная мотыга");
        RUSSIAN_NAMES.put("iron_hoe", "Железная мотыга");
        RUSSIAN_NAMES.put("golden_hoe", "Золотая мотыга");
        RUSSIAN_NAMES.put("diamond_hoe", "Алмазная мотыга");
        RUSSIAN_NAMES.put("netherite_hoe", "Незеритовая мотыга");
        RUSSIAN_NAMES.put("shears", "Ножницы");
        RUSSIAN_NAMES.put("flint_and_steel", "Огниво");
        RUSSIAN_NAMES.put("fishing_rod", "Удочка");
        RUSSIAN_NAMES.put("compass", "Компас");
        RUSSIAN_NAMES.put("recovery_compass", "Компас восстановления");
        RUSSIAN_NAMES.put("clock", "Часы");
        RUSSIAN_NAMES.put("spyglass", "Подзорная труба");
        RUSSIAN_NAMES.put("brush", "Кисть");
        RUSSIAN_NAMES.put("lead", "Поводок");
        RUSSIAN_NAMES.put("name_tag", "Бирка");
        RUSSIAN_NAMES.put("leather_helmet", "Кожаный шлем");
        RUSSIAN_NAMES.put("leather_chestplate", "Кожаная куртка");
        RUSSIAN_NAMES.put("leather_leggings", "Кожаные штаны");
        RUSSIAN_NAMES.put("leather_boots", "Кожаные ботинки");
        RUSSIAN_NAMES.put("chainmail_helmet", "Кольчужный шлем");
        RUSSIAN_NAMES.put("chainmail_chestplate", "Кольчужная кираса");
        RUSSIAN_NAMES.put("chainmail_leggings", "Кольчужные поножи");
        RUSSIAN_NAMES.put("chainmail_boots", "Кольчужные ботинки");
        RUSSIAN_NAMES.put("iron_helmet", "Железный шлем");
        RUSSIAN_NAMES.put("iron_chestplate", "Железная кираса");
        RUSSIAN_NAMES.put("iron_leggings", "Железные поножи");
        RUSSIAN_NAMES.put("iron_boots", "Железные ботинки");
        RUSSIAN_NAMES.put("golden_helmet", "Золотой шлем");
        RUSSIAN_NAMES.put("golden_chestplate", "Золотая кираса");
        RUSSIAN_NAMES.put("golden_leggings", "Золотые поножи");
        RUSSIAN_NAMES.put("golden_boots", "Золотые ботинки");
        RUSSIAN_NAMES.put("diamond_helmet", "Алмазный шлем");
        RUSSIAN_NAMES.put("diamond_chestplate", "Алмазная кираса");
        RUSSIAN_NAMES.put("diamond_leggings", "Алмазные поножи");
        RUSSIAN_NAMES.put("diamond_boots", "Алмазные ботинки");
        RUSSIAN_NAMES.put("netherite_helmet", "Незеритовый шлем");
        RUSSIAN_NAMES.put("netherite_chestplate", "Незеритовая кираса");
        RUSSIAN_NAMES.put("netherite_leggings", "Незеритовые поножи");
        RUSSIAN_NAMES.put("netherite_boots", "Незеритовые ботинки");
        RUSSIAN_NAMES.put("turtle_helmet", "Черепаший панцирь");
        RUSSIAN_NAMES.put("elytra", "Элитры");
        RUSSIAN_NAMES.put("shield", "Щит");
        RUSSIAN_NAMES.put("totem_of_undying", "Тотем бессмертия");
        RUSSIAN_NAMES.put("apple", "Яблоко");
        RUSSIAN_NAMES.put("golden_apple", "Золотое яблоко");
        RUSSIAN_NAMES.put("enchanted_golden_apple", "Зачарованное золотое яблоко");
        RUSSIAN_NAMES.put("bread", "Хлеб");
        RUSSIAN_NAMES.put("cooked_porkchop", "Жареная свинина");
        RUSSIAN_NAMES.put("porkchop", "Сырая свинина");
        RUSSIAN_NAMES.put("cooked_beef", "Жареная говядина");
        RUSSIAN_NAMES.put("beef", "Сырая говядина");
        RUSSIAN_NAMES.put("cooked_chicken", "Жареная курятина");
        RUSSIAN_NAMES.put("chicken", "Сырая курятина");
        RUSSIAN_NAMES.put("cooked_mutton", "Жареная баранина");
        RUSSIAN_NAMES.put("mutton", "Сырая баранина");
        RUSSIAN_NAMES.put("cooked_cod", "Жареная треска");
        RUSSIAN_NAMES.put("cod", "Сырая треска");
        RUSSIAN_NAMES.put("cooked_salmon", "Жареный лосось");
        RUSSIAN_NAMES.put("salmon", "Сырой лосось");
        RUSSIAN_NAMES.put("tropical_fish", "Тропическая рыба");
        RUSSIAN_NAMES.put("pufferfish", "Иглобрюх");
        RUSSIAN_NAMES.put("cooked_rabbit", "Жареная крольчатина");
        RUSSIAN_NAMES.put("rabbit", "Сырая крольчатина");
        RUSSIAN_NAMES.put("rabbit_stew", "Тушёный кролик");
        RUSSIAN_NAMES.put("mushroom_stew", "Тушёные грибы");
        RUSSIAN_NAMES.put("beetroot_soup", "Свекольник");
        RUSSIAN_NAMES.put("suspicious_stew", "Подозрительное рагу");
        RUSSIAN_NAMES.put("baked_potato", "Печёный картофель");
        RUSSIAN_NAMES.put("potato", "Картофель");
        RUSSIAN_NAMES.put("poisonous_potato", "Ядовитый картофель");
        RUSSIAN_NAMES.put("carrot", "Морковь");
        RUSSIAN_NAMES.put("golden_carrot", "Золотая морковь");
        RUSSIAN_NAMES.put("beetroot", "Свёкла");
        RUSSIAN_NAMES.put("melon_slice", "Ломтик арбуза");
        RUSSIAN_NAMES.put("sweet_berries", "Сладкие ягоды");
        RUSSIAN_NAMES.put("glow_berries", "Светящиеся ягоды");
        RUSSIAN_NAMES.put("chorus_fruit", "Плод хоруса");
        RUSSIAN_NAMES.put("dried_kelp", "Сушёная ламинария");
        RUSSIAN_NAMES.put("cookie", "Печенье");
        RUSSIAN_NAMES.put("cake", "Торт");
        RUSSIAN_NAMES.put("pumpkin_pie", "Тыквенный пирог");
        RUSSIAN_NAMES.put("honey_bottle", "Бутылочка мёда");
        RUSSIAN_NAMES.put("rotten_flesh", "Гнилая плоть");
        RUSSIAN_NAMES.put("spider_eye", "Паучий глаз");
        RUSSIAN_NAMES.put("fermented_spider_eye", "Ферментированный паучий глаз");
        RUSSIAN_NAMES.put("potion", "Зелье");
        RUSSIAN_NAMES.put("splash_potion", "Взрывное зелье");
        RUSSIAN_NAMES.put("lingering_potion", "Туманное зелье");
        RUSSIAN_NAMES.put("glass_bottle", "Стеклянная бутылочка");
        RUSSIAN_NAMES.put("water_bucket", "Ведро воды");
        RUSSIAN_NAMES.put("lava_bucket", "Ведро лавы");
        RUSSIAN_NAMES.put("milk_bucket", "Ведро молока");
        RUSSIAN_NAMES.put("bucket", "Ведро");
        RUSSIAN_NAMES.put("white_dye", "Белый краситель");
        RUSSIAN_NAMES.put("orange_dye", "Оранжевый краситель");
        RUSSIAN_NAMES.put("magenta_dye", "Пурпурный краситель");
        RUSSIAN_NAMES.put("light_blue_dye", "Голубой краситель");
        RUSSIAN_NAMES.put("yellow_dye", "Жёлтый краситель");
        RUSSIAN_NAMES.put("lime_dye", "Лаймовый краситель");
        RUSSIAN_NAMES.put("pink_dye", "Розовый краситель");
        RUSSIAN_NAMES.put("gray_dye", "Серый краситель");
        RUSSIAN_NAMES.put("light_gray_dye", "Светло-серый краситель");
        RUSSIAN_NAMES.put("cyan_dye", "Бирюзовый краситель");
        RUSSIAN_NAMES.put("purple_dye", "Фиолетовый краситель");
        RUSSIAN_NAMES.put("blue_dye", "Синий краситель");
        RUSSIAN_NAMES.put("brown_dye", "Коричневый краситель");
        RUSSIAN_NAMES.put("green_dye", "Зелёный краситель");
        RUSSIAN_NAMES.put("red_dye", "Красный краситель");
        RUSSIAN_NAMES.put("black_dye", "Чёрный краситель");
        RUSSIAN_NAMES.put("white_wool", "Белая шерсть");
        RUSSIAN_NAMES.put("orange_wool", "Оранжевая шерсть");
        RUSSIAN_NAMES.put("magenta_wool", "Пурпурная шерсть");
        RUSSIAN_NAMES.put("light_blue_wool", "Голубая шерсть");
        RUSSIAN_NAMES.put("yellow_wool", "Жёлтая шерсть");
        RUSSIAN_NAMES.put("lime_wool", "Лаймовая шерсть");
        RUSSIAN_NAMES.put("pink_wool", "Розовая шерсть");
        RUSSIAN_NAMES.put("gray_wool", "Серая шерсть");
        RUSSIAN_NAMES.put("light_gray_wool", "Светло-серая шерсть");
        RUSSIAN_NAMES.put("cyan_wool", "Бирюзовая шерсть");
        RUSSIAN_NAMES.put("purple_wool", "Фиолетовая шерсть");
        RUSSIAN_NAMES.put("blue_wool", "Синяя шерсть");
        RUSSIAN_NAMES.put("brown_wool", "Коричневая шерсть");
        RUSSIAN_NAMES.put("green_wool", "Зелёная шерсть");
        RUSSIAN_NAMES.put("red_wool", "Красная шерсть");
        RUSSIAN_NAMES.put("black_wool", "Чёрная шерсть");
        RUSSIAN_NAMES.put("glass", "Стекло");
        RUSSIAN_NAMES.put("white_stained_glass", "Белое стекло");
        RUSSIAN_NAMES.put("orange_stained_glass", "Оранжевое стекло");
        RUSSIAN_NAMES.put("magenta_stained_glass", "Пурпурное стекло");
        RUSSIAN_NAMES.put("light_blue_stained_glass", "Голубое стекло");
        RUSSIAN_NAMES.put("yellow_stained_glass", "Жёлтое стекло");
        RUSSIAN_NAMES.put("lime_stained_glass", "Лаймовое стекло");
        RUSSIAN_NAMES.put("pink_stained_glass", "Розовое стекло");
        RUSSIAN_NAMES.put("gray_stained_glass", "Серое стекло");
        RUSSIAN_NAMES.put("light_gray_stained_glass", "Светло-серое стекло");
        RUSSIAN_NAMES.put("cyan_stained_glass", "Бирюзовое стекло");
        RUSSIAN_NAMES.put("purple_stained_glass", "Фиолетовое стекло");
        RUSSIAN_NAMES.put("blue_stained_glass", "Синее стекло");
        RUSSIAN_NAMES.put("brown_stained_glass", "Коричневое стекло");
        RUSSIAN_NAMES.put("green_stained_glass", "Зелёное стекло");
        RUSSIAN_NAMES.put("red_stained_glass", "Красное стекло");
        RUSSIAN_NAMES.put("black_stained_glass", "Чёрное стекло");
        RUSSIAN_NAMES.put("tinted_glass", "Тонированное стекло");
        RUSSIAN_NAMES.put("glass_pane", "Стеклянная панель");
        RUSSIAN_NAMES.put("redstone_block", "Блок красного камня");
        RUSSIAN_NAMES.put("redstone_torch", "Факел из красного камня");
        RUSSIAN_NAMES.put("redstone_lamp", "Лампа из красного камня");
        RUSSIAN_NAMES.put("repeater", "Повторитель");
        RUSSIAN_NAMES.put("comparator", "Компаратор");
        RUSSIAN_NAMES.put("piston", "Поршень");
        RUSSIAN_NAMES.put("sticky_piston", "Липкий поршень");
        RUSSIAN_NAMES.put("observer", "Наблюдатель");
        RUSSIAN_NAMES.put("hopper", "Воронка");
        RUSSIAN_NAMES.put("dropper", "Выбрасыватель");
        RUSSIAN_NAMES.put("dispenser", "Раздатчик");
        RUSSIAN_NAMES.put("lever", "Рычаг");
        RUSSIAN_NAMES.put("tripwire_hook", "Крюк натяжной");
        RUSSIAN_NAMES.put("daylight_detector", "Датчик дневного света");
        RUSSIAN_NAMES.put("target", "Мишень");
        RUSSIAN_NAMES.put("lightning_rod", "Громоотвод");
        RUSSIAN_NAMES.put("tnt", "ТНТ");
        RUSSIAN_NAMES.put("sculk_sensor", "Скалк-датчик");
        RUSSIAN_NAMES.put("calibrated_sculk_sensor", "Калиброванный скалк-датчик");
        RUSSIAN_NAMES.put("sculk_shrieker", "Скалк-крикун");
        RUSSIAN_NAMES.put("sculk_catalyst", "Скалк-катализатор");
        RUSSIAN_NAMES.put("sculk", "Скалк");
        RUSSIAN_NAMES.put("crafting_table", "Верстак");
        RUSSIAN_NAMES.put("furnace", "Печь");
        RUSSIAN_NAMES.put("blast_furnace", "Плавильная печь");
        RUSSIAN_NAMES.put("smoker", "Коптильня");
        RUSSIAN_NAMES.put("anvil", "Наковальня");
        RUSSIAN_NAMES.put("chipped_anvil", "Слегка повреждённая наковальня");
        RUSSIAN_NAMES.put("damaged_anvil", "Сильно повреждённая наковальня");
        RUSSIAN_NAMES.put("enchanting_table", "Стол зачарований");
        RUSSIAN_NAMES.put("brewing_stand", "Варочная стойка");
        RUSSIAN_NAMES.put("cauldron", "Котёл");
        RUSSIAN_NAMES.put("beacon", "Маяк");
        RUSSIAN_NAMES.put("conduit", "Проводник");
        RUSSIAN_NAMES.put("lodestone", "Магнетит");
        RUSSIAN_NAMES.put("respawn_anchor", "Якорь возрождения");
        RUSSIAN_NAMES.put("grindstone", "Точило");
        RUSSIAN_NAMES.put("stonecutter", "Камнерез");
        RUSSIAN_NAMES.put("cartography_table", "Стол картографа");
        RUSSIAN_NAMES.put("fletching_table", "Стол лучника");
        RUSSIAN_NAMES.put("smithing_table", "Стол кузнеца");
        RUSSIAN_NAMES.put("loom", "Ткацкий станок");
        RUSSIAN_NAMES.put("lectern", "Кафедра");
        RUSSIAN_NAMES.put("composter", "Компостер");
        RUSSIAN_NAMES.put("barrel", "Бочка");
        RUSSIAN_NAMES.put("chest", "Сундук");
        RUSSIAN_NAMES.put("ender_chest", "Сундук Края");
        RUSSIAN_NAMES.put("trapped_chest", "Сундук-ловушка");
        RUSSIAN_NAMES.put("shulker_box", "шалкер");
        RUSSIAN_NAMES.put("jukebox", "Проигрыватель");
        RUSSIAN_NAMES.put("note_block", "Нотный блок");
        RUSSIAN_NAMES.put("bell", "Колокол");
        RUSSIAN_NAMES.put("campfire", "Костёр");
        RUSSIAN_NAMES.put("soul_campfire", "Костёр душ");
        RUSSIAN_NAMES.put("torch", "Факел");
        RUSSIAN_NAMES.put("soul_torch", "Факел душ");
        RUSSIAN_NAMES.put("lantern", "Фонарь");
        RUSSIAN_NAMES.put("soul_lantern", "Фонарь душ");
        RUSSIAN_NAMES.put("end_rod", "Стержень Края");
        RUSSIAN_NAMES.put("candle", "Свеча");
        RUSSIAN_NAMES.put("bookshelf", "Книжная полка");
        RUSSIAN_NAMES.put("chiseled_bookshelf", "Резная книжная полка");
        RUSSIAN_NAMES.put("decorated_pot", "Декоративный горшок");
        RUSSIAN_NAMES.put("white_bed", "Белая кровать");
        RUSSIAN_NAMES.put("orange_bed", "Оранжевая кровать");
        RUSSIAN_NAMES.put("magenta_bed", "Пурпурная кровать");
        RUSSIAN_NAMES.put("light_blue_bed", "Голубая кровать");
        RUSSIAN_NAMES.put("yellow_bed", "Жёлтая кровать");
        RUSSIAN_NAMES.put("lime_bed", "Лаймовая кровать");
        RUSSIAN_NAMES.put("pink_bed", "Розовая кровать");
        RUSSIAN_NAMES.put("gray_bed", "Серая кровать");
        RUSSIAN_NAMES.put("light_gray_bed", "Светло-серая кровать");
        RUSSIAN_NAMES.put("cyan_bed", "Бирюзовая кровать");
        RUSSIAN_NAMES.put("purple_bed", "Фиолетовая кровать");
        RUSSIAN_NAMES.put("blue_bed", "Синяя кровать");
        RUSSIAN_NAMES.put("brown_bed", "Коричневая кровать");
        RUSSIAN_NAMES.put("green_bed", "Зелёная кровать");
        RUSSIAN_NAMES.put("red_bed", "Красная кровать");
        RUSSIAN_NAMES.put("black_bed", "Чёрная кровать");
        RUSSIAN_NAMES.put("oak_door", "Дубовая дверь");
        RUSSIAN_NAMES.put("spruce_door", "Еловая дверь");
        RUSSIAN_NAMES.put("birch_door", "Берёзовая дверь");
        RUSSIAN_NAMES.put("jungle_door", "Тропическая дверь");
        RUSSIAN_NAMES.put("acacia_door", "Акациевая дверь");
        RUSSIAN_NAMES.put("dark_oak_door", "Дверь из тёмного дуба");
        RUSSIAN_NAMES.put("mangrove_door", "Мангровая дверь");
        RUSSIAN_NAMES.put("cherry_door", "Вишнёвая дверь");
        RUSSIAN_NAMES.put("bamboo_door", "Бамбуковая дверь");
        RUSSIAN_NAMES.put("crimson_door", "Багровая дверь");
        RUSSIAN_NAMES.put("warped_door", "Искажённая дверь");
        RUSSIAN_NAMES.put("iron_door", "Железная дверь");
        RUSSIAN_NAMES.put("oak_stairs", "Дубовые ступеньки");
        RUSSIAN_NAMES.put("cobblestone_stairs", "Ступеньки из булыжника");
        RUSSIAN_NAMES.put("stone_stairs", "Каменные ступеньки");
        RUSSIAN_NAMES.put("oak_slab", "Дубовая плита");
        RUSSIAN_NAMES.put("stone_slab", "Каменная плита");
        RUSSIAN_NAMES.put("cobblestone_slab", "Плита из булыжника");
        RUSSIAN_NAMES.put("oak_sapling", "Дубовый саженец");
        RUSSIAN_NAMES.put("spruce_sapling", "Еловый саженец");
        RUSSIAN_NAMES.put("birch_sapling", "Берёзовый саженец");
        RUSSIAN_NAMES.put("jungle_sapling", "Тропический саженец");
        RUSSIAN_NAMES.put("acacia_sapling", "Акациевый саженец");
        RUSSIAN_NAMES.put("dark_oak_sapling", "Саженец тёмного дуба");
        RUSSIAN_NAMES.put("cherry_sapling", "Вишнёвый саженец");
        RUSSIAN_NAMES.put("mangrove_propagule", "Мангровый отросток");
        RUSSIAN_NAMES.put("bamboo", "Бамбук");
        RUSSIAN_NAMES.put("sugar_cane", "Сахарный тростник");
        RUSSIAN_NAMES.put("cactus", "Кактус");
        RUSSIAN_NAMES.put("kelp", "Ламинария");
        RUSSIAN_NAMES.put("vine", "Лианы");
        RUSSIAN_NAMES.put("lily_pad", "Кувшинка");
        RUSSIAN_NAMES.put("sunflower", "Подсолнух");
        RUSSIAN_NAMES.put("dandelion", "Одуванчик");
        RUSSIAN_NAMES.put("poppy", "Мак");
        RUSSIAN_NAMES.put("blue_orchid", "Синяя орхидея");
        RUSSIAN_NAMES.put("allium", "Лук-батун");
        RUSSIAN_NAMES.put("azure_bluet", "Голубая звёздочка");
        RUSSIAN_NAMES.put("red_tulip", "Красный тюльпан");
        RUSSIAN_NAMES.put("orange_tulip", "Оранжевый тюльпан");
        RUSSIAN_NAMES.put("white_tulip", "Белый тюльпан");
        RUSSIAN_NAMES.put("pink_tulip", "Розовый тюльпан");
        RUSSIAN_NAMES.put("oxeye_daisy", "Ромашка");
        RUSSIAN_NAMES.put("cornflower", "Василёк");
        RUSSIAN_NAMES.put("lily_of_the_valley", "Ландыш");
        RUSSIAN_NAMES.put("torchflower", "Факелоцвет");
        RUSSIAN_NAMES.put("pitcher_plant", "Кувшиночник");
        RUSSIAN_NAMES.put("wither_rose", "Роза увядания");
        RUSSIAN_NAMES.put("spore_blossom", "Споровый цветок");
        RUSSIAN_NAMES.put("brown_mushroom", "Коричневый гриб");
        RUSSIAN_NAMES.put("red_mushroom", "Красный гриб");
        RUSSIAN_NAMES.put("crimson_fungus", "Багровый гриб");
        RUSSIAN_NAMES.put("warped_fungus", "Искажённый гриб");
        RUSSIAN_NAMES.put("wheat_seeds", "Семена пшеницы");
        RUSSIAN_NAMES.put("wheat", "Пшеница");
        RUSSIAN_NAMES.put("beetroot_seeds", "Семена свёклы");
        RUSSIAN_NAMES.put("melon_seeds", "Семена арбуза");
        RUSSIAN_NAMES.put("pumpkin_seeds", "Семена тыквы");
        RUSSIAN_NAMES.put("torchflower_seeds", "Семена факелоцвета");
        RUSSIAN_NAMES.put("pitcher_pod", "Стручок кувшиночника");
        RUSSIAN_NAMES.put("cocoa_beans", "Какао-бобы");
        RUSSIAN_NAMES.put("melon", "Арбуз");
        RUSSIAN_NAMES.put("pumpkin", "Тыква");
        RUSSIAN_NAMES.put("carved_pumpkin", "Вырезанная тыква");
        RUSSIAN_NAMES.put("jack_o_lantern", "Светильник Джека");
        RUSSIAN_NAMES.put("moss_block", "Блок мха");
        RUSSIAN_NAMES.put("moss_carpet", "Ковёр из мха");
        RUSSIAN_NAMES.put("hanging_roots", "Свисающие корни");
        RUSSIAN_NAMES.put("dripleaf", "Капелистник");
        RUSSIAN_NAMES.put("big_dripleaf", "Большой капелистник");
        RUSSIAN_NAMES.put("small_dripleaf", "Маленький капелистник");
        RUSSIAN_NAMES.put("azalea", "Азалия");
        RUSSIAN_NAMES.put("flowering_azalea", "Цветущая азалия");
        RUSSIAN_NAMES.put("dead_bush", "Мёртвый куст");
        RUSSIAN_NAMES.put("short_grass", "Трава");
        RUSSIAN_NAMES.put("tall_grass", "Высокая трава");
        RUSSIAN_NAMES.put("fern", "Папоротник");
        RUSSIAN_NAMES.put("large_fern", "Большой папоротник");
        RUSSIAN_NAMES.put("nether_wart", "Адский нарост");
        RUSSIAN_NAMES.put("chorus_flower", "Цветок хоруса");
        RUSSIAN_NAMES.put("chorus_plant", "Хорус");
        RUSSIAN_NAMES.put("sea_pickle", "Морской огурец");
        RUSSIAN_NAMES.put("seagrass", "Морская трава");
        RUSSIAN_NAMES.put("oak_leaves", "Дубовая листва");
        RUSSIAN_NAMES.put("spruce_leaves", "Еловая хвоя");
        RUSSIAN_NAMES.put("birch_leaves", "Берёзовая листва");
        RUSSIAN_NAMES.put("jungle_leaves", "Тропическая листва");
        RUSSIAN_NAMES.put("acacia_leaves", "Акациевая листва");
        RUSSIAN_NAMES.put("dark_oak_leaves", "Листва тёмного дуба");
        RUSSIAN_NAMES.put("mangrove_leaves", "Мангровая листва");
        RUSSIAN_NAMES.put("cherry_leaves", "Вишнёвая листва");
        RUSSIAN_NAMES.put("azalea_leaves", "Листва азалии");
        RUSSIAN_NAMES.put("flowering_azalea_leaves", "Листва цветущей азалии");
        RUSSIAN_NAMES.put("oak_boat", "Дубовая лодка");
        RUSSIAN_NAMES.put("spruce_boat", "Еловая лодка");
        RUSSIAN_NAMES.put("birch_boat", "Берёзовая лодка");
        RUSSIAN_NAMES.put("jungle_boat", "Тропическая лодка");
        RUSSIAN_NAMES.put("acacia_boat", "Акациевая лодка");
        RUSSIAN_NAMES.put("dark_oak_boat", "Лодка из тёмного дуба");
        RUSSIAN_NAMES.put("mangrove_boat", "Мангровая лодка");
        RUSSIAN_NAMES.put("cherry_boat", "Вишнёвая лодка");
        RUSSIAN_NAMES.put("bamboo_raft", "Бамбуковый плот");
        RUSSIAN_NAMES.put("oak_chest_boat", "Дубовая лодка с сундуком");
        RUSSIAN_NAMES.put("minecart", "Вагонетка");
        RUSSIAN_NAMES.put("chest_minecart", "Вагонетка с сундуком");
        RUSSIAN_NAMES.put("hopper_minecart", "Вагонетка с воронкой");
        RUSSIAN_NAMES.put("tnt_minecart", "Вагонетка с ТНТ");
        RUSSIAN_NAMES.put("furnace_minecart", "Вагонетка с печью");
        RUSSIAN_NAMES.put("rail", "Рельсы");
        RUSSIAN_NAMES.put("powered_rail", "Электрические рельсы");
        RUSSIAN_NAMES.put("detector_rail", "Нажимные рельсы");
        RUSSIAN_NAMES.put("activator_rail", "Активирующие рельсы");
        RUSSIAN_NAMES.put("saddle", "Седло");
        RUSSIAN_NAMES.put("horse_armor", "Конская броня");
        RUSSIAN_NAMES.put("iron_horse_armor", "Железная конская броня");
        RUSSIAN_NAMES.put("golden_horse_armor", "Золотая конская броня");
        RUSSIAN_NAMES.put("diamond_horse_armor", "Алмазная конская броня");
        RUSSIAN_NAMES.put("oak_sign", "Дубовая табличка");
        RUSSIAN_NAMES.put("spruce_sign", "Еловая табличка");
        RUSSIAN_NAMES.put("birch_sign", "Берёзовая табличка");
        RUSSIAN_NAMES.put("jungle_sign", "Тропическая табличка");
        RUSSIAN_NAMES.put("acacia_sign", "Акациевая табличка");
        RUSSIAN_NAMES.put("dark_oak_sign", "Табличка из тёмного дуба");
        RUSSIAN_NAMES.put("mangrove_sign", "Мангровая табличка");
        RUSSIAN_NAMES.put("cherry_sign", "Вишнёвая табличка");
        RUSSIAN_NAMES.put("bamboo_sign", "Бамбуковая табличка");
        RUSSIAN_NAMES.put("crimson_sign", "Багровая табличка");
        RUSSIAN_NAMES.put("warped_sign", "Искажённая табличка");
        RUSSIAN_NAMES.put("oak_hanging_sign", "Подвесная дубовая табличка");
        RUSSIAN_NAMES.put("oak_fence", "Дубовый забор");
        RUSSIAN_NAMES.put("spruce_fence", "Еловый забор");
        RUSSIAN_NAMES.put("birch_fence", "Берёзовый забор");
        RUSSIAN_NAMES.put("oak_fence_gate", "Дубовые ворота");
        RUSSIAN_NAMES.put("nether_brick_fence", "Адский кирпичный забор");
        RUSSIAN_NAMES.put("iron_bars", "Железная решётка");
        RUSSIAN_NAMES.put("stone_button", "Каменная кнопка");
        RUSSIAN_NAMES.put("oak_button", "Дубовая кнопка");
        RUSSIAN_NAMES.put("polished_blackstone_button", "Кнопка из полированного чернита");
        RUSSIAN_NAMES.put("stone_pressure_plate", "Каменная нажимная пластина");
        RUSSIAN_NAMES.put("oak_pressure_plate", "Дубовая нажимная пластина");
        RUSSIAN_NAMES.put("light_weighted_pressure_plate", "Лёгкая нажимная пластина");
        RUSSIAN_NAMES.put("heavy_weighted_pressure_plate", "Тяжёлая нажимная пластина");
        RUSSIAN_NAMES.put("oak_trapdoor", "Дубовый люк");
        RUSSIAN_NAMES.put("iron_trapdoor", "Железный люк");
        RUSSIAN_NAMES.put("ladder", "Лестница");
        RUSSIAN_NAMES.put("scaffolding", "Строительные леса");
        RUSSIAN_NAMES.put("snow_block", "Снежный блок");
        RUSSIAN_NAMES.put("snowball", "Снежок");
        RUSSIAN_NAMES.put("snow", "Снег");
        RUSSIAN_NAMES.put("ice", "Лёд");
        RUSSIAN_NAMES.put("packed_ice", "Плотный лёд");
        RUSSIAN_NAMES.put("blue_ice", "Синий лёд");
        RUSSIAN_NAMES.put("frosted_ice", "Замороженный лёд");
        RUSSIAN_NAMES.put("sponge", "Губка");
        RUSSIAN_NAMES.put("wet_sponge", "Мокрая губка");
        RUSSIAN_NAMES.put("cobweb", "Паутина");
        RUSSIAN_NAMES.put("bedrock", "Коренная порода");
        RUSSIAN_NAMES.put("spawner", "Спавнер");
        RUSSIAN_NAMES.put("farmland", "Грядка");
        RUSSIAN_NAMES.put("dirt_path", "Тропинка");
        RUSSIAN_NAMES.put("mycelium", "Мицелий");
        RUSSIAN_NAMES.put("end_portal_frame", "Рамка портала Края");
        RUSSIAN_NAMES.put("dragon_egg", "Яйцо дракона");
        RUSSIAN_NAMES.put("nether_portal", "Портал в Нижний мир");
        RUSSIAN_NAMES.put("end_crystal", "Кристалл Края");
        RUSSIAN_NAMES.put("armor_stand", "Стойка для брони");
        RUSSIAN_NAMES.put("item_frame", "Рамка");
        RUSSIAN_NAMES.put("glow_item_frame", "Светящаяся рамка");
        RUSSIAN_NAMES.put("painting", "Картина");
        RUSSIAN_NAMES.put("map", "Карта");
        RUSSIAN_NAMES.put("filled_map", "Заполненная карта");
        RUSSIAN_NAMES.put("writable_book", "Книга с пером");
        RUSSIAN_NAMES.put("written_book", "Написанная книга");
        RUSSIAN_NAMES.put("enchanted_book", "Зачарованная книга");
        RUSSIAN_NAMES.put("knowledge_book", "Книга знаний");
        RUSSIAN_NAMES.put("firework_rocket", "Пиротехническая ракета");
        RUSSIAN_NAMES.put("firework_star", "Пиротехническая звезда");
        RUSSIAN_NAMES.put("fire_charge", "Огненный заряд");
        RUSSIAN_NAMES.put("egg", "Яйцо");
        RUSSIAN_NAMES.put("wheat_block", "Блок пшеницы");
        RUSSIAN_NAMES.put("skeleton_skull", "Череп скелета");
        RUSSIAN_NAMES.put("wither_skeleton_skull", "Череп визер-скелета");
        RUSSIAN_NAMES.put("zombie_head", "Голова зомби");
        RUSSIAN_NAMES.put("player_head", "Голова игрока");
        RUSSIAN_NAMES.put("creeper_head", "Голова крипера");
        RUSSIAN_NAMES.put("dragon_head", "Голова дракона");
        RUSSIAN_NAMES.put("piglin_head", "Голова пиглина");
        RUSSIAN_NAMES.put("zombie_spawn_egg", "Яйцо призыва зомби");
        RUSSIAN_NAMES.put("zombie_villager_spawn_egg","яйцо призыва зомби-крестьянина");
        RUSSIAN_NAMES.put("skeleton_spawn_egg", "Яйцо призыва скелета");
        RUSSIAN_NAMES.put("creeper_spawn_egg", "Яйцо призыва крипера");
        RUSSIAN_NAMES.put("spider_spawn_egg", "Яйцо призыва паука");
        RUSSIAN_NAMES.put("enderman_spawn_egg", "Яйцо призыва эндермена");
        RUSSIAN_NAMES.put("villager_spawn_egg", "Яйцо призыва жителя");
        RUSSIAN_NAMES.put("cow_spawn_egg", "Яйцо призыва коровы");
        RUSSIAN_NAMES.put("pig_spawn_egg", "Яйцо призыва свиньи");
        RUSSIAN_NAMES.put("sheep_spawn_egg", "Яйцо призыва овцы");
        RUSSIAN_NAMES.put("chicken_spawn_egg", "Яйцо призыва курицы");
        RUSSIAN_NAMES.put("wolf_spawn_egg", "Яйцо призыва волка");
        RUSSIAN_NAMES.put("cat_spawn_egg", "Яйцо призыва кошки");
        RUSSIAN_NAMES.put("horse_spawn_egg", "Яйцо призыва лошади");
        RUSSIAN_NAMES.put("iron_golem_spawn_egg", "Яйцо призыва железного голема");
        RUSSIAN_NAMES.put("white_banner", "Белый флаг");
        RUSSIAN_NAMES.put("red_banner", "Красный флаг");
        RUSSIAN_NAMES.put("blue_banner", "Синий флаг");
        RUSSIAN_NAMES.put("black_banner", "Чёрный флаг");
        RUSSIAN_NAMES.put("green_banner", "Зелёный флаг");
        RUSSIAN_NAMES.put("white_carpet", "Белый ковёр");
        RUSSIAN_NAMES.put("red_carpet", "Красный ковёр");
        RUSSIAN_NAMES.put("white_concrete", "Белый бетон");
        RUSSIAN_NAMES.put("orange_concrete", "Оранжевый бетон");
        RUSSIAN_NAMES.put("magenta_concrete", "Пурпурный бетон");
        RUSSIAN_NAMES.put("light_blue_concrete", "Голубой бетон");
        RUSSIAN_NAMES.put("yellow_concrete", "Жёлтый бетон");
        RUSSIAN_NAMES.put("lime_concrete", "Лаймовый бетон");
        RUSSIAN_NAMES.put("pink_concrete", "Розовый бетон");
        RUSSIAN_NAMES.put("gray_concrete", "Серый бетон");
        RUSSIAN_NAMES.put("light_gray_concrete", "Светло-серый бетон");
        RUSSIAN_NAMES.put("cyan_concrete", "Бирюзовый бетон");
        RUSSIAN_NAMES.put("purple_concrete", "Фиолетовый бетон");
        RUSSIAN_NAMES.put("blue_concrete", "Синий бетон");
        RUSSIAN_NAMES.put("brown_concrete", "Коричневый бетон");
        RUSSIAN_NAMES.put("green_concrete", "Зелёный бетон");
        RUSSIAN_NAMES.put("red_concrete", "Красный бетон");
        RUSSIAN_NAMES.put("black_concrete", "Чёрный бетон");
        RUSSIAN_NAMES.put("white_concrete_powder", "Белый цемент");
        RUSSIAN_NAMES.put("white_terracotta", "Белая терракота");
        RUSSIAN_NAMES.put("orange_terracotta", "Оранжевая терракота");
        RUSSIAN_NAMES.put("magenta_terracotta", "Пурпурная терракота");
        RUSSIAN_NAMES.put("light_blue_terracotta", "Голубая терракота");
        RUSSIAN_NAMES.put("yellow_terracotta", "Жёлтая терракота");
        RUSSIAN_NAMES.put("lime_terracotta", "Лаймовая терракота");
        RUSSIAN_NAMES.put("pink_terracotta", "Розовая терракота");
        RUSSIAN_NAMES.put("gray_terracotta", "Серая терракота");
        RUSSIAN_NAMES.put("light_gray_terracotta", "Светло-серая терракота");
        RUSSIAN_NAMES.put("cyan_terracotta", "Бирюзовая терракота");
        RUSSIAN_NAMES.put("purple_terracotta", "Фиолетовая терракота");
        RUSSIAN_NAMES.put("blue_terracotta", "Синяя терракота");
        RUSSIAN_NAMES.put("brown_terracotta", "Коричневая терракота");
        RUSSIAN_NAMES.put("green_terracotta", "Зелёная терракота");
        RUSSIAN_NAMES.put("red_terracotta", "Красная терракота");
        RUSSIAN_NAMES.put("black_terracotta", "Чёрная терракота");
        RUSSIAN_NAMES.put("white_glazed_terracotta", "Белая глазурованная терракота");
        RUSSIAN_NAMES.put("white_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("orange_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("magenta_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("light_blue_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("yellow_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("lime_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("pink_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("gray_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("light_gray_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("cyan_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("purple_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("blue_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("brown_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("green_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("red_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("black_shulker_box", "шалкер");
        RUSSIAN_NAMES.put("music_disc_13", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_cat", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_blocks", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_chirp", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_far", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_mall", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_mellohi", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_stal", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_strad", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_ward", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_11", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_wait", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_otherside", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_5", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_pigstep", "Музыкальный диск");
        RUSSIAN_NAMES.put("music_disc_relic", "Музыкальный диск");
        RUSSIAN_NAMES.put("netherite_upgrade_smithing_template", "Шаблон кузнеца: незерит");
        RUSSIAN_NAMES.put("coast_armor_trim_smithing_template", "Шаблон кузнеца: берег");
        RUSSIAN_NAMES.put("dune_armor_trim_smithing_template", "Шаблон кузнеца: дюна");
        RUSSIAN_NAMES.put("eye_armor_trim_smithing_template", "Шаблон кузнеца: глаз");
        RUSSIAN_NAMES.put("host_armor_trim_smithing_template", "Шаблон кузнеца: хозяин");
        RUSSIAN_NAMES.put("raiser_armor_trim_smithing_template", "Шаблон кузнеца: возвышение");
        RUSSIAN_NAMES.put("rib_armor_trim_smithing_template", "Шаблон кузнеца: ребро");
        RUSSIAN_NAMES.put("sentry_armor_trim_smithing_template", "Шаблон кузнеца: часовой");
        RUSSIAN_NAMES.put("shaper_armor_trim_smithing_template", "Шаблон кузнеца: создатель");
        RUSSIAN_NAMES.put("silence_armor_trim_smithing_template", "Шаблон кузнеца: тишина");
        RUSSIAN_NAMES.put("snout_armor_trim_smithing_template", "Шаблон кузнеца: рыло");
        RUSSIAN_NAMES.put("spire_armor_trim_smithing_template", "Шаблон кузнеца: шпиль");
        RUSSIAN_NAMES.put("tide_armor_trim_smithing_template", "Шаблон кузнеца: прилив");
        RUSSIAN_NAMES.put("vex_armor_trim_smithing_template", "Шаблон кузнеца: вредина");
        RUSSIAN_NAMES.put("ward_armor_trim_smithing_template", "Шаблон кузнеца: страж");
        RUSSIAN_NAMES.put("wayfinder_armor_trim_smithing_template", "Шаблон кузнеца: следопыт");
        RUSSIAN_NAMES.put("wild_armor_trim_smithing_template", "Шаблон кузнеца: дикий");
        RUSSIAN_NAMES.put("trial_key", "Пробный ключ");
        RUSSIAN_NAMES.put("ominous_trial_key", "Зловещий пробный ключ");
        RUSSIAN_NAMES.put("wind_charge", "Заряд ветра");
        RUSSIAN_NAMES.put("breeze_rod", "Стержень бриза");
    }
    void onKey(EventKey e) {
        if (!isEnabled()) return;
        if (e.getKey() == searchItemBind.getBind() && searchItemBind.getBind() != -1) {
            searchItemInHand();
        }
    }
// on HUI PIsun sosiski
    void onChat(EventChatMessage e) {
        String msg = e.message;
        if (msg == null) return;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^/ah sell (\\d+)\\s*!\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(msg.trim());
        if (!m.matches()) return;

        long pricePerItem = Long.parseLong(m.group(1));
        ItemStack held = mc.player.getMainHandStack();
        int count = held.isEmpty() ? 1 : held.getCount();
        long total = pricePerItem * count;

        e.cancel();
        NetworkUtility.sendCommand("ah sell " + total);
    }

    private boolean suppressNextAhSell = false;


    private void searchItemInHand() {
        if (mc.player == null) return;
        ItemStack heldItem = mc.player.getMainHandStack();
        if (heldItem.isEmpty()) return;

        if (mc.currentScreen != null) mc.setScreen(null);

        String crusherName = checkIfCrusherItem(heldItem);
        if (crusherName != null) {
            NetworkUtility.sendCommand("ah search " + crusherName);
            return;
        }

        String itemName = resolveRussianName(heldItem);
        if (!itemName.isEmpty()) {
            String cleanName = cleanForSearch(itemName);
            if (!cleanName.isEmpty()) {
                NetworkUtility.sendCommand("ah search " + cleanName);
            }
        }
    }

    private String resolveRussianName(ItemStack stack) {
        try {
            if (stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME)) {
                String customName = stripFormatting(stack.getName().getString()).trim();
                if (!customName.isEmpty()) {
                    return customName;
                }
            }
            String displayName = stripFormatting(stack.getName().getString()).trim();
            String defaultName = stripFormatting(stack.getItem().getName().getString()).trim();
            if (!displayName.equals(defaultName) && !displayName.isEmpty()) {
                return displayName;
            }
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            String path = itemId.getPath();
            String mapped = RUSSIAN_NAMES.get(path);
            if (mapped != null && !mapped.isEmpty()) {
                return mapped;
            }
            String translationKey = stack.getItem().getTranslationKey();
            String translated = net.minecraft.client.resource.language.I18n.translate(translationKey);
            if (!translated.equals(translationKey) && !translated.isEmpty()) {
                String cleaned = stripFormatting(translated).trim();
                if (!cleaned.isEmpty() && containsCyrillic(cleaned)) {
                    return cleaned;
                }
            }
            if (!displayName.isEmpty()) {
                return displayName;
            }
            return "";
        } catch (Throwable t) {
            return "";
        }
    }
    private String cleanForSearch(String input) {
        if (input == null || input.isEmpty()) return "";
        String stripped = stripFormatting(input);
        if (stripped.contains("Пузырёк опыта") || stripped.contains("пузырёк опыта")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)\\s*Ур\\.?\\]");
            java.util.regex.Matcher matcher = pattern.matcher(stripped);
            if (matcher.find()) {
                String level = matcher.group(1);
                return "опыт " + level + "уровень";
            }
            return "опыт";
        }
        if (stripped.contains("TNT - TIER WHITE") || stripped.contains("tnt - tier white")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)\\s*Ур\\.?\\]");
            java.util.regex.Matcher matcher = pattern.matcher(stripped);
            if (matcher.find()) {
                return "таер вайт";
            }
            return "таер вайт";
        }
        if (stripped.contains("TNT - TIER BLACK") || stripped.contains("tnt - tier black")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)\\s*Ур\\.?\\]");
            java.util.regex.Matcher matcher = pattern.matcher(stripped);
            if (matcher.find()) {
                return "таер блэк";
            }
            return "таер блэк";
        }
        stripped = stripped.replaceAll("[\\[\\](){}【】『』「」〈〉《》«»]", "");
        StringBuilder sb = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (Character.isLetter(c) || c == ' ' || c == '-') {
                sb.append(c);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }
    private boolean containsCyrillic(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'А' && c <= 'я') || c == 'ё' || c == 'Ё') {
                return true;
            }
        }
        return false;
    }
    private String stripFormatting(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§') {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    public void renderCheap(DrawContext context, Slot slot) {
        if (!isEnabled()) return;
        context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, cheapSlotColor.get().getRGB());
    }
    public void renderGood(DrawContext context, Slot slot) {
        if (!isEnabled()) return;
        context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, goodSlotColor.get().getRGB());
    }
    public ColorSetting getCheapSlotColor() {
        return cheapSlotColor;
    }

    public ColorSetting getGoodSlotColor() {
        return goodSlotColor;
    }


    private String checkIfCrusherItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        AutoBuyManager manager = AutoBuy.INSTANCE.getManager();
        if (manager == null) return null;
        java.util.List<ItemBuy> allItems = new java.util.ArrayList<>();
        allItems.addAll(manager.getFuntime());
        allItems.addAll(manager.getHollyworld());

        for (ItemBuy itemBuy : allItems) {
            if (itemBuy instanceof LoreEnchantItemBuy crusherItem) {
                if (stack.getItem() != crusherItem.getItemStack().getItem()) continue;
                boolean allEnchantsMatch = true;
                for (Enchant enchant : crusherItem.getEnchants()) {
                    if (!enchant.isEnchanted(stack)) {
                        allEnchantsMatch = false;
                        break;
                    }
                }
                if (allEnchantsMatch) {
                    return crusherItem.getSearchName();
                }
            } else if (itemBuy instanceof LoreItemBuy loreItem) {
                if (loreItem.isBuy(stack)) {
                    return loreItem.getSearchName();
                }
            }
        }

        return null;
    }
}