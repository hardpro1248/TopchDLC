package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.ClientRenderer;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.utils.media.MediaPlayer;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Create by daun kvass
 */
public class DynamicIsland extends Module {
    public static final DynamicIsland INSTANCE = new DynamicIsland();

    private final EnumSetting<Style> style = enumSetting("Стиль", Style.GLASS);

    private final CheckBox showTime = checkbox("Show Time", true);
    private final CheckBox showMusic = checkbox("Show Music", true);
    private final CheckBox showBars = checkbox("Show Bars", true);
    private final CheckBox hideBossBar = checkbox("Hide BossBar", false);

    private final MediaPlayer mediaPlayer = new MediaPlayer();

    private enum Mode { DEFAULT, MUSIC }

    private float defaultAnimation = 1f;
    private float musicAnimation = 0f;
    private float animatedWidth = 60f;

    private float[] targetBarHeights = new float[]{4f, 3f, 2.5f};
    private float[] currentBarHeights = new float[]{4f, 3f, 2.5f};
    private long lastBarUpdate = 0;

    private final Map<UUID, Float> bossBarAnimations = new java.util.HashMap<>();

    public DynamicIsland() {
        super("DynamicIsland", Category.RENDER, "хы хы айпхон но не айпхон");
        setEnabled(true, false);
    }

    @Override
    protected void onEnable() {
        mediaPlayer.start();
    }

    @Override
    protected void onDisable() {
        mediaPlayer.shutdown();
    }

    private void renderbackground(ClientRenderer renderer,
                                  float rx, float ry, float rw, float rh,
                                  Vector4f radius, float alpha) {

        renderer.blur(rx, ry, rw, rh, radius, 15f, alpha);

        if (style.get() == Style.DARK) {
            Color mainBg = new Color(20, 20, 25, (int) (180 * alpha));
            renderer.rect(rx, ry, rw, rh, radius, 1, mainBg, mainBg, mainBg, mainBg);

            Color outline = new Color(255, 255, 255, (int) (15 * alpha));
            renderer.outline(rx, ry, rw, rh, 0.4f, radius, new Vector2f(1), outline, outline, outline, outline);
        } else {
            Color mainBg = new Color(62, 62, 71, 0);
            renderer.rect(rx, ry, rw, rh, radius, 1, mainBg, mainBg, mainBg, mainBg);

            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color outline = new Color(255, 255, 255, outAlpha);
            renderer.outline(rx, ry, rw, rh, 0.4f, radius, new Vector2f(1), outline, outline, outline, outline);
        }
    }

    public void renderBossBars(DrawContext context, Map<UUID, ClientBossBar> bossBars) {
        if (hideBossBar.get() || bossBars.isEmpty()) return;

        ClientRenderer renderer = Client.RENDERER;
        int screenWidth = mc.getWindow().getScaledWidth();

        float barW = 215f;
        float barH = 5f;
        float fontSize = 7f;
        float padding = 8f;

        float startY = 22f;
        float currentY = startY;

        for (Map.Entry<UUID, ClientBossBar> entry : bossBars.entrySet()) {
            ClientBossBar bossBar = entry.getValue();
            UUID id = entry.getKey();

            float targetProgress = bossBar.getPercent();
            float currentProgress = bossBarAnimations.getOrDefault(id, targetProgress);
            currentProgress = lerp(currentProgress, targetProgress, 0.1f);
            bossBarAnimations.put(id, currentProgress);

            String name = bossBar.getName().getString();
            float containerH = barH + fontSize + padding * 2 + 4f;


            float nameX = screenWidth / 2f - renderer.textWidth(name, TextureUse.SFMEDIUM, fontSize) / 2f;
            float nameY = currentY + padding;
            renderer.text(name, nameX, nameY, TextureUse.SFMEDIUM, fontSize, getColorForBossBar(bossBar));

            float barX = screenWidth / 2f - barW / 2f;
            float barY = nameY + fontSize + 4f;

            Color barSlotBg = new Color(255, 255, 255, 15);
            Vector4f barRadius = new Vector4f(barH / 2f);
            renderer.rect(barX, barY, barW, barH, barRadius, 1, barSlotBg, barSlotBg, barSlotBg, barSlotBg);

            if (currentProgress > 0.005f) {
                float fillW = Math.max(barW * currentProgress, barH);
                Color c1 = getBarFillColor(bossBar);
                Color c2 = getBarFillColor2(bossBar);
                renderer.rect(barX, barY, fillW, barH, barRadius, 1, c1, c2, c2, c1);
            }

            String percent = (int) (currentProgress * 100) + "%";
            float percentW = renderer.textWidth(percent, TextureUse.SFMEDIUM, fontSize - 1);
            renderer.text(percent, screenWidth / 2f - percentW / 2f,
                    barY + (barH - fontSize) / 2f,
                    TextureUse.SFMEDIUM, fontSize - 1, new Color(255, 255, 255, 200));

            currentY += containerH + 6f;
            if (currentY >= mc.getWindow().getScaledHeight() / 3f) break;
        }

        bossBarAnimations.keySet().retainAll(bossBars.keySet());
    }

    private Color getColorForBossBar(BossBar b) {
        return switch (b.getColor()) {
            case PINK -> new Color(255, 130, 170);
            case BLUE -> new Color(100, 180, 255);
            case RED -> new Color(255, 100, 100);
            case GREEN -> new Color(100, 255, 130);
            case YELLOW -> new Color(255, 230, 100);
            case PURPLE -> new Color(180, 130, 255);
            case WHITE -> new Color(230, 230, 230);
        };
    }

    private Color getBarFillColor(BossBar b) {
        Color c = getColorForBossBar(b);
        return new Color(Math.min(255, c.getRed() + 20), Math.min(255, c.getGreen() + 20), Math.min(255, c.getBlue() + 20), 220);
    }

    private Color getBarFillColor2(BossBar b) {
        Color c = getColorForBossBar(b);
        return new Color(Math.max(0, c.getRed() - 30), Math.max(0, c.getGreen() - 30), Math.max(0, c.getBlue() - 30), 220);
    }

    public void render(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        mediaPlayer.onTick();
        updateAnimations();

        ClientRenderer renderer = Client.RENDERER;
        int screenWidth = mc.getWindow().getScaledWidth();

        String clientName = "TopchDLC";
        String track = mediaPlayer.getTitle();
        String artist = mediaPlayer.getArtist();
        String fullTrack = track + (artist.isEmpty() ? "" : " - " + artist);

        float padding = 8f;
        float baseHeight = 15f;
        float fontSize = 7f;
        float maxW = 350f;

        float defaultWidth = Math.min(renderer.textWidth(clientName, TextureUse.SFMEDIUM, fontSize) + padding * 2, maxW);
        float musicWidth = Math.min(renderer.textWidth(fullTrack, TextureUse.SFMEDIUM, fontSize) + padding * 2, maxW);

        float targetWidth = defaultWidth * defaultAnimation + musicWidth * musicAnimation;
        animatedWidth = lerp(animatedWidth, Math.max(46, targetWidth), 0.12f);

        float x = screenWidth / 2f - animatedWidth / 2f;
        float y = 4f;
        float h = baseHeight;
        Vector4f cornerRadius = new Vector4f(h / 2f);

        renderbackground(renderer, x, y, animatedWidth, h, cornerRadius, 1.0f);

        float textY = y + (h - renderer.textHeight(TextureUse.SFMEDIUM, fontSize)) / 2f;

        if (defaultAnimation > 0.01f) {
            int a = (int) (255 * defaultAnimation);
            renderer.textCentered(clientName, x + animatedWidth / 2f, textY,
                    TextureUse.SFMEDIUM, fontSize, new Color(255, 255, 255, a));
        }

        if (musicAnimation > 0.01f) {
            int a = (int) (255 * musicAnimation);
            float maxAvail = maxW - padding * 2;
            String displayTrack = fullTrack;
            if (renderer.textWidth(displayTrack, TextureUse.SFMEDIUM, fontSize) > maxAvail) {
                while (renderer.textWidth(displayTrack + "...", TextureUse.SFMEDIUM, fontSize)
                        > maxAvail && displayTrack.length() > 3)
                    displayTrack = displayTrack.substring(0, displayTrack.length() - 1);
                displayTrack += "...";
            }
            renderer.textCentered(displayTrack, x + animatedWidth / 2f, textY,
                    TextureUse.SFMEDIUM, fontSize, new Color(255, 255, 255, a));
        }

        if (showTime.get()) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            float timeWidth = renderer.textWidth(time, TextureUse.SFMEDIUM, fontSize);
            renderer.text(time, x - timeWidth - 6f, textY,
                    TextureUse.SFMEDIUM, fontSize, new Color(255, 255, 255, 200));
        }

        if (showBars.get() && musicAnimation > 0.01f && mediaPlayer.isPlaying()) {
            int a = (int) (200 * musicAnimation);
            float barX = x + animatedWidth + 5f;
            float baseBarY = y + 2.5f;
            Color barColor = new Color(200, 210, 230, a);
            for (int i = 0; i < 3; i++) {
                float barY = baseBarY + (10 - currentBarHeights[i]) / 2f;
                renderer.rect(barX + i * 3.5f, barY, 2f, currentBarHeights[i],
                        new Vector4f(1f), 1, barColor, barColor, barColor, barColor);
            }
        }
    }

    private void updateAnimations() {
        boolean hasMedia = showMusic.get() && mediaPlayer.hasMedia();
        Mode targetMode = hasMedia ? Mode.MUSIC : Mode.DEFAULT;
        float speed = 0.1f;
        defaultAnimation = lerp(defaultAnimation, targetMode == Mode.DEFAULT ? 1f : 0f, speed);
        musicAnimation = lerp(musicAnimation, targetMode == Mode.MUSIC ? 1f : 0f, speed);

        if (hasMedia && System.currentTimeMillis() - lastBarUpdate > 100) {
            updateBarHeights();
            lastBarUpdate = System.currentTimeMillis();
        }
        for (int i = 0; i < 3; i++)
            currentBarHeights[i] = lerp(currentBarHeights[i], targetBarHeights[i], 0.25f);
    }

    private void updateBarHeights() {
        for (int i = 0; i < 3; i++)
            targetBarHeights[i] = 2f + (float) Math.random() * 5.5f;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}