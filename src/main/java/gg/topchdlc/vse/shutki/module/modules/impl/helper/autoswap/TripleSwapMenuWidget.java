package gg.topchdlc.vse.shutki.module.modules.impl.helper.autoswap;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.AutoSwap;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.client.client.ClientColors;

import java.awt.Color;
import java.util.Arrays;

public class TripleSwapMenuWidget extends UIWidget {

    static final int SLOTS = 3;
    private static final float OUTER_R   = 90f;
    private static final float INNER_R   = 60f;
    private static final float ITEM_R    = (INNER_R + OUTER_R) / 2f;
    private static final float ITEM_SIZE = 18f;
    private static final float[] MID_DEG = { -90f, 150f, 30f };
    private static final float SPREAD    = 58f;

    private float cx, cy;
    private final float[] hoverAnim = new float[SLOTS];
    private long lastTime = 0;

    @Override
    public void opened() {
        cx = mc.getWindow().getScaledWidth() / 2f;
        cy = mc.getWindow().getScaledHeight() / 2f;
        float s = OUTER_R + 15f;
        bound(cx - s, cy - s, s * 2, s * 2);
        animation.reset();
        animation.setDirection(Direction.BACKWARDS);

        Arrays.fill(hoverAnim, 0f);
        lastTime = System.currentTimeMillis();
    }

    private float midRad(int i) { return (float) Math.toRadians(MID_DEG[i]); }

    private boolean inSector(float mx, float my, int i) {
        float dx = mx - cx, dy = my - cy;
        float dist = (float)Math.sqrt(dx*dx + dy*dy);
        if (dist < INNER_R || dist > OUTER_R) return false;

        float angle = (float)Math.toDegrees(Math.atan2(dy, dx));
        float d = ((angle - MID_DEG[i]) % 360 + 360) % 360;
        if (d > 180) d -= 360;

        return Math.abs(d) <= SPREAD;
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;
        float alpha = 1f - animation.getOutput();
        if (alpha <= 0.01f) return;

        float prev = Client.RENDERER.getCrenderSystem().alpha();
        Client.RENDERER.getCrenderSystem().alpha(prev * alpha);
        renderMenu(mouseX, mouseY);
        Client.RENDERER.getCrenderSystem().alpha(prev);
    }

    private void renderMenu(int mouseX, int mouseY) {
        AutoSwap as = AutoSwap.INSTANCE;
        DrawContext ctx = Client.RENDERER.getDrawContext();

        long now = System.currentTimeMillis();
        float delta = (now - lastTime) / 1000f;
        lastTime = now;

        for (int s = 0; s < SLOTS; s++) {
            boolean hovered = inSector(mouseX, mouseY, s);

            float target = hovered ? 1f : 0f;
            hoverAnim[s] += (target - hoverAnim[s]) * 15f * delta;
            hoverAnim[s] = Math.max(0f, Math.min(1f, hoverAnim[s]));
            float anim = hoverAnim[s];

            float shiftDist = anim * 8f;
            float midRadian = midRad(s);
            float scx = cx + (float) Math.cos(midRadian) * shiftDist;
            float scy = cy + (float) Math.sin(midRadian) * shiftDist;
            Color fillBase = new Color(255, 255, 255, 30);
            Color fillHover = new Color(80, 140, 255, 120);
            Color currentFill = lerpColor(fillBase, fillHover, anim);

            Color outBase = new Color(255, 255, 255, 130);
            Color outHover = new Color(255, 255, 255, 255);
            Color currentOut = lerpColor(outBase, outHover, anim);

            drawSegmentFill(scx, scy, INNER_R, OUTER_R, MID_DEG[s], SPREAD, currentFill);
            drawSegmentOutline(scx, scy, INNER_R, OUTER_R, MID_DEG[s], SPREAD, currentOut);

            float iconX = scx + (float)Math.cos(midRadian) * ITEM_R;
            float iconY = scy + (float)Math.sin(midRadian) * ITEM_R;

            ItemStack stack = as.getTripleSlot(s);
            if (stack == null || stack.isEmpty()) {
                Client.RENDERER.textCentered("+", iconX, iconY - 5f,
                        TextureUse.SFMEDIUM, 14f, new Color(255, 255, 255, 180));
            } else if (ctx != null) {
                final float fx = iconX - ITEM_SIZE/2f;
                final float fy = iconY - ITEM_SIZE/2f;
                final ItemStack fs = stack;
                Client.RENDERER.queueTask(() -> {
                    if (mc.player == null) return;
                    var m = ctx.getMatrices();
                    m.pushMatrix();
                    float scale = ITEM_SIZE / 16f;
                    m.translate(fx, fy);
                    m.scale(scale, scale);
                    ctx.drawItem(fs, 0, 0);
                    ctx.drawStackOverlay(mc.textRenderer, fs, 0, 0);
                    m.popMatrix();
                });
            }

            float textX = scx + (float)Math.cos(midRadian) * (OUTER_R + 12f);
            float textY = scy + (float)Math.sin(midRadian) * (OUTER_R + 12f);
            Client.RENDERER.textCentered("Слот " + (s+1), textX, textY - 3f,
                    TextureUse.SFMEDIUM, 5f, ClientColors.SECONDARY_FORE_COLOR);
        }
    }

    private void drawSegmentFill(float scx, float scy, float inner, float outer, float midAng, float spread, Color color) {
        int startY = (int) Math.floor(-outer);
        int endY = (int) Math.ceil(outer);
        inner += 0.5f;
        outer -= 0.5f;

        for (int dy = startY; dy <= endY; dy++) {
            float[] pts = new float[6];
            int count = 0;

            if (outer * outer >= dy * dy) {
                float x = (float) Math.sqrt(outer * outer - dy * dy);
                pts[count++] = -x; pts[count++] = x;
            }
            if (inner * inner >= dy * dy) {
                float x = (float) Math.sqrt(inner * inner - dy * dy);
                pts[count++] = -x; pts[count++] = x;
            }

            float[] angles = { midAng - spread, midAng + spread };
            for (float ang : angles) {
                float rad = (float) Math.toRadians(ang);
                float s = (float) Math.sin(rad);
                float c = (float) Math.cos(rad);

                if (Math.abs(s) > 0.001f) {
                    float x = (c * dy) / s;
                    if ((x * c + dy * s) > 0 && (x * x + dy * dy) <= outer * outer + 1f) {
                        pts[count++] = x;
                    }
                } else if (Math.abs(dy) <= 0.5f) {
                    pts[count++] = c > 0 ? outer : -outer;
                }
            }

            Arrays.sort(pts, 0, count);

            for (int i = 0; i < count - 1; i++) {
                float x1 = pts[i];
                float x2 = pts[i + 1];
                if (x2 - x1 < 0.5f) continue;

                float midX = (x1 + x2) / 2f;
                float d2 = midX * midX + dy * dy;

                if (d2 >= inner * inner - 0.5f && d2 <= outer * outer + 0.5f) {
                    float angle = (float) Math.toDegrees(Math.atan2(dy, midX));
                    float d = ((angle - midAng) % 360 + 360) % 360;
                    if (d > 180) d -= 360;

                    if (Math.abs(d) <= spread + 0.2f) {
                        Client.RENDERER.rect(scx + x1, scy + dy, x2 - x1, 1.0f, new Vector4f(0), 0f, color, color, color, color);
                    }
                }
            }
        }
    }

    private void drawSolidLine(float x1, float y1, float x2, float y2, float thick, Color color) {
        Client.RENDERER.line(x1, y1, x2, y2, thick, 1.0f, thick, color, color);
        Client.RENDERER.rect(x2 - thick / 2f, y2 - thick / 2f, thick, thick, new Vector4f(thick), 1f, color, color, color, color);
    }

    private void drawSegmentOutline(float scx, float scy, float inner, float outer, float midAng, float spread, Color color) {
        float startAng = midAng - spread;
        float endAng = midAng + spread;
        float thickness = 1.8f;
        int arcSteps = 30;
        for (int i = 0; i < arcSteps; i++) {
            float a1 = (float) Math.toRadians(startAng + (spread * 2f * i / arcSteps));
            float a2 = (float) Math.toRadians(startAng + (spread * 2f * (i + 1) / arcSteps));
            drawSolidLine(
                    scx + (float)Math.cos(a1)*outer, scy + (float)Math.sin(a1)*outer,
                    scx + (float)Math.cos(a2)*outer, scy + (float)Math.sin(a2)*outer,
                    thickness, color
            );
        }
        for (int i = 0; i < arcSteps; i++) {
            float a1 = (float) Math.toRadians(startAng + (spread * 2f * i / arcSteps));
            float a2 = (float) Math.toRadians(startAng + (spread * 2f * (i + 1) / arcSteps));
            drawSolidLine(
                    scx + (float)Math.cos(a1)*inner, scy + (float)Math.sin(a1)*inner,
                    scx + (float)Math.cos(a2)*inner, scy + (float)Math.sin(a2)*inner,
                    thickness, color
            );
        }
        float rStart = (float) Math.toRadians(startAng);
        drawSolidLine(
                scx + (float)Math.cos(rStart)*inner, scy + (float)Math.sin(rStart)*inner,
                scx + (float)Math.cos(rStart)*outer, scy + (float)Math.sin(rStart)*outer,
                thickness, color
        );
        float rEnd = (float) Math.toRadians(endAng);
        drawSolidLine(
                scx + (float)Math.cos(rEnd)*inner, scy + (float)Math.sin(rEnd)*inner,
                scx + (float)Math.cos(rEnd)*outer, scy + (float)Math.sin(rEnd)*outer,
                thickness, color
        );
    }

    private Color lerpColor(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (mc.player == null) return false;
        AutoSwap as = AutoSwap.INSTANCE;
        for (int s = 0; s < SLOTS; s++) {
            if (!inSector(mouseX, mouseY, s)) continue;
            ItemStack stack = as.getTripleSlot(s);

            if (button == 0) {
                if (stack == null || stack.isEmpty()) {
                    as.openInventoryForSlot(s);
                } else {
                    int invSlot = findItemSlot(stack);
                    if (invSlot >= 0) {
                        int swapSlot = invSlot < 9 ? invSlot + 36 : invSlot;
                        as.performTripleSwap(swapSlot);
                    }
                    as.closeMenu();
                }
                return true;
            } else if (button == 1) {
                as.setTripleSlot(s, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    private int findItemSlot(ItemStack target) {
        if (mc.player == null || target == null || target.isEmpty()) return -1;

        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);

            if (s.isEmpty() || s.getItem() != target.getItem()) continue;
            if (!s.getName().getString().equals(target.getName().getString())) continue;
            if (s.hasGlint() != target.hasGlint()) continue;

            return i;
        }
        return -1;
    }


}