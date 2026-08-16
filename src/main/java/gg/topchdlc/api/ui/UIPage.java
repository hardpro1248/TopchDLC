package gg.topchdlc.api.ui;

import gg.topchdlc.api.render.RendererObject;

public abstract class UIPage extends RendererObject
{
    @Override
    public abstract void render(int mouseX, int mouseY);

}
