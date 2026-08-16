package gg.topchdlc.api.ui;

import gg.topchdlc.api.ui.parts.UIPart;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.CharInput;

import java.util.ArrayList;

public class UIScreen extends Screen {

    final ArrayList<UIPart> parts = new ArrayList<>();

    public UIScreen() {
        super(Text.empty());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        for (UIPart part : parts) {
            part.render(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        for (UIPart part : parts) {
            if (part.click((int) click.x(), (int) click.y(), click.button())) return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        for (UIPart part : parts) {
            part.release(click.button());
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (UIPart part : parts) {
            if (part.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        for (UIPart part : parts) {
            part.mouseDragged((int) click.x(), (int) click.y());
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        for (UIPart part : parts) {
            part.keyPressed(input.key(), input.scancode(), input.modifiers());
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        for (UIPart part : parts) {

            part.chartyped((char) input.codepoint(), input.modifiers());
        }
        return super.charTyped(input);
    }

    public void addPart(UIPart part) {
        parts.add(part);
    }
    public void removePart(UIPart part) {
        parts.remove(part);
    }
}