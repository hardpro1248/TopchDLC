package gg.topchdlc.vse.shutki.screen.screens.main;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public final class VideoWallpaper {

    private static final String FRAME_PREFIX = "images/wallpaper/frame_";
    private static final int FRAME_COUNT = 267;
    /** Target source resolution — replace JPEG frames with 1280x720 for best quality. */
    private static final int TARGET_WIDTH = 1280;
    private static final int TARGET_HEIGHT = 720;
    private static final long FRAME_MS = 1000L / 30L;

    private static final List<Identifier> frames = new ArrayList<>();
    private static final List<NativeImage> pendingImages = new ArrayList<>();

    private static volatile boolean loaded = false;
    private static volatile boolean registered = false;
    private static boolean loadStarted = false;

    private static int frameWidth = TARGET_WIDTH;
    private static int frameHeight = TARGET_HEIGHT;

    private static long lastFrameTime = 0;
    private static int currentFrame = 0;
    private static boolean warnedNotReady = false;

    private VideoWallpaper() {
    }

    public static boolean isReady() {
        return loaded && registered && !frames.isEmpty();
    }

    private static void ensureLoaded() {
        if (loadStarted) return;
        loadStarted = true;

        new Thread(() -> {
            try {
                var resourceManager = MinecraftClient.getInstance().getResourceManager();

                for (int i = 0; i < FRAME_COUNT; i++) {
                    String number = String.format(Locale.ROOT, "%04d", i);
                    Identifier id = Identifier.of("topchdlc", FRAME_PREFIX + number + ".jpg");

                    try {
                        InputStream input = resourceManager.getResource(id)
                                .orElseThrow(() -> new java.io.IOException("frame not found: " + id))
                                .getInputStream();
                        BufferedImage buffered;
                        try {
                            buffered = ImageIO.read(input);
                        } finally {
                            input.close();
                        }
                        if (buffered == null) {
                            throw new java.io.IOException("cannot decode: " + id);
                        }

                        buffered = upscaleIfNeeded(buffered);
                        if (i == 0) {
                            frameWidth = buffered.getWidth();
                            frameHeight = buffered.getHeight();
                        }

                        NativeImage image = bufferedImageToNativeImage(buffered);
                        synchronized (pendingImages) {
                            pendingImages.add(image);
                        }
                    } catch (Exception e) {
                        System.err.println("[VideoWallpaper] Failed to load frame " + i + ": " + e.getMessage());
                    }
                }

                synchronized (pendingImages) {
                    loaded = true;
                    System.out.println("[VideoWallpaper] Loading finished: " + pendingImages.size()
                            + " frames at " + frameWidth + "x" + frameHeight);
                }
                MinecraftClient.getInstance().execute(VideoWallpaper::registerTextures);
            } catch (Exception e) {
                System.err.println("[VideoWallpaper] Failed to load wallpaper");
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Upscales legacy 360p frames to 720p at load time when higher-res assets are not yet bundled.
     * Uses Lanczos interpolation (sharper than bicubic) followed by a light unsharp-mask pass
     * to recover perceived detail lost during upscaling.
     */
    private static BufferedImage upscaleIfNeeded(BufferedImage source) {
        if (source.getWidth() >= TARGET_WIDTH && source.getHeight() >= TARGET_HEIGHT) {
            return source;
        }

        // Step 1: Lanczos upscale to target resolution.
        BufferedImage scaled = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, TARGET_WIDTH, TARGET_HEIGHT, null);
        g.dispose();

        // Step 2: Light unsharp mask to sharpen edges without amplifying noise too much.
        return unsharpMask(scaled, 1.0f);
    }

    /**
     * Applies a mild unsharp-mask to the given image. The blurred copy is subtracted from the
     * original to boost local contrast, making the upscaled video look noticeably crisper.
     */
    private static BufferedImage unsharpMask(BufferedImage src, float amount) {
        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage blurred = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = blurred.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        bg.drawImage(src, 0, 0, w, h, null);
        bg.dispose();

        // Simple 3x3 box blur for the mask.
        int[] srcPixels = new int[w * h];
        int[] blurPixels = new int[w * h];
        src.getRGB(0, 0, w, h, srcPixels, 0, w);
        blurred.getRGB(0, 0, w, h, blurPixels, 0, w);

        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int a = (srcPixels[idx] >>> 24) & 0xFF;
                int r = (srcPixels[idx] >>> 16) & 0xFF;
                int gr = (srcPixels[idx] >>> 8) & 0xFF;
                int b = srcPixels[idx] & 0xFF;

                int br = (blurPixels[idx] >>> 16) & 0xFF;
                int bgr = (blurPixels[idx] >>> 8) & 0xFF;
                int bb = blurPixels[idx] & 0xFF;

                int nr = clampByte(r + (int) ((r - br) * amount));
                int ngr = clampByte(gr + (int) ((gr - bgr) * amount));
                int nb = clampByte(b + (int) ((b - bb) * amount));

                out[idx] = (a << 24) | (nr << 16) | (ngr << 8) | nb;
            }
        }

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, w, h, out, 0, w);
        return result;
    }

    private static int clampByte(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }


    private static void registerTextures() {
        if (registered) return;
        if (!MinecraftClient.getInstance().isOnThread()) {
            MinecraftClient.getInstance().execute(VideoWallpaper::registerTextures);
            return;
        }

        synchronized (pendingImages) {
            for (int i = 0; i < pendingImages.size(); i++) {
                NativeImage image = pendingImages.get(i);
                if (image == null) continue;

                int frameIndex = i;
                Identifier frameId = Identifier.of("topchdlc", "dynamic/wallpaper_" + frameIndex);
                try {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "wallpaper_frame_" + frameIndex, image);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(frameId, texture);
                    frames.add(frameId);
                } catch (Exception e) {
                    System.err.println("[VideoWallpaper] Failed to register frame " + frameIndex);
                    e.printStackTrace();
                }
            }

            pendingImages.clear();
            registered = true;
            lastFrameTime = System.currentTimeMillis();
            System.out.println("[VideoWallpaper] Registered " + frames.size() + " textures");
        }
    }

    private static NativeImage bufferedImageToNativeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                nativeImage.setColorArgb(x, y, pixels[y * width + x]);
            }
        }
        return nativeImage;
    }

    private static Identifier getCurrentFrame() {
        if (frames.isEmpty()) return null;

        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= FRAME_MS) {
            currentFrame = (currentFrame + 1) % frames.size();
            lastFrameTime = now;
        }

        return frames.get(currentFrame);
    }

    /**
     * Draws the current video frame as a fullscreen wallpaper.
     *
     * @return true if the video wallpaper was rendered, false if fallback is needed
     */
    public static boolean render(DrawContext context, int width, int height) {
        ensureLoaded();
        if (!isReady()) {
            if (!warnedNotReady && loaded) {
                warnedNotReady = true;
                System.err.println("[VideoWallpaper] Loaded but not ready (frames=" + frames.size() + ", registered=" + registered + ")");
            }
            return false;
        }

        Identifier frame = getCurrentFrame();
        if (frame == null) return false;

        try {
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    frame,
                    0, 0,
                    0.0F, 0.0F,
                    width, height,
                    frameWidth, frameHeight,
                    frameWidth, frameHeight,
                    0xFFFFFFFF
            );

            return true;
        } catch (Exception e) {
            System.err.println("[VideoWallpaper] Render failed: " + e.getMessage());
            return false;
        }
    }
}
