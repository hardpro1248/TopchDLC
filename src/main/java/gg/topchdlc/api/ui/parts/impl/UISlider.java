package gg.topchdlc.api.ui.parts.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UISlider extends RendererObject {
    public float min, max;
    private float animation = -999;
    public Consumer<Float> callback;
    public Supplier<Float> get, increment;
    private boolean dragging;

    public UISlider(Supplier<Float> get, float min, float max, Supplier<Float> increment, Consumer<Float> callback) {
        this.get = get;
        this.min = min;
        this.max = max;
        this.increment = increment;
        this.callback = callback;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (dragging) {
            updateValue(mouseX);
        }
        float value = this.get.get();
        float progress = MathHelper.clamp((value - min) / (max - min), 0, 1);

        if (animation == -999) {
            animation = progress;
        }
        animation = MathUtility.linearFps(animation, progress, 10);

        Color thumb = (ClientColors.DISABLED);
        Color shar1 = ClientSettings.INSTANCE.getColor(0);
        Color shar2 = ClientSettings.INSTANCE.getColor(90);

        Color progressColor1 = ColorUtility.blend(shar1, Color.BLACK, 0.4f);
        Color progressColor2 = ColorUtility.blend(shar2, Color.BLACK, 0.4f);
        progressColor1 = ColorUtility.injectAlpha(progressColor1, 200);
        progressColor2 = ColorUtility.injectAlpha(progressColor2, 200);

        float progressWidth = width * animation;

        Client.RENDERER.rect(x, y + height / 2F - 1.5F, width, 3, new Vector4f(1.5F), 1,
                thumb, thumb, thumb, thumb);
        Client.RENDERER.rect(x, y + height / 2F - 1.5F, progressWidth, 3, new Vector4f(1.5F), 1,
                progressColor1, progressColor1, progressColor2, progressColor2);

        float thumbX = x + progressWidth - 3.5f;
        Client.RENDERER.rect(thumbX, y + height / 2F - 2.5F, 7, 5, new Vector4f(3), 1, shar1, shar1, shar2, shar2);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (MathUtility.mouseIn(x - 2, y, width + 4, height, mouseX, mouseY) && button == 0) {
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

    private void updateValue(int mouseX) {
        float progress = MathUtility.clamp((mouseX - x) / width, 0f, 1f);
        float newValue = min + progress * (max - min);

        float increment = this.increment.get();
        if (increment > 0f) {
            newValue = Math.round(newValue / increment) * increment;
        }

        callback.accept(newValue);
    }
}