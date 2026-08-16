package gg.topchdlc.vse.shutki.module.settings.impl.choice;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.widgets.impl.ChoiceDropdownWidget;
import gg.topchdlc.api.ui.widgets.impl.ChoiceSettingsWidget;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChoiceRenderer<C extends Choice> extends SettingRenderer<ChoiceSetting<C>> {

    public ChoiceRenderer(ChoiceSetting<C> setting) {
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

        C active = setting.get();
        boolean hasSubSettings = active != null && !active.getSettingRenderers().isEmpty();

        String valText = active != null ? active.name : "None";
        float valFontSize = 6.0f;
        float valTextWidth = Client.RENDERER.textWidth(valText, TextureUse.SFMEDIUM, valFontSize);
        float btnWidth = Math.max(38f, valTextWidth + 12f);
        float btnHeight = 11f;

        float gearWidth = hasSubSettings ? 11f : 0f;
        float gap = 3f;

        float rightBlockWidth = btnWidth + (hasSubSettings ? gearWidth + gap : 0f);
        float maxNameWidth = width - padding * 2 - rightBlockWidth - 6f;
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

        float curX = endX;

        curX -= btnWidth;
        float btnY = y + (totalH / 2f) - (btnHeight / 2f);
        boolean btnHovered = MathUtility.mouseIn(curX, btnY, btnWidth, btnHeight, mouseX, mouseY);

        Color btnBg = btnHovered ? new Color(255, 255, 255, 14) : new Color(255, 255, 255, 6);
        Color btnBorder = new Color(255, 255, 255, 10);

        Client.RENDERER.rect(curX, btnY, btnWidth, btnHeight, new Vector4f(3f), 1, btnBg, btnBg, btnBg, btnBg);
        Client.RENDERER.outline(curX, btnY, btnWidth, btnHeight, 0.2f, new Vector4f(3f), new Vector2f(1), btnBorder, btnBorder, btnBorder, btnBorder);

        float textX = curX + (btnWidth - valTextWidth) / 2f;
        float textY = btnY + (btnHeight - valFontSize) / 2f - 0.5f;
        Client.RENDERER.text(valText, textX, textY, TextureUse.SFMEDIUM, valFontSize, Color.WHITE);

        if (hasSubSettings) {
            curX -= (gearWidth + gap);
            float gearY = y + (totalH / 2f) - (btnHeight / 2f);
            boolean gearHovered = MathUtility.mouseIn(curX, gearY, gearWidth, btnHeight, mouseX, mouseY);

            Color gearBg = gearHovered ? new Color(255, 255, 255, 16) : new Color(255, 255, 255, 6);
            Color gearBorder = new Color(255, 255, 255, 10);

            Client.RENDERER.rect(curX, gearY, gearWidth, btnHeight, new Vector4f(3f), 1, gearBg, gearBg, gearBg, gearBg);
            Client.RENDERER.outline(curX, gearY, gearWidth, btnHeight, 0.2f, new Vector4f(3f), new Vector2f(1), gearBorder, gearBorder, gearBorder, gearBorder);

            Color iconColor = gearHovered ? Color.WHITE : new Color(180, 180, 185);
            Client.RENDERER.textCenteredStrict(IconUse.GEAR.glyph, curX + gearWidth / 2f, gearY + btnHeight / 1.35f, TextureUse.ICONS, 5.5f, iconColor);
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button == 0) {
            float padding = 2f;
            float endX = x + width - padding;

            C active = setting.get();
            boolean hasSubSettings = active != null && !active.getSettingRenderers().isEmpty();

            String valText = active != null ? active.name : "None";
            float valTextWidth = Client.RENDERER.textWidth(valText, TextureUse.SFMEDIUM, 6.0f);
            float btnWidth = Math.max(38f, valTextWidth + 12f);
            float btnHeight = 11f;
            float gearWidth = hasSubSettings ? 11f : 0f;
            float gap = 3f;

            float btnX = endX - btnWidth;
            float btnY = y + (height / 2f) - (btnHeight / 2f);

            if (MathUtility.mouseIn(btnX, btnY, btnWidth, btnHeight, mouseX, mouseY)) {

                float maxTextWidth = 0f;
                for (C choice : setting.getChoices()) {
                    float w = Client.RENDERER.textWidth(choice.name, TextureUse.SFMEDIUM, 5.8f);
                    if (w > maxTextWidth) maxTextWidth = w;
                }

                float dropWidth = Math.max(btnWidth, maxTextWidth + 16f);
                float dropX = Math.min(btnX + btnWidth - dropWidth, endX - dropWidth);

                ChoiceDropdownWidget<C> widget = new ChoiceDropdownWidget<>(setting, dropX, btnY + btnHeight + 2f, dropWidth);
                Client.GLASS_GUI.WIDGETS.register(widget);
                return true;
            }

            if (hasSubSettings) {
                float gearX = btnX - gearWidth - gap;
                float gearY = y + (height / 2f) - (btnHeight / 2f);

                if (MathUtility.mouseIn(gearX, gearY, gearWidth, btnHeight, mouseX, mouseY)) {
                    ChoiceSettingsWidget widget = new ChoiceSettingsWidget(active, gearX, gearY + btnHeight + 2f, 130f);
                    Client.GLASS_GUI.WIDGETS.register(widget);
                    return true;
                }
            }
        }
        return super.click(mouseX, mouseY, button);
    }
}