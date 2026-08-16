package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.client.CustomizeSetting;


/**
 * Кастомная модель персонажа. Меняет скин игрока на выбранную модель
 * (миньон, ленивец, зомби, панда, дьявол) или на скин игрока по нику.
 * Видна только тебе и друзьям.
 */
public class CustomModel extends Module {
    public static final CustomModel INSTANCE = new CustomModel();

    public enum Model {
        Minion("minion.png"),
        Sloth("sloth.png"),
        Zombie("zombie.png"),
        Panda("panda.png"),
        Devil("devil.png");

        private final String file;
        Model(String file) { this.file = file; }
        @Override public String toString() { return name(); }
        public String getFile() { return file; }
    }

    public final EnumSetting<Model> model = enumSetting("Модель", Model.Minion);

    private CustomModel() {
        super("CustomModel", Category.RENDER, "Кастомная модель персонажа");
    }

    @Override
    protected void onEnable() {
        ClientSettings.INSTANCE.customizeSetting.customModel.set(true);
        applyModel();
    }

    @Override
    protected void onDisable() {
        ClientSettings.INSTANCE.customizeSetting.customModel.set(false);
    }

    private void applyModel() {
        try {
            for (var mode : CustomizeSetting.ModelMode.values()) {
                if (mode.name().equalsIgnoreCase(model.get().name())) {
                    ClientSettings.INSTANCE.customizeSetting.modelMode.select(mode);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
