package gg.topchdlc.api.ui.parts.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.vse.shutki.module.settings.impl.range.FloatRange;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIRange extends RendererObject {
    public float min, max, increment;
    public Consumer<Float> setMin, setMax;
    public Supplier<FloatRange> get;

    private float animation1, animation2;
    private boolean dragging;

    public UIRange(float min, float max, float increment, Consumer<Float> setMin, Consumer<Float> setMax, Supplier<FloatRange> get) {
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.setMin = setMin;
        this.setMax = setMax;
        this.get = get;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (dragging) update(mouseX);
        FloatRange value = this.get.get();
        float v1 = value.min;
        float v2 = value.max;
        float progress1 = MathHelper.clamp((v1 - min) / (max - min), 0, 1);
        float progress2 = MathHelper.clamp((v2 - min) / (max - min), 0, 1);

        animation1 = MathUtility.linearFps(animation1, progress1, 10);
        animation2 = MathUtility.linearFps(animation2, progress2, 10);

        Color body = ClientColors.DISABLED;
        Color progressColor1 = ClientSettings.INSTANCE.getColor(0);
        Color progressColor2 = ClientSettings.INSTANCE.getColor(90);

        float progressOffset = width * animation1;
        float progressWidth = width * (animation2 - animation1);

        Client.RENDERER.rect(x, y + height / 2F - 1.5F, width, 3, new Vector4f(1.5F), 1,
                body, body, body, body);
        Client.RENDERER.rect(x + progressOffset, y + height / 2F - 1.5F, progressWidth, 3, new Vector4f(1.5F), 1,
                progressColor1, progressColor1, progressColor2, progressColor2);

        Client.RENDERER.rect(x + progressOffset - 2, y + height / 2F - 2, 4, 4, new Vector4f(3), 1, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR);
        Client.RENDERER.rect(x + (width * animation2) - 2, y + height / 2F - 2, 4, 4, new Vector4f(3), 1, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR, ClientColors.DARK_GRAY_COLOR);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (MathUtility.mouseIn(x, y - (height * 0.5f), width, height * 1.2f, mouseX, mouseY) && button == 0) {
            dragging = true;
            return true;
        } else {
            return super.click(mouseX, mouseY, button);
        }
    }

    @Override
    public void release(int button) {
        if (button == 0)
            dragging = false;
    }

    private void update(int mx) {
        float mouseX = (float) mx;
        FloatRange current = get.get();
        float progress = MathUtility.clamp((mouseX - x) / width, 0f, 1f);
        float newValue = min + progress * (max - min);
        if (increment > 0f) {
            newValue = Math.round(newValue / increment) * increment;
        }
        float distMin = Math.abs(newValue - current.min);
        float distMax = Math.abs(newValue - current.max);
        if (distMin <= distMax) {
            setMin.accept(newValue);
        } else setMax.accept(newValue);
    }
}
