package gg.topchdlc.vse.utils.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import gg.topchdlc.vse.utils.render.impl.HandFirePipeline;
import gg.topchdlc.vse.utils.render.impl.HandTrailPipeline;
import gg.topchdlc.vse.utils.render.impl.KawaseBlurPipeline;
import gg.topchdlc.vse.utils.render.impl.MaskDiffPipeline;
import gg.topchdlc.vse.utils.render.posteffect.PostEffectContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import java.awt.Color;

/**
 * Create by daun kvass
 */
public class BlockHighlightRenderer {
    private static BlockHighlightRenderer instance;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final PostEffectContext ctx = new PostEffectContext();
    private HandFirePipeline firePipeline;
    private HandTrailPipeline trailPipeline;
    private KawaseBlurPipeline kawaseBlur;
    private MaskDiffPipeline maskDiff;

    private boolean initialized = false;
    private boolean enabled = false;
    private float intensity = 1.0f;
    private float radius = 0.35f;
    private float speed = 1.0f;
    private float flameHeight = 1.0f;
    private float trailBlurRadius = 1.55f;
    private float trailSoftness = 1.35f;
    private float trailFade = 0.84f;
    private float glowBlurRadius = 1.45f;
    private float glowSoftness = 1.25f;
    private float glowFade = 0.68f;
    private float smoke = 0.55f;

    private long startTimeNanos = System.nanoTime();

    public static BlockHighlightRenderer getInstance() {
        if (instance == null) {
            instance = new BlockHighlightRenderer();
        }
        return instance;
    }

    private void ensureInitialized() {
        if (initialized) return;
        closePipelines();
        firePipeline = new HandFirePipeline();
        trailPipeline = new HandTrailPipeline();
        kawaseBlur = new KawaseBlurPipeline();
        maskDiff = new MaskDiffPipeline();
        startTimeNanos = System.nanoTime();
        initialized = true;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            ensureInitialized();
        } else {
            ctx.endFrame();
            if (trailPipeline != null) trailPipeline.clear();
        }
    }

    public boolean isEnabled() { return enabled; }

    public void setIntensity(float v) { this.intensity = v; }
    public void setRadius(float v) { this.radius = v; }
    public void setSpeed(float v) { this.speed = v; }
    public void setFlameHeight(float v) { this.flameHeight = v; }
    public void setTrailFade(float v) { this.trailFade = v; }
    public void setTrailSoftness(float v) { this.trailSoftness = v; }
    public void setTrailBlurRadius(float v) { this.trailBlurRadius = v; }
    public void setGlowFade(float v) { this.glowFade = v; }
    public void setGlowSoftness(float v) { this.glowSoftness = v; }
    public void setGlowBlurRadius(float v) { this.glowBlurRadius = v; }
    public void setSmoke(float v) { this.smoke = v; }

    public void captureBefore() {
        if (!enabled) return;
        ensureInitialized();
        ctx.captureBefore();
    }

    public void captureAfter() {
        if (!enabled) return;
        ensureInitialized();
        ctx.captureAfter();
    }

    public boolean renderGlowEffect(Color color) {
        if (!enabled || !ctx.isAfterCaptured()) return false;
        ensureInitialized();

        try {
            Framebuffer fb = client.getFramebuffer();
            if (fb == null || fb.getColorAttachment() == null) return false;

            int w = ctx.width();
            int h = ctx.height();

            maskDiff.createMask(ctx.mask(), ctx.sceneBefore(), ctx.sceneAfter(), ctx.depthBefore(), ctx.depthAfter(), w, h);

            float time = (System.nanoTime() - startTimeNanos) / 1_000_000_000.0f;

            float r = color.getRed() / 255.0f;
            float g = color.getGreen() / 255.0f;
            float b = color.getBlue() / 255.0f;
            float a = color.getAlpha() / 255.0f;

            float activity = 0.6f;
            float slash = 0.0f;
            float slashDirection = 1.0f;

            GpuTextureView trailView = trailPipeline.update(
                    ctx.sceneAfter(), ctx.mask(), w, h, time, intensity,
                    r, g, b, a, radius, speed, flameHeight,
                    trailSoftness, trailBlurRadius, smoke,
                    activity, slash, slashDirection, trailFade
            );

            if (trailView == null) return false;

            int blurIterations = glowBlurRadius >= 2.15f ? 3 : (glowBlurRadius >= 1.05f ? 2 : 1);
            GpuTextureView smokeView = kawaseBlur.blur(
                    trailPipeline.texture(), trailView, w, h,
                    blurIterations, Math.max(1.05f, glowBlurRadius * 1.22f)
            );

            if (smokeView == null) smokeView = trailView;

            firePipeline.composite(
                    fb.getColorAttachmentView(), ctx.sceneAfter(), smokeView, ctx.mask(),
                    w, h, time, intensity, r, g, b, a, radius, speed,
                    flameHeight, glowSoftness, glowBlurRadius, smoke, activity
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            ctx.endFrame();
        }
    }

    public void invalidate() {
        closePipelines();
        initialized = false;
    }

    public void close() {
        closePipelines();
        ctx.close();
        initialized = false;
    }

    private void closePipelines() {
        if (firePipeline != null) { firePipeline.close(); firePipeline = null; }
        if (trailPipeline != null) { trailPipeline.close(); trailPipeline = null; }
        if (kawaseBlur != null) { kawaseBlur.close(); kawaseBlur = null; }
        if (maskDiff != null) { maskDiff.close(); maskDiff = null; }
    }
}