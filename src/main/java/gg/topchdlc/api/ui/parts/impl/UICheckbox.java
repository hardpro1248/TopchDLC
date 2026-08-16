package gg.topchdlc.api.ui.parts.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.parts.UIPart;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientSettings;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UICheckbox extends UIPart {
    private final Supplier<Boolean> get;
    private final Consumer<Boolean> set;
    private final SmoothStepAnimation checkAnimation = new SmoothStepAnimation(300, 1);

    public UICheckbox(Supplier<Boolean> get, Consumer<Boolean> set) {
        this.get = get;
        this.set = set;
        checkAnimation.setDirection(get.get() ? Direction.BACKWARDS : Direction.FORWARDS);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        height = 15;
        checkAnimation.setDirection(get.get() ? Direction.BACKWARDS : Direction.FORWARDS);

        float anim = 1f - checkAnimation.getOutput();

        Client.RENDERER.drawModuleRect(x, y, 12, 12);

        {
            float prevAlpha = Client.RENDERER.getCrenderSystem().alpha();
            Client.RENDERER.getCrenderSystem().alpha(anim);
            Color themeColor = ClientSettings.INSTANCE.getColor(0);
            Client.RENDERER.text(IconUse.CHECK, x, y, TextureUse.ICONS, 8, themeColor);

            Client.RENDERER.getCrenderSystem().alpha(prevAlpha);
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (hover(mouseX, mouseY) && button == 0) {
            set.accept(!get.get());
            return true;
        }

        return super.click(mouseX, mouseY, button);
    }
}
