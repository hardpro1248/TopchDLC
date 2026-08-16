package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGetFov;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import net.minecraft.entity.effect.StatusEffects;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.mixin.accessor.IItemDisplayEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.Identifier;

public class Removals extends Module {
    private Removals() {
        super("Removals", Category.RENDER, "удалет что то ");
    }
    public static final Removals INSTANCE = new Removals();
    public MultiEnumSetting<Removal> removals = multiEnumSetting("Elements", Removal.class);
    public MultiEnumSetting<EntityKind> entities = multiEnumSetting("Entites", EntityKind.class);
    public MultiEnumSetting<Particle> particle = multiEnumSetting("particle", Particle.class);
    public MultiEnumSetting<Sound> soundMultiEnumSetting = multiEnumSetting("Sound", Sound.class);
    public SliderSetting fov = sliderSetting("FOV", 90, 0, 180).visible(() -> removals.get(Removal.FOV));

    public boolean doesNotRenderEntity(Entity entity) {
        if (entity instanceof ArmorStandEntity && entities.get(EntityKind.ArmorStand)) return true;
        if (entity instanceof DisplayEntity.ItemDisplayEntity d) {
            DisplayEntity.ItemDisplayEntity.Data data = ((IItemDisplayEntity)d).client$data();
            if (data != null) {
                Identifier model = data.itemStack().get(DataComponentTypes.ITEM_MODEL);
            }
        }
        return false;
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventGetFov e) {
            if (removals.get(Removal.FOV)) {
                if (e.fov == mc.options.getFov().getValue())
                    e.fov = fov.get();
            }
            if (removals.get(Removal.SlownessFov) && mc.player != null) {
                if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS) || mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                    e.fov = mc.options.getFov().getValue();
                }
            }
        }
    };

    @AllArgsConstructor
    @Getter
    public enum Removal implements EnumChoice {
        HurtView("Hurt view", true),
        FireOverlay("Fire overlay", true),
        BlockOverlay("Block overlay", true),
        Darkness("Darkness", true),
        Blindness("Blindness", true),
        SignText("Sign text", false),
        Armor("Armor", false),
        TotemPop("Totem pop", true),
        FOV("FOV limit", false),
        Nametags("Nametags", false),
        cameraNoClip("Camera clip",false),
        Invisibility("Invisibility", true),
        Vignette("Vignette",true),
        Vegetation("Vegetation", true),
        Poral("PortalOV",true),
        Scoreboard("Scoreboard", false),
        Nausea("Nausea", true),
        Under("water",true),
        SlownessFov("Slowness FOV", true);
        final String renderName;
        final boolean defaultEnabled;
    }

    public enum EntityKind {
        ArmorStand
    }
    @AllArgsConstructor
    @Getter
    public enum Particle implements EnumChoice {
        Bubbles("Air bar", true),
        Cam_F("CampFire",true),
        TotemP("Totem",true),
        RainP("Weather",true),
        Glow("glow",true),
        Effect("Effect",true),
        Drips("Drips",true),
        Item("Item",true),
        Other("Other",true),
        Block("BLock",true);
        final String renderName;
        private final boolean defaultEnabled;
    }

    @AllArgsConstructor
    @Getter
    public enum Sound implements EnumChoice {
        SoundWithering("Withering sound", true),
        SoundTotem("Totem sound", true),
        SoundExperience("Experience sound", true),
        SoundTRIDENT("Trident sound", true);
        final String renderName;
        private final boolean defaultEnabled;
    }

}
