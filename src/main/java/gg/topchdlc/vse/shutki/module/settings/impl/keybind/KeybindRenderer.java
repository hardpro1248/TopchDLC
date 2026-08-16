package gg.topchdlc.vse.shutki.module.settings.impl.keybind;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Rectangle;
import lombok.Getter;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class KeybindRenderer extends SettingRenderer<KeybindSetting> {
    public KeybindRenderer(KeybindSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Getter
    private boolean binding = false;

    private final Rectangle bindbound = new Rectangle();

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

        String bind = TextUtility.keyToString(this.setting.getBind());
        float bindFontSize = 6.0f;
        float bindTextWidth = Client.RENDERER.textWidth(bind, TextureUse.SFMEDIUM, bindFontSize);

        float btnWidth = binding ? 26f : Math.max(20f, bindTextWidth + 8f);
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
        bindbound.bound(btnX, btnY, btnWidth, btnHeight);

        boolean hovered = bindbound.hovered(mouseX, mouseY);
        Color btnBg = binding ? new Color(255, 255, 255, 20) : (hovered ? new Color(255, 255, 255, 14) : new Color(255, 255, 255, 6));
        Color btnBorder = new Color(255, 255, 255, binding ? 25 : 10);

        Client.RENDERER.rect(btnX, btnY, btnWidth, btnHeight, new Vector4f(3f), 1, btnBg, btnBg, btnBg, btnBg);
        Client.RENDERER.outline(btnX, btnY, btnWidth, btnHeight, 0.2f, new Vector4f(3f), new Vector2f(1), btnBorder, btnBorder, btnBorder, btnBorder);

        if (!binding) {
            float textX = btnX + (btnWidth - bindTextWidth) / 2f;
            float textY = btnY + (btnHeight - bindFontSize) / 2f - 0.5f;
            Client.RENDERER.text(bind, textX, textY, TextureUse.SFMEDIUM, bindFontSize, Color.WHITE);
        } else {
            float pad = 4f;
            float dotSize = 2.5f;
            float startDotX = btnX + (btnWidth / 2F) - (pad * 2f) / 2F - (dotSize / 2f);
            float dotY = btnY + (btnHeight / 2F) - (dotSize / 2f);

            for (int i = 0; i < 3; i++) {
                float sin = (float) Math.sin(System.currentTimeMillis() / 200d - (i * 0.8f));
                float alphaFactor = MathUtility.clamp((sin + 1f) / 2f, 0.2f, 1f);
                Color dotColor = new Color(255, 255, 255, (int) (255 * alphaFactor));

                Client.RENDERER.rect(startDotX + (i * pad), dotY, dotSize, dotSize, new Vector4f(1.2f), 1, dotColor, dotColor, dotColor, dotColor);
            }
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (binding && button >= 0) {
            int mouseKeyCode = -100 - button;
            this.setting.bind(mouseKeyCode);
            this.binding = false;
            return true;
        }
        if (bindbound.hovered(mouseX, mouseY) && button == 0) {
            binding = true;
            return true;
        }
        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);

        if (this.binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                keyCode = -1;
            }
            this.setting.bind(keyCode);
            this.binding = false;
        }
    }
}