package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.experimental.Accessors;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true, chain = true)
public class ChoiceRendererWidget<C extends Choice> extends UIWidget {
    private final ChoiceSetting<C> setting;
    private final List<ChoiceObject<C>> objects;
    private float scroll, scrollAnim;

    public ChoiceRendererWidget(ChoiceSetting<C> setting, float x, float y, float width) {
        this.setting = setting;
        this.objects = new ArrayList<>();
        for (C choice : setting.getChoices()) {
            objects.add(new ChoiceObject<>(choice));
        }
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = 10;
        this.animation.setDirection(Direction.FORWARDS);
    }

    @Override
    public void opened() {
        this.animation.setDirection(Direction.FORWARDS);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        Client.RENDERER.blur(x, y, width, height, new Vector4f(6), 8f, 1f);
        Color glassBg = new Color(25, 25, 30, 255);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(6), 1, glassBg, glassBg, glassBg, glassBg);
        Color outlineColor = new Color(60, 60, 65, 255);
        Client.RENDERER.outline(x, y, width, height, 1f, new Vector4f(6), new Vector2f(1), outlineColor, outlineColor, outlineColor, outlineColor);
        Client.RENDERER.text(setting.get().name, x + 8, y + 4, TextureUse.SFMEDIUM, 5, ClientSettings.INSTANCE.getColor(0));
        float offY = 16;
        float out = this.animation.getOutput();
        system.push(x, y + 16, width, height - 16);
        for (ChoiceObject<C> obj : objects) {
            obj.bound(x + 4, y + offY + scrollAnim * out, width - 8, 14);
            obj.render(mouseX, mouseY);
            offY += 14;
        }
        system.pop();
        height = Math.min(120, Math.max(16, 16 + (offY - 16) * out));
        if (this.animation.getDirection() == Direction.BACKWARDS && out < 0.1) {
            this.shouldRemove = true;
        }
        scroll = MathHelper.clamp(scroll, -offY + height, 0);
        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 15);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hover((int) mouseX, (int) mouseY)) {
            scroll += (float) (verticalAmount * 10);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!hover(mouseX, mouseY) && this.animation.getDirection() == Direction.FORWARDS) {
            this.animation.setDirection(Direction.BACKWARDS);
            return true;
        }
        for (ChoiceObject<C> obj : objects) {
            if (obj.hover(mouseX, mouseY)) {
                setting.select(obj.choice);
                this.animation.setDirection(Direction.BACKWARDS);
                return true;
            }
        }
        return true;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        this.animation.setDirection(Direction.BACKWARDS);
    }

    @Accessors(chain = true)
    private static class ChoiceObject<C extends Choice> extends RendererObject {
        private final C choice;
        public ChoiceObject(C choice) {
            this.choice = choice;
        }
        @Override
        public void render(int mouseX, int mouseY) {
            boolean hovered = hover(mouseX, mouseY);
            boolean selected = false;
            if (hovered) {
                Color hoverBg = new Color(45, 45, 50, 255);
                Client.RENDERER.rect(x, y, width, height, new Vector4f(3), 1, hoverBg, hoverBg, hoverBg, hoverBg);
            }
            Color textColor = hovered ? Color.WHITE : new Color(200, 200, 200);
            Client.RENDERER.text(choice.name, x + 8, y + 4, TextureUse.SFMEDIUM, 5, textColor);
        }
    }
}