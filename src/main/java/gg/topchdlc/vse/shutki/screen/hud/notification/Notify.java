package gg.topchdlc.vse.shutki.screen.hud.notification;

import gg.topchdlc.vse.shutki.module.modules.impl.misc.Nottification;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.main.NotificationElement;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.NurickNotifyRenderer;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.*;

@Getter
public class Notify extends RendererObject {
    private final Text text;
    private IconUse icon;
    private final long duration;
    private final String item;
    private final net.minecraft.item.ItemStack itemStack;

    private float lerpX = -999, lerpY = -999;
    private final SmoothStepAnimation animation;
    private boolean isDemo = false;
    private boolean forcedExpire = false;

    private static final float HEIGHT = 14.5f;
    private static final float SPACING = 1.5f;
    private static final float TEXT_SIZE = 7.0f;

    public Notify markDemo() { this.isDemo = true; return this; }

    public void dismiss() {
        this.forcedExpire = true;
    }

    public Notify(Text text, IconUse icon, long duration) {
        this.text = text; this.icon = icon; this.duration = System.currentTimeMillis() + duration;
        this.item = null; this.itemStack = null;
        this.animation = new SmoothStepAnimation(300, 1, Direction.BACKWARDS);
        this.animation.setDirection(Direction.FORWARDS);
    }

    public Notify(Text text, String item, long duration) {
        this.text = text; this.icon = null; this.item = item; this.itemStack = null;
        this.duration = System.currentTimeMillis() + duration;
        this.animation = new SmoothStepAnimation(300, 1, Direction.BACKWARDS);
        this.animation.setDirection(Direction.FORWARDS);
    }

    public Notify(Text text, net.minecraft.item.ItemStack stack, long duration) {
        this.text = text; this.icon = null; this.item = null; this.itemStack = stack.copy();
        this.duration = System.currentTimeMillis() + duration;
        this.animation = new SmoothStepAnimation(300, 1, Direction.BACKWARDS);
        this.animation.setDirection(Direction.FORWARDS);
    }

    public boolean shouldRemove() {
        return !isDemo && (forcedExpire || System.currentTimeMillis() >= this.duration) && animation.getDirection() == Direction.BACKWARDS && animation.isDone();
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (lerpX == -999 || lerpY == -999) { lerpX = x; lerpY = y; }

        lerpX = MathUtility.linearFps(lerpX, x, 12);
        lerpY = MathUtility.linearFps(lerpY, y, 12);

        if (!this.isDemo) {
            if (this.forcedExpire) {
                this.animation.setDirection(Direction.BACKWARDS);
            } else {
                this.animation.setDirection(this.duration - System.currentTimeMillis() > 300 ? Direction.FORWARDS : Direction.BACKWARDS);
            }
        }

        float alpha = (float) this.animation.getOutput();
        if (alpha < 0.001f) return;

        float renderX = lerpX;
        float renderY = lerpY;

        if (this.animation.getDirection() == Direction.FORWARDS) {
            renderY -= (1.0f - alpha) * 15f;
        } else {
            renderX += (1.0f - alpha) * 40f;
        }

        CRenderSystem system = Client.RENDERER.getCrenderSystem();
        float prevAlpha = system.alpha();
        system.alpha(alpha);

        if (Interface.INSTANCE.hudStyle.is(Interface.HudStyle.Solution)) {
            NurickNotifyRenderer.render(this, renderX, renderY, alpha);
        } else {
            NotificationElement.Style currentStyle = Nottification.INSTANCE.notificationElement.style.get();
            renderNewStyle(renderX, renderY, alpha, currentStyle);
        }

        system.alpha(prevAlpha);
    }

    private void renderNewStyle(float lx, float ly, float alpha, NotificationElement.Style s) {
        String content = text.getString();
        boolean isModule = (content.contains("включен") || content.contains("выключен") || content.contains("enabled")) && !content.contains("§");

        float textW = Client.RENDERER.textWidth(content, TextureUse.SFMEDIUM, TEXT_SIZE) + 12;
        float totalW = HEIGHT + SPACING + textW;
        float startX = lx - totalW / 2f;

        drawStyleBox(startX, ly, HEIGHT, HEIGHT, alpha, s);
        if (icon != null) {
            Client.RENDERER.text(icon.glyph, startX + HEIGHT/2f - 4.3f, ly + HEIGHT/2f - 3f, TextureUse.ICONS, 6, new Color(255, 255, 255, (int)(255 * alpha)));
        } else {
            drawItemIcon(startX + 2f, ly + 2f, 10.5f, alpha);
        }

        float contentX = startX + HEIGHT + SPACING;
        drawStyleBox(contentX, ly, textW, HEIGHT, alpha, s);

        float textY = ly + HEIGHT/2f - TEXT_SIZE/2f - 0.3f;
        if (isModule) {
            drawSplitModuleText(content, contentX + 6, textY, alpha);
        } else {
            Client.RENDERER.text(text, contentX + 6, textY, TextureUse.SFMEDIUM, TEXT_SIZE);
        }
    }

    private void drawSplitModuleText(String str, float tx, float ty, float alpha) {
        String[] words = str.split(" ");
        float xOff = 0;
        int a = MathHelper.clamp((int)(255 * alpha), 0, 255);

        for (String word : words) {
            Color color = Color.WHITE;
            String clean = Formatting.strip(word).toLowerCase();

            if (clean.contains("включен") || clean.contains("enabled")) color = Color.GREEN;
            else if (clean.contains("выключен") || clean.contains("disabled")) color = Color.RED;
            else if (clean.length() > 3) color = ClientSettings.INSTANCE.getColor(0);

            String toDraw = word + " ";
            Client.RENDERER.text(toDraw, tx + xOff, ty, TextureUse.SFMEDIUM, TEXT_SIZE, new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
            xOff += Client.RENDERER.textWidth(toDraw, TextureUse.SFMEDIUM, TEXT_SIZE);
        }
    }

    private void drawStyleBox(float rx, float ry, float rw, float rh, float alpha, NotificationElement.Style s) {
        Vector4f round = new Vector4f(4.5f);
        Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);
        if (s == NotificationElement.Style.DARK) {
            Color bg = new Color(20, 20, 25, (int) (180 * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bg, bg, bg, bg);
            Color out = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new org.joml.Vector2f(1), out, out, out, out);
        } else {
            Color bg = new Color(62, 62, 71, 0);
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bg, bg, bg, bg);
            Color out = new Color(255, 255, 255, (int) (25 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new org.joml.Vector2f(1), out, out, out, out);
        }
    }

    private void drawItemIcon(float x, float y, float size, float alpha) {
        var dc = Client.RENDERER.getDrawContext();
        if (dc == null) return;
        ItemStack stack = null;
        if (itemStack != null) stack = itemStack;
        else if (item != null) stack = Registries.ITEM.get(Identifier.ofVanilla(item)).getDefaultStack();
        if (stack != null && !stack.isEmpty()) {
            final ItemStack fs = stack;
            Client.RENDERER.queueTask(() -> {
                var m = dc.getMatrices(); m.pushMatrix(); m.translate(x, y); m.scale(size / 16f, size / 16f); dc.drawItem(fs, 0, 0); m.popMatrix();
            });
        }
    }
}