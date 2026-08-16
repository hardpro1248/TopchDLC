package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.utils.animations.Animation;
import gg.topchdlc.vse.utils.animations.Easings;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.List;

/**
 * Create by daun kvass
 */
public class ChoiceSettingsWidget extends UIWidget {
    private final Choice choice;
    private final Animation animation = new Animation();
    private boolean closing = false;
    private float scroll = 0f;
    private float scrollAnim = 0f;

    public ChoiceSettingsWidget(Choice choice, float targetX, float targetY, float targetWidth) {
        this.choice = choice;
        this.x = targetX;
        this.y = targetY;
        this.width = targetWidth;

        this.animation.set(0.0);
        this.animation.run(1.0, 0.18, Easings.LINEAR);
    }

    private void close() {
        if (!closing) {
            closing = true;
            this.animation.run(0.0, 0.15, Easings.LINEAR);
        }
    }

    @Override
    public void render(int mouseX, int mouseY) {
        animation.update();
        float progress = animation.get();

        if (closing && animation.isFinished() && progress <= 0.01f) {
            this.shouldRemove = true;
            return;
        }

        List<SettingRenderer<?>> renderers = choice.getSettingRenderers();
        float totalContentHeight = 15f;

        for (SettingRenderer<?> renderer : renderers) {
            if (renderer.getSetting().getVisible().get()) {
                float h = Math.max(14f, renderer.getHeight());
                totalContentHeight += h + 2.5f;
            }
        }

        float maxDisplayHeight = 130f;
        float targetHeight = Math.min(maxDisplayHeight, totalContentHeight);

        this.height = targetHeight * progress;

        float maxScroll = Math.max(0, totalContentHeight - targetHeight);
        scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 15f);

        int bgAlpha = (int) (210 * progress);
        int borderAlpha = (int) (10 * progress);
        Color glassBg = new Color(10, 11, 16, bgAlpha);
        Color outlineColor = new Color(255, 255, 255, borderAlpha);

        Client.RENDERER.blur(x, y, width, height, new Vector4f(5), 10f, progress);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(5), 1, glassBg, glassBg, glassBg, glassBg);
        Client.RENDERER.outline(x, y, width, height, 0.3f, new Vector4f(5), new Vector2f(1), outlineColor, outlineColor, outlineColor, outlineColor);

        if (progress > 0.15f) {
            Client.RENDERER.getCrenderSystem().push(x, y, width, height);

            Client.RENDERER.text("Настройки : " + choice.name , x + 5f, y + 4f, TextureUse.SFMEDIUM, 5.5f, ClientColors.FORE_COLOR);

            float curY = y + 14f + (scrollAnim * progress);

            for (SettingRenderer<?> renderer : renderers) {
                if (!renderer.getSetting().getVisible().get()) continue;

                float sH = Math.max(14f, renderer.getHeight());
                renderer.bound(x + 4f, curY, width - 8f, sH);
                renderer.render(mouseX, mouseY);

                curY += sH + 2.5f;
            }

            Client.RENDERER.getCrenderSystem().pop();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (MathUtility.mouseIn(x, y, width, height, (int) mouseX, (int) mouseY)) {
            scroll += (float) (verticalAmount * 14);
            return true;
        }
        return false;
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!hover(mouseX, mouseY)) {
            close();
            return true;
        }

        float curY = y + 14f + scrollAnim;
        for (SettingRenderer<?> renderer : choice.getSettingRenderers()) {
            if (!renderer.getSetting().getVisible().get()) continue;

            float sH = Math.max(14f, renderer.getHeight());
            if (MathUtility.mouseIn(x + 4f, curY, width - 8f, sH, mouseX, mouseY)) {
                renderer.click(mouseX, mouseY, button);
                return true;
            }
            curY += sH + 2.5f;
        }

        return true;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (SettingRenderer<?> renderer : choice.getSettingRenderers()) {
            if (renderer.getSetting().getVisible().get()) {
                renderer.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        for (SettingRenderer<?> renderer : choice.getSettingRenderers()) {
            if (renderer.getSetting().getVisible().get()) {
                renderer.chartyped(ch, keyCode);
            }
        }
        super.chartyped(ch, keyCode);
    }

    @Override
    public void release(int button) {
        for (SettingRenderer<?> renderer : choice.getSettingRenderers()) {
            if (renderer.getSetting().getVisible().get()) {
                renderer.release(button);
            }
        }
        super.release(button);
    }
}