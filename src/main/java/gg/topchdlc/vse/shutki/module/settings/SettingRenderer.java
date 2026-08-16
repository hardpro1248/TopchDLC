package gg.topchdlc.vse.shutki.module.settings;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.utils.animations.impl.ElasticAnimation;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import lombok.Getter;
import net.fabricmc.loader.impl.util.StringUtil;

public abstract class SettingRenderer<T extends Setting<?>> extends RendererObject {
    @Getter
    protected T setting;

    public ElasticAnimation visible = new ElasticAnimation(500, 1, 8, 4, false);

    public SettingRenderer(T setting) {
        this.setting = setting;
    }

    protected float drawDesc(float yOffset) {
        return drawDesc(yOffset, 12, 20);
    }

    protected float drawDesc(float yOffset, float lineY, int chars) {
        if (!setting.getDesc().isEmpty()) {
            float off = 0;
            for (String line : StringUtil.wrapLines(setting.getDesc(), chars).split("\n")) {
                Client.RENDERER.text(line, x + 2, y + lineY + off + yOffset, TextureUse.SFMEDIUM, 7, ClientColors.DARK_GRAY_COLOR);
                off += 8;
            }
            return off + yOffset;
        }
        return yOffset;
    }
}