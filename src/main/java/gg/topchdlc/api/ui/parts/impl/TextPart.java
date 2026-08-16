package gg.topchdlc.api.ui.parts.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.parts.UIPart;
import gg.topchdlc.vse.utils.client.client.ClientColors;
public class TextPart extends UIPart {
    private final String text;
    private final int size;

    public TextPart(String text) {
        this(text, 8);
    }

    public TextPart(String text, int size) {
        this.text = text;
        this.size = size;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        Client.RENDERER.text(text, x, y, TextureUse.SFMEDIUM, size, ClientColors.FORE_COLOR);
    }

    @Override
    public float getPreferredHeight(float availableWidth) {
        return size + 4;
    }
}


