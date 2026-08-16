package gg.topchdlc.vse.shutki.module.modules.impl.render.esp.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.modules.impl.render.esp.ESPRenderer;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.joml.Vector4f;
import java.awt.Color;
public class SkeletonRenderer extends ESPRenderer {
    public SkeletonRenderer(Align align) {
        super(align);
    }

    @Override
    public Color render(float minX, float minY, float maxX, float maxY, float width, Entity entity) {
        if (!(entity instanceof LivingEntity)) return null;

        float w = (maxX - minX);
        float h = (maxY - minY);
        if (w <= 0 || h <= 0) return null;

        float cx = minX + w / 2f;

        float headTopY = minY;
        float neckY = minY + h * 0.15f;
        float shouldersY = minY + h * 0.22f;
        float chestY = minY + h * 0.30f;
        float pelvisY = minY + h * 0.58f;
        float kneesY = minY + h * 0.80f;
        float feetY = maxY;

        float shoulderOffsetX = w * 0.25f;
        float handOffsetX = w * 0.33f;
        float hipOffsetX = w * 0.18f;

        float leftShoulderX = minX + shoulderOffsetX;
        float rightShoulderX = maxX - shoulderOffsetX;
        float leftHandX = minX + handOffsetX;
        float rightHandX = maxX - handOffsetX;
        float leftHipX = cx - hipOffsetX;
        float rightHipX = cx + hipOffsetX;

        Color c0 = ClientSettings.INSTANCE.getColor(0);

        vline(cx, headTopY, neckY, width, c0);
        vline(cx, neckY, pelvisY, width, c0);

        hline(leftShoulderX, rightShoulderX, shouldersY, width, c0);

        vline(leftShoulderX, shouldersY, chestY, width, c0);
        vline(rightShoulderX, shouldersY, chestY, width, c0);
        vline(leftHandX, chestY, chestY + h * 0.12f, width, c0);
        vline(rightHandX, chestY, chestY + h * 0.12f, width, c0);

        hline(leftHipX, rightHipX, pelvisY, width, c0);

        vline(leftHipX, pelvisY, kneesY, width, c0);
        vline(rightHipX, pelvisY, kneesY, width, c0);
        vline(leftHipX, kneesY, feetY, width, c0);
        vline(rightHipX, kneesY, feetY, width, c0);
        return c0;
    }

    private void hline(float x1, float x2, float y, float thickness, Color color) {
        if (x2 < x1) { float t = x1; x1 = x2; x2 = t; }
        Client.RENDERER.rect(x1, y - thickness / 2f, (x2 - x1), thickness, new Vector4f(0), 0,
                color, color, color, color);
    }

    private void vline(float x, float y1, float y2, float thickness, Color color) {
        if (y2 < y1) { float t = y1; y1 = y2; y2 = t; }
        Client.RENDERER.rect(x - thickness / 2f, y1, thickness, (y2 - y1), new Vector4f(0), 0,
                color, color, color, color);
    }
}
