package gg.topchdlc.vse.shutki.module.modules.impl.render.esp.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.ClientRenderer;
import gg.topchdlc.vse.shutki.module.modules.impl.render.ESP;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.ESPRenderer;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;

import java.awt.*;

public class GlowRenderer extends ESPRenderer {
    public GlowRenderer(Align align) {
        super(align);
    }

    public Color render(float minX, float minY, float maxX, float maxY, float width, Entity entity) {
        float w = maxX - minX;
        float h = maxY - minY;

        float expand = ESP.INSTANCE.glowExpand.get();
        float smooth = ESP.INSTANCE.glowSmooth.get();

        Color color1 = ClientSettings.INSTANCE.getColor(0);
        Color color2 = ClientSettings.INSTANCE.getColor(90);

        ClientRenderer render = Client.RENDERER;

        Color glowColor = new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), (int) (ESP.INSTANCE.glowAlpha.get() * 255));
        Color glowColor2 = new Color(color2.getRed(), color2.getGreen(), color2.getBlue(), (int) (ESP.INSTANCE.glowAlpha.get() * 255));

        render.glow(minX - expand, minY - expand, w + expand * 2, h + expand * 2, new Vector4f(0), smooth, glowColor, glowColor, glowColor2, glowColor2);
        render.glow(minX - expand * 0.5F, minY - expand * 0.5F, w + expand, h + expand, new Vector4f(0), smooth, glowColor2, glowColor2, glowColor, glowColor);

        return color1;
    }
}
