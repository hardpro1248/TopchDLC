package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.macros.Macro;
import gg.topchdlc.api.macros.constructor.MacroBlock;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.vse.utils.other.LogUtility;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Rectangle;
import lombok.Getter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class MacrosWidget extends UIWidget {
    Drag drag = new Drag();

    ArrayList<BlockSample> blocks = new ArrayList<>();

    ArrayList<BlockSample> samples = new ArrayList<>();
    public BlockSample currentSample = null;
    int insertingAt = -1;

    float scroll, scrollAnim;

    Rectangle submit = new Rectangle();

    public SmoothStepAnimation animation = new SmoothStepAnimation(300, 1);

    public MacrosWidget() {
        for (MacroBlock macroBlock : Client.MACROS.getBlocks().getHandled()) {
            samples.add(new BlockSample(macroBlock, this, false));
        }
    }

    @Override
    public void render(int mouseX, int mouseY) {
        if (drag.dragging) {
            x = mouseX - drag.dX;
            y = mouseY - drag.dY;
        }
        CRenderSystem system = Client.RENDERER.getCrenderSystem();

        MatrixStack stack = Client.RENDERER.getStack();

        float alpha = system.alpha();

        float anim = 1F - animation.getOutput();

        system.alpha(anim * alpha);

        stack.push();
        MathUtility.scale(stack, x + width / 2F, y + height / 2F, 0.8F + anim * 0.2F);

        Client.RENDERER.outlined(x, y, width, height);
        Client.RENDERER.text("Macros constructor", x + 8, y + 8, TextureUse.SFMEDIUM, 9, ClientColors.FORE_COLOR);


        system.push(x, y + 30, width, height - 60);

        {
            float offy = 30;
            for (BlockSample object : samples) {
                object.bound(x + width / 2F + 4, y + offy, width / 2F - 8, 15)
                        .render(mouseX, mouseY);
                offy += object.getHeight() + 2;
            }
        }

        {
            int i = 0;

            float offy = 30;

            float lastY = -1;

            for (BlockSample object : blocks) {
                if (object == currentSample) {
                    continue;
                }

                object.bound(x + 4, y + offy + scroll, width / 2F - 8, 15);

                if (currentSample != null && !drag.dragging && MathUtility.delta(mouseY, object.getY() + 16) < 10) {
                    offy += 27;

                    insertingAt = i;
                }

                object.render(mouseX, mouseY);

                offy += object.getHeight() + 12;

                if (object != blocks.getLast()) {
                    Client.RENDERER.rect(x + width / 4F - 0.5F, y + offy - 10 + scrollAnim, 1, 8, new Vector4f(0), 0,
                            ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
                }

                lastY = object.lerpedY;

                i++;
            }


            if (currentSample != null && mouseX < x + width / 2F) {
                Client.RENDERER.outline(x + 4, y + 30 + 27 * (insertingAt + 1) + scrollAnim, width / 2F - 8, 15, 1, new Vector4f(4), new Vector2f(1),
                        ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
            }
        }
        system.pop();

        {
            submit.bound(x + 6, y + height - 26, 100, 22);
            Client.RENDERER.rect(submit, new Vector4f(4), 1, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
            Client.RENDERER.textCentered("Submit",
                    submit.getX() + submit.getWidth() / 2F - 1,
                    submit.getY() + submit.getHeight() / 2F - Client.RENDERER.textHeight(TextureUse.SFMEDIUM, 8) / 2F, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
        }

        if (currentSample != null) {
            currentSample.bound(mouseX - currentSample.dX, mouseY - currentSample.dY, currentSample.getWidth(), currentSample.getHeight())
                    .render(mouseX, mouseY);
        } else {
            insertingAt = -1;
        }

        stack.pop();
        system.alpha(alpha);

        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 10);

        if (anim < 0.1 && animation.getDirection() == Direction.FORWARDS) {
            shouldRemove = true;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hover((int) mouseX, (int) mouseY)) {
            scroll += (float) (verticalAmount * 15);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (submit.hovered(mouseX, mouseY)) {
            Macro macro = Client.MACROS.addFromBuilder(blocks);
            macro.setKey(GLFW.GLFW_KEY_U);

            LogUtility.debug("created macros from " + blocks.size() + " insn");
            blocks.clear();
        }

        if (MathUtility.mouseIn(x, y + 30, width, height - 60, mouseX, mouseY)) {
            for (BlockSample object : samples) {
                if (object.click(mouseX, mouseY, button)) {
                    return true;
                }
            }
            for (BlockSample object : blocks) {
                if (object.click(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        if (hover(mouseX, mouseY)) {
            drag.dragging = true;
            drag.dX = mouseX - x;
            drag.dY = mouseY - y;
            return true;
        } else {
            animation.setDirection(Direction.FORWARDS);
            return true;
        }
    }

    @Override
    public void release(int button) {

        if (currentSample != null) {
            if (!currentSample.isAdded && currentSample.getX() < x + width / 2F) {
                BlockSample sample = new BlockSample(currentSample.original, this, true);
                if (insertingAt != -1) {
                    blocks.add(insertingAt + 1, sample);
                } else
                    blocks.add(sample);
            } else if (currentSample.isAdded && currentSample.getX() > x + width / 2F) {
                blocks.remove(currentSample);
            }
            if (currentSample.isAdded && currentSample.getX() < x + width / 2F && insertingAt != -1) {
                blocks.remove(currentSample);
                blocks.add(insertingAt + 1, currentSample);
            }
        }

        currentSample = null;

        for (BlockSample object : samples) {
            object.release(button);
        }
        for (BlockSample object : blocks) {
            object.release(button);
        }
        drag.dragging = false;
        super.release(button);
    }

    public static class BlockSample extends RendererObject {
        @Getter
        private MacroBlock original;
        private MacrosWidget parent;
        public float dX, dY;
        public float lerpedX = -999, lerpedY = -999;
        public boolean isAdded = false;
        public BlockSample(MacroBlock original, MacrosWidget parent, boolean isAdded) {
            this.original = original;
            this.parent = parent;
            this.isAdded = isAdded;
        }

        @Override
        public void render(int mouseX, int mouseY) {
            float dx = x - parent.x;
            float dy = y - parent.y;
            if (isAdded && (lerpedX != -999 && lerpedY != -999)) {
                lerpedX = MathUtility.linearFps(lerpedX, dx, 10);
                lerpedY = MathUtility.linearFps(lerpedY, dy, 10);
            } else {
                lerpedX = x - parent.x;
                lerpedY = y - parent.y;
            }
            Client.RENDERER.rect(parent.x + lerpedX, parent.y + lerpedY, width, height, new Vector4f(0), 0, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
            Client.RENDERER.text(original.renderName(), parent.x + lerpedX + 4, parent.y + lerpedY + height / 2F - Client.RENDERER.textHeight(TextureUse.SFMEDIUM, 8) / 2F, TextureUse.SFMEDIUM, 8, ClientColors.FORE_COLOR);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int button) {
            if (!hover(mouseX, mouseY)) {
                return false;
            }
            dX = mouseX - x;
            dY = mouseY - y;

            parent.currentSample = this;
            return true;
        }

        @Override
        public void release(int button) {
            super.release(button);
        }
    }
}
