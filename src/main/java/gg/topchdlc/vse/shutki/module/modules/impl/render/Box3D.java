package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

/**
 * Create by daun kvass
 */
public class Box3D extends Module {
    public static final Box3D INSTANCE = new Box3D();

    private static final Identifier GLOW_TEXTURE = Identifier.of("topchdlc", "images/world/bloom.png");

    private final MultiEnumSetting<Target> targets = multiEnumSetting("Отображать", Target.Players);
    private final CheckBox fill = checkbox("Заливка", true);
    private final CheckBox outline = checkbox("Обводка", true);
    private final CheckBox bloom = checkbox("Свечение (Bloom)", true);

    private final SliderSetting fillAlpha = sliderSetting("Прозрачность заливки", 50, 10, 255).increment(5f);
    private final SliderSetting bloomAlpha = sliderSetting("Сила свечения", 120, 10, 255).increment(5f);

    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate imm;

    public Box3D() {
        super("Box3D", Category.RENDER, "Рисует 3D боксы с обводкой и блумом вокруг сущностей");
    }

    @Override
    public void toggle() {
        super.toggle();
        if (!isEnabled() && allocator != null) {
            allocator.close();
            allocator = null;
            imm = null;
        }
    }

    public EventBus<Event> bus = event -> {
        if (event instanceof Event3D e) {
            onRender3D(e);
        }
    };

    private void onRender3D(Event3D event) {
        if (mc.world == null || mc.player == null) return;

        if (allocator == null) {
            allocator = new BufferAllocator(512 * 1024);
            imm = VertexConsumerProvider.immediate(allocator);
        }

        MatrixStack stack = event.stack;
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        for (Entity entity : mc.world.getEntities()) {
            if (shouldRender(entity)) {
                renderEntityBox(stack, entity, tickDelta, cam);
            }
        }

        imm.draw();
    }

    private void renderEntityBox(MatrixStack stack, Entity entity, float tickDelta, Vec3d cam) {
        double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - cam.x;
        double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - cam.y;
        double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - cam.z;

        Box bb = entity.getBoundingBox();
        float w = (float) (bb.maxX - bb.minX) / 2f;
        float h = (float) (bb.maxY - bb.minY);

        Color color = getEntityColor(entity);

        stack.push();
        stack.translate(x, y, z);
        Matrix4f matrix = stack.peek().getPositionMatrix();

        if (fill.get()) {
            VertexConsumer fillBuf = imm.getBuffer(ClientPipelines.FILL);
            int fAlpha = (int) fillAlpha.get();
            int col = ColorUtility.replAlpha(color.getRGB(), fAlpha);
            drawBoxFill(fillBuf, matrix, w, h, col);
        }

        if (bloom.get()) {
            int bAlpha = (int) bloomAlpha.get();
            int bloomCol = ColorUtility.replAlpha(color.getRGB(), bAlpha);

            VertexConsumer bloomTexBuf = imm.getBuffer(ClientPipelines.TARGET_ESP.apply(GLOW_TEXTURE));
            drawTexturedGlowBox(bloomTexBuf, matrix, w + 0.01f, h + 0.02f, bloomCol);

            VertexConsumer glowBuf = imm.getBuffer(ClientPipelines.GLOW);
            int outerGlowCol = ColorUtility.replAlpha(color.getRGB(), Math.min(bAlpha / 2, 255));
            drawBoxFill(glowBuf, matrix, w + 0.03f, h + 0.05f, outerGlowCol);
        }

        if (outline.get()) {
            VertexConsumer lineBuf = imm.getBuffer(ClientPipelines.OUTLINE_NO);
            drawBoxOutline(lineBuf, matrix, w, h, color.getRGB());
        }

        stack.pop();
    }


    private void drawBoxFill(VertexConsumer buf, Matrix4f mat, float w, float h, int col) {
        int r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF, a = (col >>> 24);

        vertex(buf, mat, -w, 0, -w, r, g, b, a);
        vertex(buf, mat, w, 0, -w, r, g, b, a);
        vertex(buf, mat, w, 0, w, r, g, b, a);
        vertex(buf, mat, -w, 0, w, r, g, b, a);

        vertex(buf, mat, -w, h, -w, r, g, b, a);
        vertex(buf, mat, -w, h, w, r, g, b, a);
        vertex(buf, mat, w, h, w, r, g, b, a);
        vertex(buf, mat, w, h, -w, r, g, b, a);

        vertex(buf, mat, -w, 0, -w, r, g, b, a);
        vertex(buf, mat, -w, h, -w, r, g, b, a);
        vertex(buf, mat, w, h, -w, r, g, b, a);
        vertex(buf, mat, w, 0, -w, r, g, b, a);

        vertex(buf, mat, -w, 0, w, r, g, b, a);
        vertex(buf, mat, w, 0, w, r, g, b, a);
        vertex(buf, mat, w, h, w, r, g, b, a);
        vertex(buf, mat, -w, h, w, r, g, b, a);

        vertex(buf, mat, -w, 0, -w, r, g, b, a);
        vertex(buf, mat, -w, 0, w, r, g, b, a);
        vertex(buf, mat, -w, h, w, r, g, b, a);
        vertex(buf, mat, -w, h, -w, r, g, b, a);

        vertex(buf, mat, w, 0, -w, r, g, b, a);
        vertex(buf, mat, w, h, -w, r, g, b, a);
        vertex(buf, mat, w, h, w, r, g, b, a);
        vertex(buf, mat, w, 0, w, r, g, b, a);
    }


    private void drawTexturedGlowBox(VertexConsumer buf, Matrix4f mat, float w, float h, int col) {
        int r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF, a = (col >>> 24);

        texVertex(buf, mat, -w, 0, -w, 0, 0, r, g, b, a);
        texVertex(buf, mat, w, 0, -w, 1, 0, r, g, b, a);
        texVertex(buf, mat, w, 0, w, 1, 1, r, g, b, a);
        texVertex(buf, mat, -w, 0, w, 0, 1, r, g, b, a);

        texVertex(buf, mat, -w, h, -w, 0, 0, r, g, b, a);
        texVertex(buf, mat, -w, h, w, 0, 1, r, g, b, a);
        texVertex(buf, mat, w, h, w, 1, 1, r, g, b, a);
        texVertex(buf, mat, w, h, -w, 1, 0, r, g, b, a);

        texVertex(buf, mat, -w, 0, -w, 0, 1, r, g, b, a);
        texVertex(buf, mat, -w, h, -w, 0, 0, r, g, b, a);
        texVertex(buf, mat, w, h, -w, 1, 0, r, g, b, a);
        texVertex(buf, mat, w, 0, -w, 1, 1, r, g, b, a);

        texVertex(buf, mat, -w, 0, w, 0, 1, r, g, b, a);
        texVertex(buf, mat, w, 0, w, 1, 1, r, g, b, a);
        texVertex(buf, mat, w, h, w, 1, 0, r, g, b, a);
        texVertex(buf, mat, -w, h, w, 0, 0, r, g, b, a);

        texVertex(buf, mat, -w, 0, -w, 0, 1, r, g, b, a);
        texVertex(buf, mat, -w, 0, w, 1, 1, r, g, b, a);
        texVertex(buf, mat, -w, h, w, 1, 0, r, g, b, a);
        texVertex(buf, mat, -w, h, -w, 0, 0, r, g, b, a);

        texVertex(buf, mat, w, 0, -w, 0, 1, r, g, b, a);
        texVertex(buf, mat, w, h, -w, 0, 0, r, g, b, a);
        texVertex(buf, mat, w, h, w, 1, 0, r, g, b, a);
        texVertex(buf, mat, w, 0, w, 1, 1, r, g, b, a);
    }


    private void drawBoxOutline(VertexConsumer buf, Matrix4f mat, float w, float h, int col) {
        int r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF, a = 255;

        vertex(buf, mat, -w, 0, -w, r, g, b, a);
        vertex(buf, mat, w, 0, -w, r, g, b, a);
        vertex(buf, mat, w, 0, w, r, g, b, a);
        vertex(buf, mat, -w, 0, w, r, g, b, a);
        vertex(buf, mat, -w, 0, -w, r, g, b, a);

        vertex(buf, mat, -w, h, -w, r, g, b, a);
        vertex(buf, mat, w, h, -w, r, g, b, a);
        vertex(buf, mat, w, h, w, r, g, b, a);
        vertex(buf, mat, -w, h, w, r, g, b, a);
        vertex(buf, mat, -w, h, -w, r, g, b, a);

        vertex(buf, mat, w, 0, -w, r, g, b, a);
        vertex(buf, mat, w, h, -w, r, g, b, a);

        vertex(buf, mat, w, 0, w, r, g, b, a);
        vertex(buf, mat, w, h, w, r, g, b, a);

        vertex(buf, mat, -w, 0, w, r, g, b, a);
        vertex(buf, mat, -w, h, w, r, g, b, a);
    }

    private void vertex(VertexConsumer buf, Matrix4f mat, float x, float y, float z, int r, int g, int b, int a) {
        buf.vertex(mat, x, y, z).color(r, g, b, a);
    }

    private void texVertex(VertexConsumer buf, Matrix4f mat, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        buf.vertex(mat, x, y, z).texture(u, v).color(r, g, b, a);
    }

    private Color getEntityColor(Entity entity) {
        if (entity instanceof PlayerEntity p && Client.FRIENDS.isFriend(p.getName().getString())) {
            return new Color(100, 255, 100);
        }
        if (entity instanceof ItemEntity) return new Color(255, 255, 255);
        return new Color(ClientSettings.INSTANCE.getColor(0).getRGB());
    }

    private boolean shouldRender(Entity entity) {
        if (entity == mc.player) {
            return mc.options.getPerspective() != Perspective.FIRST_PERSON && targets.get(Target.Self);
        }
        if (entity instanceof PlayerEntity p) {
            if (Client.FRIENDS.isFriend(p.getName().getString())) return targets.get(Target.Friends);
            return targets.get(Target.Players);
        }
        return entity instanceof ItemEntity && targets.get(Target.Items);
    }

    private enum Target {
        Players, Friends, Items, Self
    }
}