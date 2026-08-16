package gg.topchdlc.vse.shutki.module.settings.impl.group;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;

public class GroupRenderer extends SettingRenderer<Group> {
    private final List<SettingRenderer<?>> renderers;

    public GroupRenderer(Group setting) {
        super(setting);
        renderers = setting.getSettingRenderers();
    }

    float renderHeight = -1;

    @Override
    public void render(int mouseX, int mouseY) {
        MatrixStack stack = Client.RENDERER.getStack();
        float expandRaw = getSetting().isExpandable() ? setting.expandAnim.getOutput() : 0;
        float expand = 1f - expandRaw;

        Color glassBg = new Color(10, 10, 10, 5);
        Client.RENDERER.rect(x, y + 2, width, renderHeight, new Vector4f(6), 1, glassBg, glassBg, glassBg, glassBg);

        Color outlineColor = new Color(255, 255, 255, 3);
        Client.RENDERER.outline(x, y + 2, width, renderHeight, 0.5f, new Vector4f(6), new Vector2f(1), outlineColor, outlineColor, outlineColor, outlineColor);

        Color titleColor;
        if (setting.isExpanded()) {
            titleColor = new Color(135, 135, 135);
        } else {
            float sin = (float) ((Math.sin(System.currentTimeMillis() / 400.0) + 1.0) / 2.0);
            int brightness = (int) (160 + sin * 75);
            titleColor = new Color(brightness, brightness, brightness);
        }

        float fontSize = 7.8f;
        float textWidth = Client.RENDERER.textWidth(setting.getName(), TextureUse.SFMEDIUM, fontSize);
        float startX = x + (width - textWidth) / 2f;

        Client.RENDERER.text(setting.getName(), startX, y + 7, TextureUse.SFMEDIUM, fontSize, titleColor);

        if (setting.enabledRenderer != null) {
            setting.enabledRenderer.noName = true;
            setting.enabledRenderer.bound(x + width - 40, y + 5, 36, 20).render(mouseX, mouseY);
        }

        stack.push();
        stack.translate(MathUtility.scaledX(x), MathUtility.scaledY(y + 22), 0);
        stack.scale(1, expand, 1);
        stack.translate(-MathUtility.scaledX(x), -MathUtility.scaledY(y + 22), 0);

        float offsetY = 12;
        if (expand > 0.1) {
            for (SettingRenderer<?> renderer : renderers) {
                if (renderer.getSetting() == setting.enabled) continue;
                float visible = renderer.visible.getOutput();
                renderer.visible.setDirection(renderer.getSetting().getVisible().get() ? Direction.FORWARDS : Direction.BACKWARDS);

                if (visible > 0.1) {
                    renderer.bound(x + 4, y + 6 + offsetY, width - 8, 15);

                    stack.push();
                    MathUtility.scale(stack, renderer.getX() + renderer.getWidth() / 2F, renderer.getY() + renderer.getHeight() / 2F, 1, visible);
                    renderer.render(mouseX, mouseY);
                    stack.pop();

                    offsetY += renderer.getHeight() * visible;
                }
            }
        }
        height = 12 + (offsetY - 12) * expand + 12;
        renderHeight = height - 4;
        stack.pop();
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_2 && MathUtility.mouseIn(x, y, width, 18, mouseX, mouseY)) {
            setting.expanded(!setting.isExpanded());
            return true;
        }
        if (setting.isExpanded()) {
            for (SettingRenderer<?> renderer : renderers) {
                if (!renderer.getSetting().getVisible().get() || renderer.getSetting() == setting.enabled) continue;
                if (renderer.click(mouseX, mouseY, button)) return true;
            }
        }
        if (setting.enabledRenderer != null) {
            if (setting.enabledRenderer.click(mouseX, mouseY, button)) return true;
        }
        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (SettingRenderer<?> renderer: renderers) {
            if (!renderer.getSetting().getVisible().get()) continue;
            renderer.keyPressed(keyCode, scanCode, modifiers);
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        for (SettingRenderer<?> renderer: renderers) {
            if (!renderer.getSetting().getVisible().get()) continue;
            renderer.chartyped(ch, keyCode);
        }
        super.chartyped(ch, keyCode);
    }

    @Override
    public void release(int button) {
        for (SettingRenderer<?> renderer: renderers) {
            if (!renderer.getSetting().getVisible().get() || renderer.getSetting() == setting.enabled) continue;
            renderer.release(button);
        }
        super.release(button);
    }
}