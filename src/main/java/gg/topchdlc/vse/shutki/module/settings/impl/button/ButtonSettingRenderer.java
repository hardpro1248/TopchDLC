package gg.topchdlc.vse.shutki.module.settings.impl.button;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import org.joml.Vector4f;

import java.awt.*;

public class ButtonSettingRenderer extends SettingRenderer<ButtonSetting> {
    private float hoverAnim = 0;

    public ButtonSettingRenderer(ButtonSetting setting) {
        super(setting);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        height = 20;
        boolean hovered = hover(mouseX, mouseY);

        Color bg = new Color(12, 12, 12, 130);
        hoverAnim = MathUtility.linearFps(hoverAnim, hovered ? 1 : 0, 10);
        Color bgColor = ColorUtility.linear(bg, bg, hoverAnim);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(5), 1, bgColor, bgColor, bgColor, bgColor);
        String text = setting.getName();
        float textX = x + width / 2f;
        float textY = y + height / 2f - 4;
        Client.RENDERER.textCentered(text, textX, textY, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button == 0 && hover(mouseX, mouseY)) {
            setting.execute();
            return true;
        }
        return false;
    }
}
