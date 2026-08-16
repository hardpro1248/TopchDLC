package gg.topchdlc.vse.shutki.module.settings.impl.enumsetting;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.widgets.impl.EnumRendererWidget;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.lang.LangUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EnumRenderer<E extends Enum<E>> extends SettingRenderer<EnumSetting<E>> {

    public EnumRenderer(EnumSetting<E> setting) {
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

        String currentValText = LangUtility.getEnumChoiceName(setting.get());
        float valFontSize = 6.0f;
        float valTextWidth = Client.RENDERER.textWidth(currentValText, TextureUse.SFMEDIUM, valFontSize);
        float btnWidth = Math.max(38f, valTextWidth + 12f);
        float btnHeight = 11f;

        float maxNameWidth = width - padding * 2 - btnWidth - 6f;
        float nameFontSize = 6.5f;
        List<String> nameLines = wrapText(setting.getName(), maxNameWidth, nameFontSize);

        float lineSpacing = 7.5f;
        float nameBlockH = nameLines.size() * lineSpacing;
        float totalH = Math.max(14f, nameBlockH + 4f);

        this.height = (int) totalH;

        float textStartY = y + (totalH - nameBlockH) / 2f + 1f;
        for (int i = 0; i < nameLines.size(); i++) {
            Client.RENDERER.text(nameLines.get(i), startX, textStartY + (i * lineSpacing), TextureUse.SFMEDIUM, nameFontSize, ClientColors.FORE_COLOR);
        }

        float btnX = endX - btnWidth;
        float btnY = y + (totalH / 2f) - (btnHeight / 2f);

        boolean hovered = MathUtility.mouseIn(btnX, btnY, btnWidth, btnHeight, mouseX, mouseY);

        Color btnBg = hovered ? new Color(255, 255, 255, 14) : new Color(255, 255, 255, 6);
        Color btnBorder = new Color(255, 255, 255, 10);

        Client.RENDERER.rect(btnX, btnY, btnWidth, btnHeight, new Vector4f(3f), 1, btnBg, btnBg, btnBg, btnBg);
        Client.RENDERER.outline(btnX, btnY, btnWidth, btnHeight, 0.2f, new Vector4f(3f), new Vector2f(1), btnBorder, btnBorder, btnBorder, btnBorder);

        float textX = btnX + (btnWidth - valTextWidth) / 2f;
        float textY = btnY + (btnHeight - valFontSize) / 2f - 0.5f;
        Client.RENDERER.text(currentValText, textX, textY, TextureUse.SFMEDIUM, valFontSize, Color.WHITE);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button == 0) {
            float padding = 2f;
            float endX = x + width - padding;

            String currentValText = LangUtility.getEnumChoiceName(setting.get());
            float valTextWidth = Client.RENDERER.textWidth(currentValText, TextureUse.SFMEDIUM, 6.0f);
            float btnWidth = Math.max(38f, valTextWidth + 12f);
            float btnHeight = 11f;

            float btnX = endX - btnWidth;
            float btnY = y + (height / 2f) - (btnHeight / 2f);

            if (MathUtility.mouseIn(btnX, btnY, btnWidth, btnHeight, mouseX, mouseY)) {
                float maxTextWidth = 0f;
                for (Enum<?> constant : setting.get().getDeclaringClass().getEnumConstants()) {
                    String cName = LangUtility.getEnumChoiceName(constant);
                    float w = Client.RENDERER.textWidth(cName, TextureUse.SFMEDIUM, 5.8f);
                    if (w > maxTextWidth) maxTextWidth = w;
                }

                float dropWidth = Math.max(btnWidth, maxTextWidth + 16f);
                float dropX = Math.min(btnX + btnWidth - dropWidth, endX - dropWidth);

                EnumRendererWidget widget = new EnumRendererWidget(this, dropX, btnY + btnHeight + 2f, dropWidth);
                if (Client.GLASS_GUI.isOpened()) {
                    Client.GLASS_GUI.WIDGETS.register(widget);
                } else {
                    Client.HUD.registerOverlay(widget);
                }
                return true;
            }
        }
        return super.click(mouseX, mouseY, button);
    }
}