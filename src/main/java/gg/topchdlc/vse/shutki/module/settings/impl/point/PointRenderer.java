package gg.topchdlc.vse.shutki.module.settings.impl.point;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.widgets.impl.PointSettingWidget;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.client.client.ClientColors;

public class PointRenderer extends SettingRenderer<PointSetting> {
    public PointRenderer(PointSetting setting) {
        super(setting);

        widget = new PointSettingWidget(this);
    }

    PointSettingWidget widget;

    @Override
    public void render(int mouseX, int mouseY) {
        height = Math.max(20, 20 + drawDesc(16));

        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        float prevAlpha = system.alpha();

        Client.RENDERER.text(setting.getName(), x + 2, y + 3, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);

        {
            float off = Math.max(50, Client.RENDERER.textWidth(setting.getName(), TextureUse.SFMEDIUM, 8) + 10);
            Client.RENDERER.drawModuleRect(x + off, y + 2, 12, 12);
            Client.RENDERER.text(IconUse.GEAR, x + off + 1.5f, y + 4f, TextureUse.ICONS, 7, ClientColors.DARK_GRAY_COLOR);
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (hover(mouseX, mouseY)) {
            Client.GLASS_GUI.WIDGETS.register(this.widget);
        }
        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void release(int button) {
        super.release(button);
    }
}
