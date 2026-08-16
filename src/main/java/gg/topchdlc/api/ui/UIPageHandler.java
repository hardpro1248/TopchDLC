package gg.topchdlc.api.ui;

import gg.topchdlc.api.render.RendererObject;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;

public class UIPageHandler extends RendererObject {

    @Getter
    private ArrayList<UIPage> pages = new ArrayList<>();

    public UIPageHandler register(UIPage... pages) {
        this.pages.addAll(Arrays.asList(pages));
        return this;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        for (UIPage page : pages) {
            page.bound(x, y, width, height)
                    .render(mouseX, mouseY);
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        for (UIPage page : pages) {
            if (page.click(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (UIPage page : pages) {
            page.keyPressed(keyCode, scanCode, modifiers);
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        for (UIPage page : pages) {
            page.chartyped(ch, keyCode);
        }
        super.chartyped(ch, keyCode);
    }

    @Override
    public void release(int button) {
        for (UIPage page : pages) {
            page.release(button);
        }
        super.release(button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (UIPage page : pages) {
            page.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
