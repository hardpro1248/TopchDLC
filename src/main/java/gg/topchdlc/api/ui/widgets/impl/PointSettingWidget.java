package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.settings.impl.point.PointRenderer;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;

public class PointSettingWidget extends UIWidget {

    private PointRenderer renderer;

    public PointSettingWidget(PointRenderer renderer) {
        this.renderer = renderer;
    }

    boolean dragging = false;

    float lerpX = -999, lerpY = -999;

    public boolean expanded = false;

    @Override
    public void render(int mouseX, int mouseY) {
        float pAnim = 1.f - animation.getOutput();
        float alpha = Client.RENDERER.getCrenderSystem().alpha();
        Client.RENDERER.getCrenderSystem().alpha(pAnim * alpha);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(6), 1, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR, ClientColors.BACK_COLOR);
        Client.RENDERER.outline(x, y, width, height, 0, new Vector4f(6), new Vector2f(1),
                ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR);
        Client.RENDERER.rect(x + width / 2f, y + 0.5f, 1, height - 1, new Vector4f(0), 0, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR);
        Client.RENDERER.rect(x + 0.5f, y + height / 2f, height - 1, 1, new Vector4f(0), 0, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR);
        Color themeColor = ClientSettings.INSTANCE.getColor(0);{
            Client.RENDERER.rect(x + width / 2f + (width / 2f) * lerpX - 3.5f, y + height / 2f + (height / 2f) * lerpY - 3.5f, 8, 8, new Vector4f(7), 1,
                    themeColor, themeColor, themeColor, themeColor);}
        Vector2f vec = renderer.getSetting().get();
        float dx = MathHelper.clamp((mouseX - (x + width / 2f)) / (width / 2f), -1, 1);
        float dy = MathHelper.clamp((mouseY - (y + height / 2f)) / (height / 2f), -1, 1);
        if (lerpX == -999 || lerpY == -999) {
            lerpX = dx;
            lerpY = dy;
        }
        if (dragging) {
            lerpX = MathUtility.linearFps(lerpX, dx, 15);
            lerpY = MathUtility.linearFps(lerpY, dy, 15);
            vec.x = lerpX;
            vec.y = lerpY;
        }
        Client.RENDERER.getCrenderSystem().alpha(alpha);
        if (pAnim < 0.1) {
            shouldRemove = true;
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (hover(mouseX, mouseY)) {
            if (button == 0)
                dragging = true;
            else {
                lerpX = 0;
                lerpY = 0;
                this.renderer.getSetting().set(new Vector2f());
            }
            return true;
        } else if (button < 2) {
            this.animation.setDirection(Direction.FORWARDS);
            return false;
        }
        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void release(int button) {
        dragging = false;
        super.release(button);
    }
}
