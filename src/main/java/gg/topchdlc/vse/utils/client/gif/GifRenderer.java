package gg.topchdlc.vse.utils.client.gif;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create by daun kvass
 */
public class GifRenderer implements MinecraftHolder {

    private static final Map<String, GifData> loadedGifs = new HashMap<>();

    public static class GifData {
        public final List<Identifier> frames = new ArrayList<>();
        public final List<Integer> delays = new ArrayList<>();
        public final List<NativeImage> pendingImages = new ArrayList<>();

        public int currentFrame = 0;
        public long lastFrameTime = 0;
        public int width, height;
        public boolean loaded = false;
        public boolean registered = false;
        private final String path;

        public GifData(String path) {
            this.path = path;
        }

        public Identifier getCurrentFrame() {
            if (frames.isEmpty() || !registered) return null;

            long now = System.currentTimeMillis();
            int delay = delays.isEmpty() ? 100 : delays.get(currentFrame % delays.size());

            if (now - lastFrameTime >= delay) {
                currentFrame = (currentFrame + 1) % frames.size();
                lastFrameTime = now;
            }

            return frames.get(currentFrame);
        }

        public void registerTextures() {
            if (!MinecraftClient.getInstance().isOnThread()) {
                MinecraftClient.getInstance().execute(this::registerTextures);
                return;
            }

            if (registered || pendingImages == null || pendingImages.isEmpty()) return;

            synchronized (pendingImages) {
                String safePath = (path == null) ? "default" : path;
                int pathHash = Math.abs(safePath.hashCode());

                for (int i = 0; i < pendingImages.size(); i++) {
                    NativeImage img = pendingImages.get(i);
                    if (img == null) continue;

                    Identifier frameId = Identifier.of("topchdlc", "dynamic/gif_" + pathHash + "_" + i);

                    try {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "gif_frame_" + frameId.getPath(), img);

                        MinecraftClient.getInstance().getTextureManager().registerTexture(frameId, texture);
                        frames.add(frameId);
                    } catch (Exception e) {
                        System.err.println("GifRenderer Failed to register frame " + i);
                        e.printStackTrace();
                    }
                }

                pendingImages.clear();
                registered = true;
                lastFrameTime = System.currentTimeMillis();
            }
        }
    }

    public static GifData loadGif(String path) {
        if (loadedGifs.containsKey(path)) {
            GifData existing = loadedGifs.get(path);
            if (existing.loaded && !existing.registered) {
                MinecraftClient.getInstance().execute(existing::registerTextures);
            }
            return existing;
        }

        GifData gifData = new GifData(path);
        loadedGifs.put(path, gifData);

        new Thread(() -> {
            try {
                Identifier id = Identifier.of("topchdlc", path);
                InputStream inputStream = MinecraftClient.getInstance().getResourceManager()
                        .getResource(id)
                        .orElseThrow(() -> new IOException("GIF resource not found: " + path))
                        .getInputStream();

                readGifData(inputStream, gifData);
                inputStream.close();

                MinecraftClient.getInstance().execute(gifData::registerTextures);

            } catch (Exception e) {
                System.err.println("GifRenderer Failed to load GIF: " + path);
                e.printStackTrace();
            }
        }).start();

        return gifData;
    }


    private static void readGifData(InputStream stream, GifData gifData) throws IOException {
        ImageInputStream imageStream = ImageIO.createImageInputStream(stream);
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        reader.setInput(imageStream);

        int numFrames = reader.getNumImages(true);

        BufferedImage master = null;
        Graphics2D masterGraphics = null;

        for (int i = 0; i < numFrames; i++) {
            BufferedImage frame = reader.read(i);
            IIOMetadata metadata = reader.getImageMetadata(i);
            Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
            NodeList children = tree.getChildNodes();

            int delay = 100;
            String disposal = "none";
            int offsetX = 0;
            int offsetY = 0;

            for (int j = 0; j < children.getLength(); j++) {
                Node node = children.item(j);
                if (node.getNodeName().equals("GraphicControlExtension")) {
                    NamedNodeMap attrs = node.getAttributes();
                    delay = Integer.parseInt(attrs.getNamedItem("delayTime").getNodeValue()) * 10;
                    disposal = attrs.getNamedItem("disposalMethod").getNodeValue();
                } else if (node.getNodeName().equals("ImageDescriptor")) {
                    NamedNodeMap attrs = node.getAttributes();
                    offsetX = Integer.parseInt(attrs.getNamedItem("imageLeftPosition").getNodeValue());
                    offsetY = Integer.parseInt(attrs.getNamedItem("imageTopPosition").getNodeValue());
                }
            }

            if (i == 0) {
                gifData.width = reader.getWidth(0);
                gifData.height = reader.getHeight(0);
                master = new BufferedImage(gifData.width, gifData.height, BufferedImage.TYPE_INT_ARGB);
                masterGraphics = master.createGraphics();
                masterGraphics.setBackground(new Color(0, 0, 0, 0));
            }

            if (masterGraphics != null) {
                if ("restoreToBackgroundColor".equals(disposal) && i > 0) {
                    masterGraphics.clearRect(offsetX, offsetY, frame.getWidth(), frame.getHeight());
                }

                masterGraphics.drawImage(frame, offsetX, offsetY, null);
            }

            NativeImage nativeImage = bufferedImageToNativeImage(master);
            if (nativeImage != null) {
                gifData.pendingImages.add(nativeImage);
                gifData.delays.add(Math.max(delay, 20));
            }
        }

        if (masterGraphics != null) masterGraphics.dispose();
        reader.dispose();
        imageStream.close();

        gifData.loaded = true;
    }

    private static NativeImage bufferedImageToNativeImage(BufferedImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = pixels[y * width + x];

                    nativeImage.setColorArgb(x, y, argb);
                }
            }
            return nativeImage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void renderGif(MatrixStack matrices,
                                 GifData gif, float x, float y, float width, float height, float radius) {
        renderGif(matrices, gif, x, y, width, height, radius, -1);
    }

    public static void renderGif(MatrixStack matrices,
                                 GifData gif, float x, float y, float width, float height, float radius, int color) {
        if (gif == null) return;


        if (!gif.loaded || !gif.registered || gif.frames.isEmpty()) return;

        Identifier currentFrame = gif.getCurrentFrame();
        if (currentFrame != null) {
            Color c = new Color(color, true);
            Vector4f rounding = new Vector4f(radius, radius, radius, radius);

            Client.RENDERER.textureRaw(
                    currentFrame,
                    x, y,
                    width, height,
                    1.0f,
                    rounding,
                    c, c, c, c
            );
        }
    }

    public static GifData loadGifFromFile(File file) {
        String path = "file:" + file.getAbsolutePath();

        if (loadedGifs.containsKey(path)) {
            GifData existing = loadedGifs.get(path);
            if (existing.loaded && !existing.registered) {
                existing.registerTextures();
            }
            return existing;
        }

        GifData gifData = new GifData(path);
        loadedGifs.put(path, gifData);

        new Thread(() -> {
            try {
                FileInputStream inputStream = new FileInputStream(file);
                readGifData(inputStream, gifData);
                inputStream.close();
            } catch (Exception e) {
                System.err.println("[GifRenderer] Failed to load custom GIF: " + file.getName());
                e.printStackTrace();
                gifData.loaded = false;
            }
        }).start();

        return gifData;
    }

    public static void cleanup() {
        for (GifData gif : loadedGifs.values()) {
            for (Identifier id : gif.frames) {
                mc.getTextureManager().destroyTexture(id);
            }
        }
        loadedGifs.clear();
    }
}