package gg.topchdlc.api.ui.widgets;


import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;

import java.awt.*;
import java.util.List;

public class HudSettingWidget extends UIWidget {
    private final HudElement parent;
    private final Drag drag = new Drag("HudSetting", () -> true);
    private List<SettingRenderer<?>> renderers;

    public HudSettingWidget(HudElement parent) {
        this.parent = parent;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (this.renderers == null) {
            this.renderers = parent.getSettingRenderers();
        }
        if (drag.dragging) {
            this.x = mouseX - drag.dX;
            this.y = mouseY - drag.dY;
        }

        CRenderSystem system = Client.RENDERER.getCrenderSystem();

        float pAnim = 1.f - parent.expandAnim.getOutput();
        float alpha = system.alpha();

        system.alpha(alpha * pAnim);

        MatrixStack stack = Client.RENDERER.getStack();
        stack.push();
        MathUtility.scale(stack, x + width / 2f, y + height / 2f, 0.94f + pAnim * 0.06f);

        Vector4f round = new Vector4f(6);
        Color bgColor = new Color(10, 10, 14, 145);
        Color dividerColor = new Color(255, 255, 255, 20);
        Client.RENDERER.blur(x, y, width, height, round, 14f, 1f);
        Client.RENDERER.rect(x, y, width, height, round, 1, bgColor, bgColor, bgColor, bgColor);
        Client.RENDERER.text("Settings", x + 6, y + 5, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
        Client.RENDERER.text(IconUse.CROSS, x + width - 14, y + 5, TextureUse.ICONS, 8, ClientColors.FORE_COLOR);
        Client.RENDERER.rect(x + 5, y + 18, width - 10, 0.5f, new Vector4f(0), 1, dividerColor, dividerColor, dividerColor, dividerColor);

        float offy = 0;
        for (SettingRenderer<?> renderer: renderers) {
            renderer.bound(x + 4, y + offy + 22, width - 8, height)
                    .render(mouseX, mouseY);
            offy += renderer.getHeight();
        }

        height = Math.max(offy + 28, 38);

        stack.pop();

        system.alpha(alpha);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        for (SettingRenderer<?> renderer: renderers) {
            if (renderer.click(mouseX, mouseY, button)) return true;
        }
        if (!hover(mouseX, mouseY) || MathUtility.mouseIn(x + width - 16, y + 6, 9, 9, mouseX, mouseY)) {
            parent.expanded = false;
            return true;
        }
        if (hover(mouseX, mouseY)) {
            drag.dragging = true;
            drag.dX = mouseX - x;
            drag.dY = mouseY - y;
        }
        return true;
    }

    @Override
    public void release(int button) {
        for (SettingRenderer<?> renderer: renderers) {
            renderer.release(button);
        }
        drag.dragging = false;
        super.release(button);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        for (SettingRenderer<?> renderer: renderers) {
            renderer.chartyped(ch, keyCode);
        }
        super.chartyped(ch, keyCode);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (SettingRenderer<?> renderer: renderers) {
            renderer.keyPressed(keyCode, scanCode, modifiers);
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }
}
