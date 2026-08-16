package gg.topchdlc.vse.utils.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import gg.topchdlc.vse.utils.render.impl.HandFirePipeline;
import gg.topchdlc.vse.utils.render.impl.HandOutlinePipeline;
import gg.topchdlc.vse.utils.render.impl.HandTrailPipeline;
import gg.topchdlc.vse.utils.render.impl.KawaseBlurPipeline;
import gg.topchdlc.vse.utils.render.impl.MaskDiffPipeline;
import gg.topchdlc.vse.utils.render.posteffect.PostEffectContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

/**
 * Create by daun kvass
 */
public class HandShaderRenderer {
    private static HandShaderRenderer instance;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final PostEffectContext ctx = new PostEffectContext();

    private HandFirePipeline firePipeline;
    private HandOutlinePipeline outlinePipeline;
    private HandTrailPipeline handTrail;
    private KawaseBlurPipeline kawaseBlur;
    private MaskDiffPipeline maskDiff;

    private boolean initialized = false;
    private boolean enabled = false;
    private boolean fireEffectEnabled = true;

    private float intensity = 1.0f, radius = 8.0f, speed = 1.0f, flameHeight = 1.0f;
    private float trailBlurRadius = 1.55f, trailSoftness = 1.35f, trailFade = 0.84f;
    private float handBlurRadius = 1.45f, handSoftness = 1.25f, handFade = 0.68f, smoke = 0.55f;
    private float colorR = 0.6f, colorG = 0.2f, colorB = 1.0f, colorA = 1.0f;
    private float attackImpulse = 0.0f, slashImpulse = 0.0f, slashDirection = 1.0f;

    private boolean outlineEnabled = false;
    private float outlineWidth = 1.5f;
    private float outlineGlow = 1.0f;
    private int outlineMode = 0;
    private float outlineR = 1.0f, outlineG = 1.0f, outlineB = 1.0f, outlineA = 1.0f;

    private long startTimeNanos = System.nanoTime();

    public static HandShaderRenderer getInstance() {
        if (instance == null) instance = new HandShaderRenderer();
        return instance;
    }

    private void ensureInitialized() {
        if (initialized) return;
        closePipelines();
        firePipeline = new HandFirePipeline();
        outlinePipeline = new HandOutlinePipeline();
        handTrail = new HandTrailPipeline();
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
            attackImpulse = 0.0f;
            slashImpulse = 0.0f;
            ctx.endFrame();
            if (handTrail != null) handTrail.clear();
        }
    }

    public boolean isEnabled() { return enabled; }

    public void setFireEffectEnabled(boolean v) { this.fireEffectEnabled = v; }
    public void setIntensity(float v) { this.intensity = v; }
    public void setRadius(float v) { this.radius = v; }
    public void setBlurRadius(float v) { this.trailBlurRadius = v; this.handBlurRadius = v; }
    public void setTrailBlurRadius(float v) { this.trailBlurRadius = v; }
    public void setHandBlurRadius(float v) { this.handBlurRadius = v; }
    public void setSpeed(float v) { this.speed = v; }
    public void setFlameHeight(float v) { this.flameHeight = v; }
    public void setTrailSoftness(float v) { this.trailSoftness = v; }
    public void setHandSoftness(float v) { this.handSoftness = v; }
    public void setTrailFade(float v) { this.trailFade = v; }
    public void setHandFade(float v) { this.handFade = v; }
    public void setSmoke(float v) { this.smoke = v; }
    public void setColor(float r, float g, float b, float a) {
        this.colorR = r; this.colorG = g; this.colorB = b; this.colorA = a;
    }

    public void setOutlineEnabled(boolean v) { this.outlineEnabled = v; }
    public void setOutlineWidth(float v) { this.outlineWidth = v; }
    public void setOutlineGlow(float v) { this.outlineGlow = v; }
    public void setOutlineMode(int v) { this.outlineMode = v; }
    public void setOutlineColor(float r, float g, float b, float a) {
        this.outlineR = r; this.outlineG = g; this.outlineB = b; this.outlineA = a;
    }

    public void captureSceneBeforeHands() {
        if (!enabled) return;
        ensureInitialized();
        ctx.captureBefore();
    }

    public void captureSceneAfterHands() {
        if (!enabled) return;
        ensureInitialized();
        ctx.captureAfter();
    }

    public boolean renderFireEffect() {
        if (!enabled || !ctx.isAfterCaptured()) return false;
        ensureInitialized();

        try {
            Framebuffer fb = client.getFramebuffer();
            if (fb == null || fb.getColorAttachment() == null) return false;

            int w = ctx.width();
            int h = ctx.height();

            maskDiff.createMask(ctx.mask(), ctx.sceneBefore(), ctx.sceneAfter(), ctx.depthBefore(), ctx.depthAfter(), w, h);

            float time = (System.nanoTime() - startTimeNanos) / 1_000_000_000.0f;

            if (fireEffectEnabled) {
                float swing = 0.0f;
                if (client.player != null && client.player.handSwinging) {
                    swing = 1.0f - Math.min(1.0f, client.player.handSwingProgress);
                }

                float activity = Math.max(attackImpulse, swing);
                float slash = slashImpulse;
                attackImpulse *= handFade;
                slashImpulse *= trailFade;
                if (attackImpulse < 0.01f) attackImpulse = 0.0f;
                if (slashImpulse < 0.01f) slashImpulse = 0.0f;

                GpuTextureView trailView = handTrail.update(ctx.sceneAfter(), ctx.mask(), w, h, time, intensity, colorR, colorG, colorB, colorA, radius, speed, flameHeight, trailSoftness, trailBlurRadius, smoke, activity, slash, slashDirection, trailFade);

                if (trailView != null) {
                    int blurIterations = handBlurRadius >= 2.15f ? 3 : (handBlurRadius >= 1.05f ? 2 : 1);
                    GpuTextureView smokeView = kawaseBlur.blur(handTrail.texture(), trailView, w, h, blurIterations, Math.max(1.05f, handBlurRadius * 1.22f));
                    if (smokeView == null) smokeView = trailView;

                    firePipeline.composite(fb.getColorAttachmentView(), ctx.sceneAfter(), smokeView, ctx.mask(), w, h, time, intensity, colorR, colorG, colorB, colorA, radius, speed, flameHeight, handSoftness, handBlurRadius, smoke, activity);
                }
            }

            if (outlineEnabled) {
                outlinePipeline.render(
                        fb.getColorAttachmentView(), ctx.sceneAfter(), ctx.mask(),
                        w, h, time, outlineWidth, outlineGlow, outlineMode,
                        outlineR, outlineG, outlineB, outlineA
                );
            }

            return true;
        } finally {
            ctx.endFrame();
        }
    }

    public void close() {
        closePipelines();
        ctx.close();
        initialized = false;
    }

    private void closePipelines() {
        if (firePipeline != null) { firePipeline.close(); firePipeline = null; }
        if (outlinePipeline != null) { outlinePipeline.close(); outlinePipeline = null; }
        if (handTrail != null) { handTrail.close(); handTrail = null; }
        if (kawaseBlur != null) { kawaseBlur.close(); kawaseBlur = null; }
        if (maskDiff != null) { maskDiff.close(); maskDiff = null; }
    }
}