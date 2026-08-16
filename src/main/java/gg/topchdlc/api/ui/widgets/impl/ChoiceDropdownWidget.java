package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.utils.animations.Animation;
import gg.topchdlc.vse.utils.animations.Easings;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.List;

/**
 * Create by daun kvass
 */
public class ChoiceDropdownWidget<C extends Choice> extends UIWidget {
    private final ChoiceSetting<C> setting;
    private final Animation animation = new Animation();
    private boolean closing = false;
    private float scroll = 0f;
    private float scrollAnim = 0f;

    public ChoiceDropdownWidget(ChoiceSetting<C> setting, float targetX, float targetY, float targetWidth) {
        this.setting = setting;
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

        List<C> choices = setting.getChoices();
        float itemHeight = 13f;
        float totalContentHeight = choices.size() * itemHeight + 3f;
        float maxDisplayHeight = 110f;
        float targetHeight = Math.min(maxDisplayHeight, totalContentHeight);

        this.height = targetHeight * progress;

        float maxScroll = Math.max(0, totalContentHeight - targetHeight);
        scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 15f);

        int bgAlpha = (int) (200 * progress);
        int borderAlpha = (int) (30 * progress);
        Color glassBg = new Color(12, 13, 18, bgAlpha);
        Color outlineColor = new Color(255, 255, 255, borderAlpha);

        Client.RENDERER.blur(x, y, width, height, new Vector4f(4), 10f, progress);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(4), 1, glassBg, glassBg, glassBg, glassBg);
        Client.RENDERER.outline(x, y, width, height, 0.3f, new Vector4f(4), new Vector2f(1), outlineColor, outlineColor, outlineColor, outlineColor);

        if (progress > 0.15f) {
            Client.RENDERER.getCrenderSystem().push(x, y, width, height);
            float curY = y + 1.5f + (scrollAnim * progress);

            for (C choice : choices) {
                boolean isSelected = setting.get() == choice;
                boolean hovered = MathUtility.mouseIn(x + 1.5f, curY, width - 3f, itemHeight, mouseX, mouseY)
                        && MathUtility.mouseIn(x, y, width, height, mouseX, mouseY);

                Color itemBg;
                if (isSelected) {
                    Color accent = ClientSettings.INSTANCE.getColor(0);
                    itemBg = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (50 * progress));
                } else if (hovered) {
                    itemBg = new Color(255, 255, 255, (int) (18 * progress));
                } else {
                    itemBg = new Color(0, 0, 0, 0);
                }

                if (itemBg.getAlpha() > 0) {
                    Client.RENDERER.rect(x + 1.5f, curY, width - 3f, itemHeight - 1f, new Vector4f(2.5f), 1, itemBg, itemBg, itemBg, itemBg);
                }

                Color textColor = isSelected ? Color.WHITE : (hovered ? new Color(220, 220, 220) : new Color(130, 130, 135));
                float textFontSize = 5.8f;
                float textWidth = Client.RENDERER.textWidth(choice.name, TextureUse.SFMEDIUM, textFontSize);
                float textX = x + (width - textWidth) / 2f;
                float textY = curY + (itemHeight - textFontSize) / 2f - 0.5f;

                Client.RENDERER.text(choice.name, textX, textY, TextureUse.SFMEDIUM, textFontSize, textColor);

                curY += itemHeight;
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

        List<C> choices = setting.getChoices();
        float itemHeight = 13f;
        float curY = y + 1.5f + scrollAnim;

        for (C choice : choices) {
            if (MathUtility.mouseIn(x + 1.5f, curY, width - 3f, itemHeight, mouseX, mouseY)) {
                setting.select(choice);
                close();
                return true;
            }
            curY += itemHeight;
        }

        return true;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        close();
    }
}