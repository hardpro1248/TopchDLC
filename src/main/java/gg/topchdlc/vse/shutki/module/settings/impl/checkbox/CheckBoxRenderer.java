package gg.topchdlc.vse.shutki.module.settings.impl.checkbox;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class CheckBoxRenderer extends SettingRenderer<CheckBox> {

    public boolean noName = false;

    private float anim1 = 0;
    private float anim2 = 0;

    public CheckBoxRenderer(CheckBox setting) {
        super(setting);
    }

    private List<String> wrapText(String text, float maxWidth, float fontSize) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (Client.RENDERER.textWidth(testLine, TextureUse.SFMEDIUM, fontSize) <= maxWidth) {
                currentLine.append(currentLine.length() == 0 ? "" : " ").append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        float padding = 2f;
        float startX = x + padding;
        float endX = x + width - padding;

        float toggleW = 17f;
        float toggleH = 8.5f;

        if (!setting.get()) {
            anim1 = MathUtility.linearFps(anim1, 0, 10);
            if (anim1 < 0.5)
                anim2 = MathUtility.linearFps(anim2, 0, 10);
            else
                anim2 = MathUtility.linearFps(anim2, 1, 10);
        } else {
            anim1 = MathUtility.linearFps(anim1, 1, 10);
            if (anim1 > 0.5)
                anim2 = MathUtility.linearFps(anim2, 0, 10);
            else
                anim2 = MathUtility.linearFps(anim2, 1, 10);
        }

        float nameFontSize = 6.5f;
        List<String> nameLines = new ArrayList<>();
        float nameBlockH = 0;
        float lineSpacing = 7.5f;

        if (!noName) {
            float maxNameWidth = width - padding * 2 - toggleW - 6f;
            nameLines = wrapText(setting.getName(), maxNameWidth, nameFontSize);
            nameBlockH = nameLines.size() * lineSpacing;
        }

        float totalH = Math.max(14f, nameBlockH + 4f);
        this.height = (int) totalH;

        if (!noName) {
            float textStartY = y + (totalH - nameBlockH) / 2f + 1f;
            for (int i = 0; i < nameLines.size(); i++) {
                Client.RENDERER.text(nameLines.get(i), startX, textStartY + (i * lineSpacing), TextureUse.SFMEDIUM, nameFontSize, ClientColors.FORE_COLOR);
            }
        }

        float toggleX = endX - toggleW;
        float toggleY = y + (totalH / 2f) - (toggleH / 2f);

        Color accentColor = ColorUtility.injectAlpha( ClientSettings.INSTANCE.getColor(0),150);
        Color thumbColor = ColorUtility.linear(ClientColors.DARK_GRAY_COLOR, accentColor, anim1);
        Color thumbBackColor = new Color(255, 255, 255, 8);
        Color toggleBorder = new Color(255, 255, 255, 7);

        Client.RENDERER.rect(toggleX, toggleY, toggleW, toggleH, new Vector4f(4f), 1, thumbBackColor, thumbBackColor, thumbBackColor, thumbBackColor);
        Client.RENDERER.outline(toggleX, toggleY, toggleW, toggleH, 0.2f, new Vector4f(4f), new Vector2f(1), toggleBorder, toggleBorder, toggleBorder, toggleBorder);

        float knobW = 6.5f + (1.5f * anim2);
        float knobH = 6.5f;
        float knobX = toggleX + 1f + ((toggleW - knobW - 2f) * anim1);
        float knobY = toggleY + (toggleH - knobH) / 2f;

        Client.RENDERER.rect(knobX, knobY, knobW, knobH, new Vector4f(3f), 1, thumbColor, thumbColor, thumbColor, thumbColor);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (hover(mouseX, mouseY) && button == 0) {
            setting.set(!setting.get());
            return true;
        }

        return super.click(mouseX, mouseY, button);
    }
}