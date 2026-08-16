package gg.topchdlc.api.ui;

import lombok.Getter;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.vse.utils.animations.impl.EaseInOutQuad;

public abstract class UIWidget extends RendererObject {
    protected int id = System.identityHashCode(this);

    @Getter
    public boolean shouldRemove = false;

    @Getter
    protected EaseInOutQuad animation = new EaseInOutQuad(300, 1);

    public void opened() {}
}
