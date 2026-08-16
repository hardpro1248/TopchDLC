package gg.topchdlc.vse.shutki.screen.screens.main;

import gg.topchdlc.Client;
import org.joml.Vector4f;

import java.awt.*;

public final class AnimatedBackground {

    private static float time = 0f;

    private AnimatedBackground() {
    }

    public static void render(float delta, float width, float height) {
        time += delta * 0.05f;
        Vector4f round = new Vector4f(0, 0, 0, 0);

        Color base = new Color(7, 6, 9, 255);
        Client.RENDERER.rect(0, 0, width, height, round, 1f, base, base, base, base);

        int cols = Math.max(48, (int) (width / 6));
        float colW = width / (float) cols;

        for (int i = 0; i < cols; i++) {
            float x = i / (float) cols;

            float wave = (float) (0.5 + 0.5 * Math.sin(x * Math.PI * 1.6 + time * 0.5 + Math.sin(x * Math.PI * 2.4 + time * 0.3) * 1.2));
            float wave2 = (float) (0.5 + 0.5 * Math.cos(x * Math.PI * 2.1 - time * 0.35));
            float mix = Math.max(0.0f, Math.min(1.0f, wave * 0.6f + wave2 * 0.4f));

            int r = (int) (16 + mix * 118 + (1 - mix) * 22);
            int g = (int) (12 + mix * 48 + (1 - mix) * 30);
            int b = (int) (15 + mix * 54 + (1 - mix) * 34);

            Color c = new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)), 255);
            Client.RENDERER.rect(i * colW, 0, colW + 0.5f, height, round, 1f, c, c, c, c);
        }

        Color darkBottom = new Color(0, 0, 0, 130);
        Color darkTop = new Color(0, 0, 0, 0);
        Client.RENDERER.rect(0, 0, width, height * 0.72f, round, 1f, darkTop, darkBottom, darkBottom, darkTop);

        Color vignette = new Color(0, 0, 0, 60);
        Client.RENDERER.rect(0, height * 0.55f, width, height * 0.45f, round, 1f, vignette, vignette, vignette, vignette);
    }
}