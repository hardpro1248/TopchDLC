package gg.topchdlc.api.ui.parts.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.ui.parts.UIPart;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class ScrollPart extends UIPart {
    private final List<UIPart> children = new ArrayList<>();
    private float scroll;
    private float scrollAnim;

    public ScrollPart(UIPart... parts) {
        if (parts != null) children.addAll(Arrays.asList(parts));
    }

    public ScrollPart add(UIPart part) {
        children.add(part);
        return this;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 10f);

        Client.RENDERER.getCrenderSystem().push(x, y, width, height);

        float offY = 0;
        for (UIPart child : children) {
            float ph = child.getPreferredHeight(width - 8);
            child.bound(x + 4, y + 4 + scrollAnim + offY, width - 8, ph);
            child.render(mouseX, mouseY);
            offY += ph + 4;
        }

        Client.RENDERER.getCrenderSystem().pop();

        float contentHeight = Math.max(0, offY - 4);
        if (contentHeight > height) {
            scroll = MathUtility.clamp(scroll, -(contentHeight - height), 0);
        } else {
            scroll = 0;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!hover((int) mouseX, (int) mouseY)) return false;
        scroll += (float) verticalAmount * 10f;
        return true;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY) {
        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).mouseDragged(mouseX, mouseY);
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i).click(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public void release(int button) {
        for (UIPart child : children) child.release(button);
    }
}


