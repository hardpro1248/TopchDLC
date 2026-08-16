package gg.topchdlc.vse.utils.client.client;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.impl.button.ButtonSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.text.TextSetting;
import net.minecraft.util.Identifier;

/**
 * Create by daun kvass
 */
public class CustomizeSetting extends Group {
    public CustomizeSetting() {
        super("Customize Settings");
        this.expanded(true);
        models.expanded(true);
    }

    @Override
    public void load(JsonObject json) {
        super.load(json);
        this.expanded(true);
        models.expanded(true);
    }
    public static final CustomizeSetting INSTANCE = new CustomizeSetting();
    public final Group capes = group("Capes");
    public final CheckBox cape = capes.checkbox("Custom cape",true);
    public enum CapeMode {
        BKGroup("bkgroup.png"),
        FBGroup("fbgroup.png"),
        Original("cape.png"),
        Red("cape_red.png"),
        Green("cape_green.png"),
        Cyan("cape_cyan.png"),
        Blue("cape_blue.png"),
        Purple("cape_purple.png"),
        Pink("cape_pink.png"),
        Gold("cape_gold.png"),
        White("cape_white.png");

        private final Identifier identifier;

        CapeMode(String fileName) {
            this.identifier = Identifier.of("topchdlc", "images/cape/" + fileName);
        }

        public Identifier getIdentifier() {
            return identifier;
        }
    }

    public enum PhotoMode {
        Nya("cute/nya.png"),
        smile("cute/smile.png"),
        cutie("cute/cutie.png"),
        cutie2("cute/cutie2.png"),
        boykisser("a/bdsm.png"),
        kowk("a/kowk.png"),
        rize("a/thousand_seven.png"),
        bat("cute/bat.png");

        private final Identifier identifier;

        PhotoMode(String fileName) {
            this.identifier = Identifier.of("topchdlc", "images/ui/pic/" + fileName);
        }

        public Identifier getIdentifier() {
            return identifier;
        }
    }
    public final EnumSetting<CapeMode> capeMode = capes.enumSetting("Cape Mode",CapeMode.BKGroup).visible(cape::get);

    public final Group models = group("Custom Model");
    public final CheckBox customModel = models.checkbox("Custom model", false);
    public enum ModelMode {
        Off(null), Minion("minion.png"), Sloth("sloth.png"), Zombie("zombie.png"), Panda("panda.png"), Devil("devil.png");

        private final Identifier identifier;

        ModelMode(String fileName) {
            this.identifier = fileName == null ? null : Identifier.of("topchdlc", "images/models/" + fileName);
        }

        public Identifier getIdentifier() {
            return identifier;
        }
    }
    public final EnumSetting<ModelMode> modelMode = models.enumSetting("Model",ModelMode.Off).visible(customModel::get);

    public final TextSetting skinName = models.text("Ник для скина", "");
    public final ButtonSetting applySkin = models.button("Применить скин", () -> {
        customModel.set(true);
        SkinManager.INSTANCE.applySkin(skinName.getText().trim());
    });

    public final EnumSetting<PhotoMode> photomode = enumSetting("Photo TargetHud other Entity Mode",PhotoMode.Nya);
}
