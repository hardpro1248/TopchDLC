package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class KeybindsElement extends HudElement {
    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);

    public KeybindsElement(Drag drag) {
        super("Keybinds", drag);
    }

    private static final float HEIGHT = 14.5f;
    private static final float PADDING_SIDE = 4.0f;
    private static final float SPACING = 0.5f;
    private static final float TEXT_SIZE = 7.0f;
    private static final float ICON_SIZE = 8.0f;

    @Override
    public void render(int mouseX, int mouseY) {
        List<Module> activeModules = new ArrayList<>();
        for (Module module : Client.MODULES.getModules()) {
            boolean active = module.isEnabled() && module.getKey() != -1;
            module.keybindAnimation.setDirection(active ? Direction.BACKWARDS : Direction.FORWARDS);

            if (1f - module.keybindAnimation.getOutput() > 0.01f || (mc.currentScreen instanceof ChatScreen && active)) {
                activeModules.add(module);
            }
        }

        if (activeModules.isEmpty() && !(mc.currentScreen instanceof ChatScreen)) return;

        boolean isRightSide = (x + width / 2f) > (mc.getWindow().getScaledWidth() / 2f);
        float currentY = y;

        renderHeader(currentY, isRightSide);
        currentY += HEIGHT + 1.5f;

        float maxRowWidth = 85;

        for (Module module : activeModules) {
            float alpha = MathHelper.clamp(1f - module.keybindAnimation.getOutput(), 0f, 1f);
            String name = module.getName();
            String key = module.getKeyText();

            float nameW = Client.RENDERER.textWidth(name, TextureUse.SFMEDIUM, TEXT_SIZE) + (PADDING_SIDE * 2);
            float keyW = Client.RENDERER.textWidth(key, TextureUse.SFMEDIUM, TEXT_SIZE) + (PADDING_SIDE * 2);

            float totalRowW = nameW + SPACING + Math.max(HEIGHT, keyW);
            maxRowWidth = Math.max(maxRowWidth, totalRowW);

            float rowX = isRightSide ? (x + width - totalRowW) : x;

            renderKeyRow(rowX, currentY, name, key, nameW, Math.max(HEIGHT, keyW), alpha, isRightSide);

            currentY += (HEIGHT + 1.0f) * alpha;
        }

        this.drag.width = width = MathUtility.linearFps(width, maxRowWidth, 10);
        this.drag.height = height = (currentY - y);
    }

    private void renderHeader(float ry, boolean right) {
        String title = "Key Binds";
        float iconW = Client.RENDERER.textWidth(IconUse.KEYBOARD.glyph, TextureUse.ICONS, ICON_SIZE);
        float textW = Client.RENDERER.textWidth(title, TextureUse.SFMEDIUM, TEXT_SIZE);
        float headerW = PADDING_SIDE + iconW + SPACING + 0.5f + SPACING + textW + PADDING_SIDE;

        float hx = right ? (x + width - headerW) : x;

        drawStyle(hx, ry, headerW, HEIGHT, 1f);

        float iconX = hx + PADDING_SIDE;
        float lineX = iconX + iconW + SPACING;
        float textX = lineX + 0.5f + SPACING;

        Client.RENDERER.text(IconUse.KEYBOARD.glyph, iconX - 1, ry + (HEIGHT / 2f) - (ICON_SIZE / 2f), TextureUse.ICONS, ICON_SIZE, Color.WHITE);
        Client.RENDERER.rect(lineX, ry + 3f, 0.5f, HEIGHT - 6f, new Vector4f(0), 1, new Color(255, 255, 255, 50), new Color(255, 255, 255, 50), new Color(255, 255, 255, 50), new Color(255, 255, 255, 50));
        Client.RENDERER.text(title, textX, ry + (HEIGHT / 2f) - (TEXT_SIZE / 2f) - 0.3f, TextureUse.SFMEDIUM, TEXT_SIZE, Color.WHITE);
    }

    private void renderKeyRow(float rx, float ry, String name, String key, float nW, float kW, float alpha, boolean right) {
        drawStyle(rx, ry, nW, HEIGHT, alpha);
        drawCenteredText(name, rx - 1, ry, nW, alpha);

        float keyX = rx + nW + SPACING;
        drawStyle(keyX, ry, kW - 3, HEIGHT, alpha);
        drawCenteredText(key, keyX - 2, ry, kW, alpha);
    }

    private void drawStyle(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(5f);
         Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);

        if (style.get() == Style.DARK) {
            Color bgColor = new Color(20, 20, 25, (int) (180 * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);

            Color outColor = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        } else {
            Color bgColor = new Color(62, 62, 71, 0);
            Client.RENDERER.rect(rx, ry, rw , rh, round, 1, bgColor, bgColor, bgColor, bgColor);

            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color outColor = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        }
    }

    private void drawCenteredText(String text, float tx, float ty, float tw, float alpha) {
        float xOff = tx + (tw / 2f) - (Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, TEXT_SIZE) / 2f);
        int textAlpha = MathHelper.clamp((int) (255 * alpha), 0, 255);
        Client.RENDERER.text(text, xOff, ty + (HEIGHT / 2f) - (TEXT_SIZE / 2f) - 0.3f, TextureUse.SFMEDIUM, TEXT_SIZE, new Color(255, 255, 255, textAlpha));
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}