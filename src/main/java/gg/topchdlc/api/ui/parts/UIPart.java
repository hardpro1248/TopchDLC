package gg.topchdlc.api.ui.parts;

import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.ui.UIStyle;

public abstract class UIPart extends RendererObject {
    public UIStyle style;

    public float getPreferredHeight(float availableWidth) {
        return 20;
    }
}
