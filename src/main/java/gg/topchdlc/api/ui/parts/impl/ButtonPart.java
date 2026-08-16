package gg.topchdlc.api.ui.parts.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.parts.UIPart;
import gg.topchdlc.vse.utils.client.client.ClientColors;

import java.util.function.Consumer;
public class ButtonPart extends UIPart {
    private final String label;
    private final Consumer<ButtonPart> onClick;

    public ButtonPart(String label, Consumer<ButtonPart> onClick) {
        this.label = label;
        this.onClick = onClick;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        Client.RENDERER.drawModuleRect(x, y, width, height);
        float tw = Client.RENDERER.textWidth(label, TextureUse.SFMEDIUM, 8);
        Client.RENDERER.text(label, x + (width - tw) / 2f, y + 4, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (hover(mouseX, mouseY) && button == 0) {
            if (onClick != null) onClick.accept(this);
            return true;
        }
        return false;
    }

    @Override
    public float getPreferredHeight(float availableWidth) {
        return 16;
    }
}


