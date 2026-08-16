package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

import java.util.List;

public class ItemReplacer extends Module {

     public static final ItemReplacer INSTANCE = new ItemReplacer();

    public enum Models {
        ABOMINABLE_BLADE("Abominable Blade"), ABOMINABLE_GREAT_SABER("Abominable Great Saber"),
        ABOMINABLE_SCYTHE("Abominable Scythe"), ACIDIC_CLEAVER("Acidic Cleaver"),
        AMETHYST_SHURIKEN("Amethyst Shuriken"), ANCIENT_ROYAL_GREAT_SWORD("Ancient Royal Great Sword"),
        AQUATIC_SACRED_BLADE("Aquatic Sacred Blade"), ARCANETHYST("Arcanethyst"),
        ASHURA_BLADE("Ashura's Blade"), AWAKENED_LICHBLADE("Awakened Lichblade"),
        BLOOD_EDGE("Blood Edge"), BLOODY_DEATH("Bloody Death"), BRAMBLETHORN("Bramblethorn"),
        BRIMSTONE_CLAYMORE("Brimstone Claymore"), CARIAN_KNIGHTS_SWORD("Carian Knight's Sword"),
        CHRONO_BLADE("Chrono Blade"), CORRUPTED_MYTHIC_BLADE("Corrupted Mythic Blade"),
        CREATION_SPLITTER("Creation Splitter"), CRESCENT_ROSE("Crescent Rose"),
        CYBER_KATANA("Cyber Katana"), CYBER_MANTIS_BLADE("Cyber Mantis Blade"),
        CYBER_SWORD("Cyber Sword"), CYBERNETIC_CHAINSAW_BLADE("Cybernetic Chainsaw Blade"),
        CYBERNETIC_KATANA("Cybernetic Katana"), CYBERNETIC_KNIFE("Cybernetic Knife"),
        DAINSLEIF("Dainsleif"), DARK_BLADE("Dark Blade"), DARK_CLEAVER("Dark Cleaver"),
        DEATH_KNIGHTS_DAGGER("Death Knight's Dagger"), DEATH_KNIGHTS_SWORD("Death Knight's Sword"),
        DEMIGODS_UNHOLY_BLADE("Demigod's Unholy Blade"), DEMIGODS_UNHOLY_HALBERD("Demigod's Unholy Halberd"),
        DEMON_LORDS_GREAT_AXE("Demon Lord's Great Axe"), DEMON_LORDS_SWORD("Demon Lord's Sword"),
        DEMONIC_BLADE("Demonic Blade"), DEMONIC_CLEAVER("Demonic Cleaver"),
        DIVINE_AXE_RHITTA("Divine Axe Rhitta"), DIVINE_JUSTICE("Divine Justice"),
        DIVINE_PUNISHER("Divine Punisher"), DIVINE_REAPER("Divine Reaper"),
        DRAGON_SLAYING_BLADE("Dragon Slaying blade"), EDGE_OF_THE_ASTRAL_PLANE("Edge Of The Astral Plane"),
        EMBERBLADE("Emberblade"), ENIGMA("Enigma"), EPIC_SWORD("Epic Sword"), ESTOC("Estoc"),
        FALLEN_GODS_SPEAR("Fallen God's Spear"), FALLEN_GODS_SWORD("Fallen God's Sword"),
        FLORAL_LONGSWORD("Floral Longsword"), FLORAL_SABRE("Floral Sabre"),
        FOREST_GUARDIANS_GLAIVE("Forest Guardian's Glaive"), FROST_AXE("Frost Axe"),
        FROST_BLADE("Frost Blade"), FROST_SCYTHE("Frost Scythe"), HEARTHFLAME("Hearthflame"),
        HERO_SWORD("Hero Sword"), HOLY_MOONLIGHT_SWORD("Holy Moonlight Sword"),
        HORNETS_NEEDLE("Hornet's Needle"), ICEWHISPER("Icewhisper"), JADE_HALBERD("Jade Halberd"),
        KATANA("Katana"), LEGENDARY_SWORD("Legendary Sword"), LONGSWORD("Longsword"),
        MAGI_SCYTHE("Magi Scythe"), MASAMUNE("Masamune"), MJOLNIR("Mjolnir"),
        MOLTEN_BLADE("Molten Blade"), MOLTEN_SWORD("Molten Sword"), MURAMASA("Muramasa"),
        MYSTICAL_SPELLBLADE("Mystical Spellblade"), MYTHIC_BLADE("Mythic Blade"),
        OCEANS_RAGE("Ocean's Rage"), PARTISAN("Partisan"), PHARAOHS_TREASURE("Pharaoh's Treasure"),
        PHEONIX_GRACE("Pheonix Grace"), PLAGUE_LONGSWORD("Plague Longsword"),
        POWER_FUSE_HAMMER("Power Fuse Hammer"), POWER_FUSE_SWORD("Power Fuse Sword"),
        REQUIEM_OF_THE_NINTH_ABYSS("Requiem of the Ninth Abyss"), RIBBON_CLEAVER("Ribbon Cleaver"),
        RIGHTEOUS_RELIC("Righteous Relic"), RIVERS_OF_BLOOD("Rivers Of Blood"),
        ROYAL_CHAKRAM("Royal Chakram"), ROYAL_RAPIER("Royal Rapier"), SABRE("Sabre"),
        SCISSOR_BLADE("Scissor Blade"), SCULK_CLEAVER("Sculk Cleaver"), SCULK_SCYTHE("Sculk Scythe"),
        SCULK_SWORD("Sculk Sword"), SENTINELS_WILL("Sentinel's Will"), SILVERINE_BLADE("Silverine Blade"),
        SOUL_CLAWS("Soul Claws"), SOUL_COLLECTOR("Soul Collector"), SOUL_DEVOURER("Soul Devourer"),
        SOUL_EDGE("Soul Edge"), SOUL_HARVESTER("Soul Harvester"), SOUL_STEALER("Soul Stealer"),
        SOULRENDER("Soulrender"), STARS_EDGE("Star's Edge"), STEEL_SWORD("Steel Sword"),
        STOP_SIGN("Stop Sign"), STORM_BRINGER("Storm Bringer"), STORM_EDGE("Storm's Edge"),
        SUNBREAK("Sunbreak"), TENGENS_BLADE("Tengen's Blade"), TERRA_BLADE("Terra Blade"),
        THOUSAND_DEMON_DAGGERS("Thousand Demon Daggers"), THUNDER_BRINGER("Thunder Bringer"),
        THUNDERBRAND("Thunderbrand"), TRUE_EXCALIBUR("True Excalibur"),
        VAMPIRIC_NEEDLE("Vampiric Needle"), WAKIZASHI("Wakizashi"), WATCHER_CLAYMORE("Watcher Claymore"),
        WATCHING_WARGLAIVE("Watching Warglaive"), WAXWEAVER("Waxweaver"),
        WHISPERWIND("Whisperwind"), WICKPIERCER("Wickpiercer"), WRAITH_SCYTHE("Wraith Scythe"),
        YORU("Yoru");

        public final String modelName;
        Models(String name) { this.modelName = name; }
        @Override public String toString() { return modelName; }
    }

    private final EnumSetting<Models> selectedModel = enumSetting("Модель", Models.KATANA);

    public ItemReplacer() {
        super("SwordReplace", Category.RENDER, "xxx");
    }

    public ItemStack getRenderStack(ItemStack stack) {
        if (!isEnabled() || stack.isEmpty()) return stack;
        if ( stack.isIn(ItemTags.SWORDS)) {
            ItemStack copy = stack.copy();
            applyModel(copy);
            return copy;
        }

        return stack;
    }

    private void applyModel(ItemStack stack) {
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                List.of(),
                List.of(),
                List.of(selectedModel.get().modelName),
                List.of()
        ));
    }
}