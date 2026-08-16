package gg.topchdlc.vse.shutki.module.modules.impl.render.esp.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.ClientRenderer;
import gg.topchdlc.vse.shutki.module.modules.impl.render.ESP;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.ESPRenderer;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;

import java.awt.*;

public class BoxRenderer extends ESPRenderer {
    public BoxRenderer(Align align) {
        super(align);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public Color render(float minX, float minY, float maxX, float maxY, float width, Entity entity) {
        float corner = ESP.INSTANCE.corners.get() ? ESP.INSTANCE.cornersLenght.get() : 1;

        float acorner = 1F - corner;

        float w = (maxX - minX);
        float h = (maxY - minY);

        Color color1, color2;
       {
            color1 = ClientSettings.INSTANCE.getColor(0);
            color2 = ClientSettings.INSTANCE.getColor(90);
        }

        int a = (int) (corner);
        int b = (int) (w / corner);

        ClientRenderer render = Client.RENDERER;
        render.rect(minX, minY, w * corner, width, new Vector4f(0), 0,
                color1, color1, color2, color2);
        render.rect(minX + w * acorner, minY, w - w * acorner, width, new Vector4f(0), 0,
                color1, color1, color2, color2);

        render.rect(minX, maxY, w * corner, width, new Vector4f(0), 0,
                color2, color2, color1, color1);
        render.rect(minX + w * acorner, maxY, w - w * acorner + width, width, new Vector4f(0), 0,
                color2, color2, color1, color1);

        render.rect(minX, minY, width, h * corner, new Vector4f(0), 0,
                color1, color2, color2, color1);
        render.rect(minX, minY + h * acorner, width, h - h * acorner, new Vector4f(0), 0,
                color1, color2, color2, color1);

        render.rect(maxX, minY, width, h * corner, new Vector4f(0), 0,
                color2, color1, color1, color2);
        render.rect(maxX, minY + h * acorner, width, h - h * acorner, new Vector4f(0), 0,
                color2, color1, color1, color2);
        return color1;
    }
}
