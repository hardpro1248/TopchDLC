package gg.topchdlc.vse.shutki.module.modules.impl.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.blockcolorlist.BlockColorListSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.block.BlockLocationTracker;
import gg.topchdlc.vse.utils.block.ChunkScanner;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BlockESP extends Module {
    public static final BlockESP INSTANCE = new BlockESP();

    private ChunkScanner getChunkScanner() {
        return Client.ChunkScanner;
    }

    private BlockESP() {
        super("BlockESP", Category.RENDER, "Подсвечивает блоки из списка");
    }

    public final SliderSetting radius = add(new SliderSetting("Radius", 16, 4, 64).increment(1));
    public final BlockColorListSetting blocks = add(new BlockColorListSetting("Blocks"));

    private static final float EXPAND = 0.003f;
    private final BufferAllocator allocator = new BufferAllocator(1 << 20);

    private final Tracker tracker = new Tracker();
    private volatile List<RenderEntry> renderList = Collections.emptyList();
    private int lastBlocksHash = -1;

    private class Tracker extends BlockLocationTracker.BlockPos2State<Color> {
        @Override
        public @Nullable Color getStateFor(BlockPos pos, BlockState state) {
            if (state.isAir()) return null;
            Map<Identifier, Color> blockMap = blocks.getBlocks();
            if (blockMap.isEmpty()) return null;
            Identifier id = net.minecraft.registry.Registries.BLOCK.getId(state.getBlock());
            return blockMap.get(id);
        }
    }

    @Override
    protected void onEnable() {
        renderList = Collections.emptyList();
        lastBlocksHash = blocks.getBlocks().hashCode();

        ChunkScanner scanner = getChunkScanner();
        if (scanner != null) {
            scanner.subscribe(tracker);
        }
    }

    @Override
    protected void onDisable() {
        ChunkScanner scanner = getChunkScanner();
        if (scanner != null) {
            scanner.unsubscribe(tracker);
        }
        tracker.clearAllChunks();
        renderList = Collections.emptyList();
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) onTick();
        else if (event instanceof Event3D e) render(e);
    };

    private void onTick() {
        if (mc.world == null || mc.player == null) return;

        int curHash = blocks.getBlocks().hashCode();
        if (curHash != lastBlocksHash) {
            lastBlocksHash = curHash;
            ChunkScanner scanner = getChunkScanner();
            if (scanner != null) {
                scanner.unsubscribe(tracker);
                tracker.clearAllChunks();
                scanner.subscribe(tracker);
            }
        }

        if (tracker.isEmpty()) {
            renderList = Collections.emptyList();
            return;
        }

        rebuildRenderList();
    }

    private void rebuildRenderList() {
        BlockPos playerPos = mc.player.getBlockPos();
        int r = (int) radius.get();
        int rSq = r * r;

        List<RenderEntry> newList = new ArrayList<>();

        tracker.stream()
                .filter(entry -> {
                    BlockPos pos = entry.getKey();
                    int dx = pos.getX() - playerPos.getX();
                    int dy = pos.getY() - playerPos.getY();
                    int dz = pos.getZ() - playerPos.getZ();
                    return dx * dx + dy * dy + dz * dz <= rSq;
                })
                .forEach(entry -> {
                    Box box = getBlockBox(entry.getKey());
                    if (box != null) {
                        newList.add(new RenderEntry(box, entry.getValue()));
                    }
                });

        renderList = newList;
    }

    private void render(Event3D event) {
        List<RenderEntry> list = renderList;
        if (list.isEmpty()) return;
        if (mc.world == null || mc.player == null) return;

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        MatrixStack stack = event.stack;

        stack.push();
        stack.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(allocator);

        for (RenderEntry entry : list) {
            Box b = entry.box;
            Color c = entry.color;

            Color fill = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                    Math.max(1, Math.min(40, c.getAlpha() / 6)));
            Color outline = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                    Math.max(180, c.getAlpha()));

            drawFilledBox(stack, imm,
                    b.minX - EXPAND, b.minY - EXPAND, b.minZ - EXPAND,
                    b.maxX + EXPAND, b.maxY + EXPAND, b.maxZ + EXPAND, fill);
            drawOutlineBox(stack, imm,
                    b.minX - EXPAND, b.minY - EXPAND, b.minZ - EXPAND,
                    b.maxX + EXPAND, b.maxY + EXPAND, b.maxZ + EXPAND, outline);
        }

        imm.draw();
        stack.pop();
    }

    private @Nullable Box getBlockBox(BlockPos pos) {
        if (mc.world == null) return null;
        try {
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) return null;
            var shape = state.getOutlineShape(mc.world, pos);
            if (shape.isEmpty()) return new Box(pos);
            return shape.getBoundingBox().offset(pos);
        } catch (Exception e) {
            return new Box(pos);
        }
    }
    private void drawFilledBox(MatrixStack stack, VertexConsumerProvider.Immediate imm,
                               double x1, double y1, double z1,
                               double x2, double y2, double z2, Color c) {
        VertexConsumer buf = imm.getBuffer(ClientPipelines.QUAD);
        Matrix4f mat = stack.peek().getPositionMatrix();
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        float fx1 = (float) x1, fy1 = (float) y1, fz1 = (float) z1;
        float fx2 = (float) x2, fy2 = (float) y2, fz2 = (float) z2;
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b, a);
    }

    private void drawOutlineBox(MatrixStack stack, VertexConsumerProvider.Immediate imm,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2, Color c) {
        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE_NO);
        Matrix4f mat = stack.peek().getPositionMatrix();
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        float fx1 = (float) x1, fy1 = (float) y1, fz1 = (float) z1;
        float fx2 = (float) x2, fy2 = (float) y2, fz2 = (float) z2;
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b, a);
    }

    private record RenderEntry(Box box, Color color) {}
}