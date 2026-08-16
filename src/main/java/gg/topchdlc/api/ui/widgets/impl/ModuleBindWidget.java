package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.screen.screens.ingame.objects.ModuleRenderer;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindRenderer;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Rectangle;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

public class ModuleBindWidget extends UIWidget implements MinecraftHolder {

    private ModuleRenderer parent;

    public ModuleBindWidget(ModuleRenderer parent) {
        this.parent = parent;
        this.animation.setDirection(Direction.BACKWARDS);
        this.animation.reset();
        this.bind = new KeybindSetting("Hotkey", parent.module.getKey()).onChanged(parent.module::setKey);
        this.bindRenderer = new KeybindRenderer(this.bind);
    }

    @Override
    public void opened() {
        bound((float) mc.mouse.getScaledX(mc.getWindow()), (float) mc.mouse.getScaledY(mc.getWindow()), 100, 48);
        this.animation.setDirection(Direction.FORWARDS);
    }

    Rectangle pressBox = new Rectangle(),
            holdBox = new Rectangle();

    float anim1, anim2;

    KeybindSetting bind;
    KeybindRenderer bindRenderer;

    @Override
    public void render(int mouseX, int mouseY) {
        MatrixStack stack = Client.RENDERER.getStack();
        CRenderSystem system = Client.RENDERER.getCrenderSystem();

        stack.push();
        MathUtility.scale(stack, x + width / 2F, y + height / 2F, 0.5F + this.animation.getOutput() * 0.5F);

        Client.RENDERER.outlined(x, y, width, height);
        bindRenderer.bound(x + 8, y + 6, width - 16, 20).render(mouseX, mouseY);
        if (!bindRenderer.isBinding()) bind.bind(parent.module.getKey());

        Module.BindType type = parent.module.getBindType();

        float baseWidth = width / 2F - 8;

        int ord = type.ordinal();
        boolean l = type == Module.BindType.PRESS;
        float ANIM = 0.8F;
        if (l || anim2 > 0.8F || anim1 > 0.05) {
            anim1 = MathUtility.linearFps(anim1, type == Module.BindType.HOLD ? 1 : 0, 10);
        }
        if (!l || anim1 < 0.2) {
            anim2 = MathUtility.linearFps(anim2, type == Module.BindType.HOLD ? 1 : 0, 10);
        }
        Client.RENDERER.drawModuleRect(x + 8 + baseWidth * anim1, y + 28, baseWidth + (anim2 - anim1) * baseWidth, 15);

        float alpha = system.alpha();

        {
            pressBox.bound(x + 8, y + 28, baseWidth, 15);
            system.alpha(alpha * ((1F - anim1) + 0.5F));
            Client.RENDERER.textCenteredStrict("Toggle", pressBox.getX() + pressBox.getWidth() / 2F, pressBox.getY() + pressBox.getHeight() / 2F, TextureUse.SFMEDIUM, 7, ClientColors.FORE_COLOR);
            system.alpha(alpha);
        }
        {
            holdBox.bound(x + 8 + baseWidth, y + 28, baseWidth, 15);
            system.alpha(alpha * ((anim1) + 0.5F));
            Client.RENDERER.textCenteredStrict("Hold", holdBox.getX() + holdBox.getWidth() / 2F, holdBox.getY() + holdBox.getHeight() / 2F, TextureUse.SFMEDIUM, 7, ClientColors.FORE_COLOR);
            system.alpha(alpha);
        }

        stack.pop();

        if (this.animation.getOutput() < 0.1 && this.animation.getDirection() == Direction.BACKWARDS) {
            this.shouldRemove = true;
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (bindRenderer.click(mouseX, mouseY, button)) return true;
        if (holdBox.hovered(mouseX, mouseY)) {
            parent.module.setBindType(Module.BindType.HOLD);
            return true;
        } else if (pressBox.hovered(mouseX, mouseY)) {
            parent.module.setBindType(Module.BindType.PRESS);
            return true;
        }
        if (!hover(mouseX, mouseY)) {
            this.animation.setDirection(Direction.BACKWARDS);
        }
        return true;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!bindRenderer.isBinding() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.animation.setDirection(Direction.BACKWARDS);
        }

        bindRenderer.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return hover((int) mouseX, (int) mouseY);
    }
}
