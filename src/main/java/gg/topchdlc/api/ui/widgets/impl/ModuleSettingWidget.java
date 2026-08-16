package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.widgets.UIModuleWidget;
import gg.topchdlc.vse.utils.animations.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.screen.screens.ingame.objects.ModuleRenderer;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.*;

public class ModuleSettingWidget extends UIModuleWidget {
    private final ModuleRenderer parent;
    private float scroll = 0, scrollAnim;

    Drag drag;
    private boolean positioned = false;
    
    public ModuleSettingWidget(ModuleRenderer parent) {
        this.parent = parent;
        this.drag = new Drag("ModuleSettingWidget", () -> true);
    }
    
    public void openAt(float x, float y, float width) {
        if (!positioned) {
            this.x = x;
            this.y = y;
            this.width = width;
            positioned = true;
        }
    }

    @Override
    public void render(int mouseX, int mouseY) {

        float pExpandAnim = 1.f - Client.GLASS_GUI.getSettingAnim().getOutput();
        Client.RENDERER.getStack().push();
        Client.RENDERER.getStack().translate((1.f - pExpandAnim) * width * 2, 0, 0);
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        float prevAlpha = system.alpha();


        system.push(x, y, width, height);

        Color back = ColorUtility.injectAlpha(ClientColors.GUI_BACKGROUND, 255);
        Color out = ClientColors.GUI_STROKE;
        Client.RENDERER.text(String.format("%s", parent.module.getName()), x + 18, y + 7, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);

        Client.RENDERER.text(IconUse.BACK, x + 6, y + 7, TextureUse.ICONS, 9, ClientColors.FORE_COLOR);

        Client.RENDERER.rect(x + 5, y + 23, width - 10, 1, new Vector4f(0), 1, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR);

        system.push(x, y + 27, width, height - 27);
        float offsetY1 = 0, offsetY2 = 0, offsetX = 0;
        boolean isRight;
        for (SettingRenderer<?> renderer: parent.settingRenderers) {
            float visible = renderer.visible.getOutput();
            renderer.visible.setDirection(renderer.getSetting().getVisible().get() ? Direction.FORWARDS : Direction.BACKWARDS);

            if (visible > 0.1) {
                isRight = offsetX > 0;

                renderer.bound(x + 8 + offsetX, y + 28 + (isRight ? offsetY2 : offsetY1) + scrollAnim, width / 2f - 16, renderer.getHeight());

                Client.RENDERER.getStack().push();
                MathUtility.scale(Client.RENDERER.getStack(), renderer.getX() + renderer.getWidth() / 2F, renderer.getY() + renderer.getHeight() / 2F, visible);
                renderer.render(mouseX, mouseY);
                Client.RENDERER.getStack().pop();
                if (!isRight)
                    offsetY1 += renderer.getHeight() * visible;
                else
                    offsetY2 += renderer.getHeight() * visible;

                offsetX += renderer.getWidth() + 12;

                if (offsetX > width / 2f) {
                    offsetX = 0;
                }
            }
        }
        system.pop();

        system.pop();

        Client.RENDERER.getStack().pop();

        height = Math.min(60 + offsetY1 + offsetY2, 300);

        scroll = MathHelper.clamp(scroll, -(offsetY1 + offsetY2) + 18, 0);
        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 10f);

        if (pExpandAnim > 0.2 && pExpandAnim < 0.9) {
            scroll = 0;
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (MathUtility.mouseIn(x + 6, y + 6, 9, 9, mouseX, mouseY)) {
            Client.GLASS_GUI.getSettingAnim().setDirection(Direction.FORWARDS);
            return true;
        }
        for (SettingRenderer<?> settingRenderer: parent.settingRenderers) {
            if (!settingRenderer.getSetting().getVisible().get()) continue;
            if (settingRenderer.click(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public void release(int button) {
        if (button == 0) drag.dragging = false;
        for (SettingRenderer<?> settingRenderer: parent.settingRenderers) {
            if (!settingRenderer.getSetting().getVisible().get()) continue;
            settingRenderer.release(button);
        }
        super.release(button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (SettingRenderer<?> settingRenderer: parent.settingRenderers) {
            if (!settingRenderer.getSetting().getVisible().get()) continue;
            settingRenderer.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        for (SettingRenderer<?> settingRenderer: parent.settingRenderers) {
            if (!settingRenderer.getSetting().getVisible().get()) continue;
            settingRenderer.chartyped(ch, keyCode);
        }
        super.chartyped(ch, keyCode);
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY) {
        if (drag.dragging) {
            this.x = mouseX - drag.dX;
            this.y = mouseY - drag.dY;
        }
        for (SettingRenderer<?> settingRenderer: parent.settingRenderers) {
            if (!settingRenderer.getSetting().getVisible().get()) continue;
            settingRenderer.mouseDragged(mouseX, mouseY);
        }
        super.mouseDragged(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hover((int) mouseX, (int) mouseY)) {
            scroll += (float) verticalAmount * 10;
        }
        return true;
    }
}
