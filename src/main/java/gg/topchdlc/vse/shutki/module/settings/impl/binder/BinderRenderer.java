package gg.topchdlc.vse.shutki.module.settings.impl.binder;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.widgets.impl.BinderWidget;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.client.ClientColors;

public class BinderRenderer extends SettingRenderer<BinderSetting> {
    public BinderRenderer(BinderSetting setting) {
        super(setting);
        widget = new BinderWidget(this);
    }

    private final BinderWidget widget;

    @Override
    public void render(int mouseX, int mouseY) {
        height = 15;

        {
            float off = Client.RENDERER.textWidth(setting.getName(), TextureUse.SFMEDIUM, 8) + 12;
            Client.RENDERER.drawModuleRect(x + off, y + 2, 12, 12);
            Client.RENDERER.text(IconUse.GEAR, x + off + 1.5f, y + 4.5f, TextureUse.ICONS, 7, ClientColors.DARK_GRAY_COLOR);
        }
        Client.RENDERER.text(this.setting.getName(), x + 2, y + 3, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (hover(mouseX, mouseY) && button < 2 && button >= 0) {
            this.widget.bound(x + width, y, 160, 200);
            widget.getAnimation().setDirection(Direction.BACKWARDS);
            widget.syncRenderers();
            Client.GLASS_GUI.WIDGETS.register(this.widget);
        }
        return super.click(mouseX, mouseY, button);
    }
}
