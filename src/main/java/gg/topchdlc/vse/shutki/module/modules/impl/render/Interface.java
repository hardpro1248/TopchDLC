package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.*;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.main.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class Interface extends Module {
    public static final Interface INSTANCE = new Interface();

    public enum HudStyle {Main ,Solution}
    public final EnumSetting<HudStyle> hudStyle = enumSetting("HUD Style", HudStyle.Main)
            .onChanged(this::applyStyle);
    public final EnumSetting<Style> style = enumSetting("Стиль Хотбара", Style.GLASS).visible(()-> hudStyle.is(HudStyle.Main));
    public final MultiEnumSetting<HudElements> MainElements = add(
            new MultiEnumSetting<>("Main elements", HudElements.class) {{
                getEnumEntries().removeIf(e -> e.style != HudStyle.Main);
                value.removeIf(e -> e.style != HudStyle.Main);
            }}.visible(() -> hudStyle.is(HudStyle.Main))
    );

    public final MultiEnumSetting<HudElements> nursultanElements = add(
            new MultiEnumSetting<>("Solution elements", HudElements.class) {{
                getEnumEntries().removeIf(e -> e.style != HudStyle.Solution);
                value.removeIf(e -> e.style != HudStyle.Solution);
            }}.visible(() -> hudStyle.is(HudStyle.Solution))
    );
    private Interface() {
        super("Interface", Category.RENDER, "ХУДЭК");

        for (HudElements el : HudElements.values()) {
            Client.HUD.register(el.getElement());
        }

        MainElements.onChange((mode, enabled) -> {
            if (hudStyle.is(HudStyle.Main)) mode.getElement().setEnabled(enabled);
        });
        nursultanElements.onChange((mode, enabled) -> {
            if (hudStyle.is(HudStyle.Solution)) mode.getElement().setEnabled(enabled);
        });

        applyStyle(HudStyle.Solution);
        setEnabled(true, false);
    }

    private void applyStyle(HudStyle style) {
        for (HudElements el : HudElements.values()) {
            if (el.style == style) {
                MultiEnumSetting<HudElements> setting;
                switch (style) {
                    case Solution:
                    default:
                        setting = nursultanElements;
                        break;
                    case Main:
                        setting = MainElements;
                        break;

                }
                el.getElement().setEnabled(setting.get(el));
            } else {
                el.getElement().setEnabled(false);
            }
        }
    }

    public static HudElements findByElement(HudElement element) {
        for (HudElements e : HudElements.values()) {
            if (e.getElement() == element) return e;
        }
        return null;
    }

    @AllArgsConstructor @Getter
    public enum HudElements implements EnumChoice {
        WatermarkWidget("Watermark Widget", true, HudStyle.Main,
                new WatermarkElement(new Drag("Watermark Widget", () -> true).bound(10, 10, 80, 18))),
        HotkeysWidget("Hotkeys Widget", true, HudStyle.Main,
                new KeybindsElement(new Drag("HotKeys Widget", () -> true).bound(10, 35, 65, 14))),
        TargethudWidget("Target Widget", true, HudStyle.Main,
                new TargetHudElement(new Drag("Target Widget", () -> true).bound(5, 110, 130, 30))),
        Hotbar("Hotbar", false, HudStyle.Main, HotbarElement.INSTANCE),
        Minecraftui("Minecraft UI", false, HudStyle.Main, MinecraftUI.INSTANCE),
        Effects("Effects Widget", true, HudStyle.Main, EffectsElement.INSTANCE),
        PvPResourcesWidget("PvPResourcesWidget", true, HudStyle.Main, PvPResourcesElement.INSTANCE),
        StaffListWidget("Staff list Widget", true, HudStyle.Main,
                new StaffElement(new Drag("Staff list Widget", () -> true).bound(5, 170, 80, 18))),
        CooldownsWidget("Cooldowns Widget", true, HudStyle.Main,
                new CooldownsElement(new Drag("Cooldowns Widget", () -> true).bound(5, 200, 80, 18))),
        Debug("Debug", false, HudStyle.Main,
                new DebugElement(new Drag("Debug", () -> true).bound(5, 170, 180, 90))),
        ArmorWidget("Armor Widget", true, HudStyle.Main,
                new ArmorHudElement(new Drag("Armor Widget", () -> true).bound(5, 230, 80, 20))),
        InventoryWidget("Inventory Widget", false, HudStyle.Main,
                new InventoryHudElement(new Drag("Inventory Widget", () -> true).bound(5, 260, 200, 100))),
        BindsItemWidget("BindsItem Widget", true, HudStyle.Main,
                new BindsHudElement(new Drag("BindsItem Widget", () -> true).bound(5, 300, 100, 22))),
        CordWidget("Cord Widget",true,HudStyle.Main,
                new CordElement(new Drag("Cord Widget", () -> true).bound(30, 50, 70, 30))),
        FpsWidget("FPS Widget",true,HudStyle.Main,
                new FpsElement(new Drag("FPS Widget", () -> true).bound(10, 25, 70, 30))),
        BpsWidget("Bps Widget",true,HudStyle.Main,
                new BpsElement(new Drag("Bps Widget", () -> true).bound(30, 25, 70, 30))),
        TpsWidget("Tps Widget",true,HudStyle.Main,
                new TpsElement(new Drag("Tps Widget", () -> true).bound(10, 40, 70, 30))),


        NursultanWatermark("WaterMarK", true, HudStyle.Solution,
                new NursultanWatermark(new Drag("WatermarkNur", () -> true).bound(10, 10, 200, 33))),
        NursultanKeybinds("Hotkeys", true, HudStyle.Solution,
                new NursultanKeybindsElement(new Drag("KeybindsNur", () -> true).bound(10, 50, 80, 10))),
        NursultanTargetHud(" TargetHud", true, HudStyle.Solution,
                new NursultanTargetHudElement(new Drag("TargetHudNur", () -> true).bound(5, 110, 100, 36))),
        EffectsElementN("Effect",true,HudStyle.Solution,
                new EffectsElementN(new Drag("EffectsN", () ->true).bound(30,150,100,80))),
        StaffNurick("Staff",true,HudStyle.Solution,
                new StaffNurick(new Drag("StaffNur",()-> true).bound(15,80,40,14))),
        BindsNurick("Binds",true,HudStyle.Solution,
                new BindsNurick(new Drag("BindsNurick", ()-> true).bound(30,110,200 ,22))),
        CooldownsElementN("Cooldowns",true, HudStyle.Solution,
                new CooldownsElementN(new Drag("Cooldowns", ()-> true).bound(40, 130,210,22))),
        NursultanScoreboard("Scoreboard", true, HudStyle.Solution,
                new ScoreboardNurick(new Drag("ScoreboardNur", () -> true).bound(10, 200, 100, 100))),
        Nursultaninv("Inv",false,HudStyle.Solution,
                new NursultanInv(new Drag("Inv",() -> true).bound(7, 260, 200, 100))),
        TOtem("TotemI", true, HudStyle.Solution, TotemNUrick.INSTANCE),
        NursultanArmor(" Armor", true, HudStyle.Solution,
                new ArmorNurick(new Drag("ArmorNur", () -> true).bound(
                        (int)(mc.getWindow().getScaledWidth() / 2 + 91),
                        (int)(mc.getWindow().getScaledHeight() - 22),
                        82, 22)));


        final String renderName;
        final boolean defaultEnabled;
        final HudStyle style;
        final HudElement element;
    }
    @AllArgsConstructor
    public enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}
