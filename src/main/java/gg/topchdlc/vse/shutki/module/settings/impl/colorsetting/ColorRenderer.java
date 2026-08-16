package gg.topchdlc.vse.shutki.module.settings.impl.colorsetting;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.widgets.impl.ColorPickerWidget;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import org.joml.Vector4f;

public class ColorRenderer extends SettingRenderer<ColorSetting> {
    ColorSetting setting;

    public ColorRenderer(ColorSetting setting) {
        super(setting);
        this.setting = setting;
        this.picker = new ColorPickerWidget(setting);
    }

    ColorPickerWidget picker;

    @Override
    public void render(int mouseX, int mouseY) {
        height = Math.max(15, 15 + drawDesc(Client.RENDERER.textLined(setting.getName(), 20, x + 2, y + 5, TextureUse.SFMEDIUM, 6.5f, ClientColors.FORE_COLOR)));

        float tWidth = Math.max(30, Client.RENDERER.textWidth(setting.getName(), TextureUse.SFMEDIUM, 9f));
        Client.RENDERER.rect(x + 8 + tWidth, y + 5, 6, 6, new Vector4f(5), 2, setting.get(), setting.get(), setting.get(), setting.get());
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (hover(mouseX, mouseY)) {
            picker.getAnimation().timerUtil.setTime(picker.getAnimation().timerUtil.getTime() + 50);
            if (Client.GLASS_GUI.isOpened()) {
                Client.GLASS_GUI.WIDGETS.register(picker);
            } else {
                Client.HUD.registerOverlay(picker);
            }
            picker.bound(x + width, y + height, 140, 160);
            picker.expanded = true;
            picker.shouldRemove = false;
            return true;
        }
        return super.click(mouseX, mouseY, button);
    }
}
