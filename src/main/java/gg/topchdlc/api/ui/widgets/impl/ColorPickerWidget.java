package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.ShaderUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Rectangle;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;

public class ColorPickerWidget extends UIWidget {
    ColorSetting colorSetting;
    Drag drag = new Drag("ColorPicker", () -> true);

    public static ArrayList<Color> lastColors = new ArrayList<>();
    public boolean expanded = false;

    public ColorPickerWidget(ColorSetting setting) {
        this.colorSetting = setting;
    }

    Rectangle color = new Rectangle();
    Rectangle hue = new Rectangle();
    Rectangle saveColor = new Rectangle();

    boolean draggingHue = false, draggingColor = false;

    public  float[] hsb = new float[3];

    @Override
    public void render(int mouseX, int mouseY) {
        if (drag.dragging) {
            x = mouseX + drag.dX;
            y = mouseY + drag.dY;
        }

        animation.setDirection(expanded ? Direction.BACKWARDS : Direction.FORWARDS);

        float pAnim = 1.f - animation.getOutput();

        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        MatrixStack stack = Client.RENDERER.getStack();

        float alpha = system.alpha();

        stack.push();
        MathUtility.scale(stack, x + width / 2f, y + height / 2f, 0.8f + pAnim * 0.2f);

        system.alpha(alpha * pAnim);

        Client.RENDERER.rect(x, y, width, height, new Vector4f(6), 1, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR);
        Client.RENDERER.outline(x, y, width, height, 1, new Vector4f(6), new Vector2f(1), ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
        Client.RENDERER.text(colorSetting.getName(), x + 6, y + 6, TextureUse.SFMEDIUM, 9, ClientColors.FORE_COLOR);
        Client.RENDERER.text(IconUse.CROSS, x + width - 16, y + 6, TextureUse.ICONS, 9, ClientColors.FORE_COLOR);
        color.bound(x + width / 2f - 60, y + 30, 120, 85);
        hue.bound(color.getX(), color.getY() + color.getHeight() + 4, color.getWidth(), 5);
        saveColor.bound(color.getX(), hue.getY() + 16, 14, 14);
        Client.RENDERER.shader(color, new Vector4f(4), 1, ShaderUse.COLOR_PICKER, Color.getHSBColor(hsb[0], 1, 1));
        Client.RENDERER.shader(hue, new Vector4f(4), 1, ShaderUse.HUE);
        Client.RENDERER.drawModuleRect(saveColor);
        Client.RENDERER.text(IconUse.ADD, saveColor.getX() + 1.5F, saveColor.getY() + 2.5f, TextureUse.ICONS, 8, ClientColors.DARK_GRAY_COLOR);
        Client.RENDERER.rect(hue.getX() + hsb[0] * hue.getWidth() - 0.5f, hue.getY(), 1, hue.getHeight(), new Vector4f(0), 0, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        Color d = ColorUtility.contrast(colorSetting.get());
        {
            Client.RENDERER.rect(color.getX() + hsb[1] * color.getWidth() - 2, (color.getY() + (1 - hsb[2]) * color.getHeight() - 2), 4, 4, new Vector4f(3), 2, d, d, d, d);
        }
        if (draggingHue) {
            hsb[0] = Math.clamp((mouseX - hue.getX()) / hue.getWidth(), 0, 1);
            colorSetting.color(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
        } else if (draggingColor) {
            hsb[1] = Math.clamp((mouseX - color.getX()) / color.getWidth(), 0, 1);
            hsb[2] = 1 - Math.clamp((mouseY - color.getY()) / color.getHeight(), 0, 1);
            colorSetting.color(Color.getHSBColor(hsb[0], hsb[1], hsb[2]));
        } else {
            Color c = colorSetting.get();
            hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
        }

        float off = 4;
        for (Color co : lastColors) {
            Client.RENDERER.rect(saveColor.getX() + saveColor.getWidth() + off, saveColor.getY(), saveColor.getWidth(), saveColor.getHeight(), new Vector4f((saveColor.getWidth() + saveColor.getHeight()) / 2f - 1), 1, co, co, co, co);
            off += saveColor.getWidth() + 4;
        }

        system.alpha(alpha);
        stack.pop();

        if (animation.finished(Direction.FORWARDS) && expanded && !shouldRemove) {
            shouldRemove = true;
            expanded = false;
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!expanded) return false;

        if (!hover(mouseX, mouseY) || MathUtility.mouseIn(x + width - 16, y + 6, 9, 9, mouseX, mouseY)) {
            expanded = false;
            animation.setDirection(Direction.FORWARDS);
            return true;
        }
        if (hue.hovered(mouseX, mouseY)) {
            draggingHue = true;
            return true;
        }
        if (color.hovered(mouseX, mouseY)) {
            draggingColor = true;
            return true;
        }
        if (saveColor.hovered(mouseX, mouseY)) {
            lastColors.add(colorSetting.get());
            if (lastColors.size() > 6) {
                lastColors.removeFirst();
            }
            return true;
        }
        float off = 4;
        for (Color co : lastColors) {
            if (MathUtility.mouseIn(saveColor.getX() + off + saveColor.getWidth(), saveColor.getY(), saveColor.getWidth(), saveColor.getHeight(), mouseX, mouseY)) {
                colorSetting.color(co);
            }
            off += saveColor.getWidth() + 4;
        }
        if (hover(mouseX, mouseY)) {
            drag.dragging = true;
            drag.dX = x - mouseX;
            drag.dY = y - mouseY;
        }
        return true;
    }

    @Override
    public void release(int button) {
        super.release(button);
        drag.dragging = false;
        draggingHue = false;
        draggingColor = false;
    }

    public static class ColorObject extends RendererObject {
        Color color;
        ColorSetting setting;
        ColorPickerWidget parent;
        public ColorObject(Color color, ColorSetting setting, ColorPickerWidget parent) {
            this.color = color;
            this.setting = setting;
            this.parent = parent;
        }

        @Override
        public void render(int mouseX, int mouseY) {
            if (hover(mouseX, mouseY))  return;
            Client.RENDERER.rect(x, y, width, height, new Vector4f((width + height) / 2f - 1), 1, color, color, color, color);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int button) {
            if (hover(mouseX, mouseY)) {
                return true;
            }
            return super.click(mouseX, mouseY, button);
        }
    }
}
