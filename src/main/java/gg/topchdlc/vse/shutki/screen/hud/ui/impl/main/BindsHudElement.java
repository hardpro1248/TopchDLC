package gg.topchdlc.vse.shutki.screen.hud.ui.impl.main;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.AssistModule;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.DecelerateAnimation;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BindsHudElement extends HudElement {
    private final EnumSetting<Style> style = settings.enumSetting("Стиль", Style.GLASS);

    private static final float HEIGHT      = 14.5f;
    private static final float PADDING_SIDE = 4.0f;
    private static final float SPACING     = 0.5f;
    private static final float ENTRY_GAP   = 2.0f;
    private static final float TEXT_SIZE   = 7.0f;
    private static final float COUNT_SIZE  = 5.5f;

    private final Map<Item, DecelerateAnimation> pressAnims = new HashMap<>();

    public BindsHudElement(Drag drag) {
        super("BindsHud", drag);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (mc.player == null) return;

        List<Object[]> entries = AssistModule.INSTANCE.getBindEntries();
        List<Object[]> active = new ArrayList<>();

        for (Object[] e : entries) {
            ItemStack item = ItemStack.EMPTY;
            int code = (int) e[1];
            if (code != -1 && !mc.player.getItemCooldownManager().isCoolingDown(item)) {
                active.add(e);
            }
        }

        boolean shown = mc.currentScreen instanceof ChatScreen || !active.isEmpty();
        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);

        float animAlpha = MathHelper.clamp(1f - (float)animation.getOutput(), 0f, 1f);
        if (animAlpha < 0.01f) return;

        float totalWidth = 0;
        List<Float> bindWidths = new ArrayList<>();
        for (Object[] entry : active) {
            String keyName = TextUtility.keyToString((int) entry[1]);
            float kw = Client.RENDERER.textWidth(keyName, TextureUse.SFMEDIUM, TEXT_SIZE) + (PADDING_SIDE * 2);
            bindWidths.add(kw);
            totalWidth += HEIGHT + SPACING + kw + ENTRY_GAP;
        }
        if (!active.isEmpty()) totalWidth -= ENTRY_GAP;

        float currentX = x;

        for (int i = 0; i < active.size(); i++) {
            Object[] entry = active.get(i);
            Item item = (Item) entry[0];
            int keyCode = (int) entry[1];
            float kw = bindWidths.get(i);
            DecelerateAnimation pAnim = pressAnims.computeIfAbsent(item, k -> new DecelerateAnimation(150, 1.0));
            boolean isPressed = false;
            long handle = mc.getWindow().getHandle();

            if (keyCode >= 0) {
                if (keyCode < 8) {
                    isPressed = GLFW.glfwGetMouseButton(handle, keyCode) == GLFW.GLFW_PRESS;
                } else {
                    isPressed = InputUtil.isKeyPressed(window, keyCode);
                }
            }

            pAnim.setDirection(isPressed ? Direction.BACKWARDS : Direction.FORWARDS);
            float jumpOffset = (float) (pAnim.getOutput() * 3.0f);

            float renderY = y - jumpOffset;

            drawStyle(currentX, renderY, HEIGHT, HEIGHT, animAlpha);

            int count = getCount(item);
            renderItemIcon(item.getDefaultStack(), count, currentX + 1.5f, renderY + 1.5f, 11.5f, animAlpha);

            float bindX = currentX + HEIGHT + SPACING;
            drawStyle(bindX + 0.5f, renderY, kw, HEIGHT, animAlpha);

            String keyStr = TextUtility.keyToString(keyCode);
            float tx = bindX + (kw / 2f) - (Client.RENDERER.textWidth(keyStr, TextureUse.SFMEDIUM, TEXT_SIZE) / 2f);
            Color bindColor = new Color(255, 255, 255, (int) (255 * animAlpha));
            Client.RENDERER.text(keyStr, tx, renderY + (HEIGHT / 2f - TEXT_SIZE / 2f) - 0.3f, TextureUse.SFMEDIUM, TEXT_SIZE, bindColor);

            currentX += (HEIGHT + SPACING + kw + ENTRY_GAP);
        }

        this.drag.width = width = MathUtility.linearFps(width, totalWidth, 10);
        this.drag.height = height = HEIGHT;
    }

    private void drawStyle(float rx, float ry, float rw, float rh, float alpha) {
        Vector4f round = new Vector4f(4.5f);
        Client.RENDERER.blur(rx, ry, rw, rh, round, 15f, alpha);

        if (style.get() == Style.DARK) {
            Color bgColor = new Color(20, 20, 25, (int) (180 * alpha));
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);
            Color outColor = new Color(255, 255, 255, (int) (15 * alpha));
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        } else {
            Color bgColor = new Color(62, 62, 71, 0);
            Client.RENDERER.rect(rx, ry, rw, rh, round, 1, bgColor, bgColor, bgColor, bgColor);
            int outAlpha = MathHelper.clamp((int) (25 * alpha), 0, 255);
            Color outColor = new Color(255, 255, 255, outAlpha);
            Client.RENDERER.outline(rx, ry, rw, rh, 0.4f, round, new Vector2f(1), outColor, outColor, outColor, outColor);
        }
    }

    private void renderItemIcon(ItemStack stack, int count, float ix, float iy, float size, float alpha) {
        if (stack == null || stack.isEmpty()) return;

        Client.RENDERER.queueTask(() -> {
            DrawContext context = Client.RENDERER.getDrawContext();
            if (context != null) {
                var matrices = context.getMatrices();
                matrices.pushMatrix();
                matrices.translate(ix, iy);
                float scale = size / 16f;
                matrices.scale(scale, scale);
                context.drawItem(stack, 0, 0);
                matrices.popMatrix();

                if (count >= 0) {
                    String cStr = String.valueOf(count);
                    float cw = Client.RENDERER.textWidth(cStr, TextureUse.SFMEDIUM, COUNT_SIZE);

                    float tx = ix + size - cw;
                    float ty = iy + size - COUNT_SIZE;

                    Color cCol = count == 0 ?
                            new Color(255, 80, 80, (int)(255 * alpha)) :
                            new Color(255, 255, 255, (int)(200 * alpha));

                    Client.RENDERER.getStack().push();
                    Client.RENDERER.getStack().peek().getPositionMatrix().identity();
                    Client.RENDERER.getCrenderSystem().layer(CRenderSystem.RenderLayer.OVERLAY);
                    Client.RENDERER.getCrenderSystem().z(1.0f);
                    Client.RENDERER.text(cStr, tx, ty, TextureUse.SFMEDIUM, COUNT_SIZE, cCol);
                    Client.RENDERER.getCrenderSystem().z(0f);
                    Client.RENDERER.getStack().pop();
                }
            }
        });
    }

    private int getCount(Item item) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) count += stack.getCount();
        }
        if (mc.player.getOffHandStack().getItem() == item) count += mc.player.getOffHandStack().getCount();
        return count;
    }

    @AllArgsConstructor
    enum Style implements EnumChoice {
        DARK("Темный"), GLASS("Стекло");
        @Getter final String renderName;
    }
}