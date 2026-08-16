package gg.topchdlc.vse.shutki.module.modules.impl.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.block.BlockLocationTracker;
import gg.topchdlc.vse.utils.block.ChunkScanner;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WardenHelper extends Module {
    public static final WardenHelper INSTANCE = new WardenHelper();

    public final SliderSetting textScale = sliderSetting("Размер текста", 6.5f, 4f, 12f).increment(0.5f);
    public final SliderSetting dotSize = sliderSetting("Размер точек GPS", 0.35f, 0.1f, 0.8f).increment(0.05f);
    public final SliderSetting dotSpacing = sliderSetting("Интервал точек", 0.5f, 0.2f, 1.0f).increment(0.1f);
    public final CheckBox renderESP = checkbox("ESP на сундуки", true);
    public final CheckBox themeColor = checkbox("Цвет от темы", false);

    private static final Identifier GLOW_TEXTURE = Identifier.of("topchdlc", "images/world/bloom.png");
    private final BufferAllocator allocator = new BufferAllocator(1 << 20);
    private final Tracker tracker = new Tracker();

    private List<WardenChest> parsedChests = new ArrayList<>();
    private WardenChest currentTargetChest = null;

    private static final BlockPos[] PREDEFINED_CHESTS = {
            new BlockPos(-2052, -55, -1961),
            new BlockPos(-2045, -49, -1961),
            new BlockPos(-2006, -54, -1954),
            new BlockPos(-2025, -55, -1952),
            new BlockPos(-2001, -55, -2005),
            new BlockPos(-2033, -55, -2009),
            new BlockPos(-2036, -55, -2015),
            new BlockPos(-2033, -55, -1994),
            new BlockPos(-2036, -55, -1988),
            new BlockPos(-1987, -55, -2007),
            new BlockPos(-2005, -54, -2027),
            new BlockPos(-2011, -48, -2042),
            new BlockPos(-2005, -55, -2056),
            new BlockPos(-1977, -55, -2056),
            new BlockPos(-1944, -55, -2045),
            new BlockPos(-1960, -55, -2030),
            new BlockPos(-1938, -48, -2034),
            new BlockPos(-1965, -55, -2008),
            new BlockPos(-1955, -55, -1995),
            new BlockPos(-1952, -48, -1982),
            new BlockPos(-1961, -48, -1974),
            new BlockPos(-1951, -49, -1948),
            new BlockPos(-1936, -55, -1948),
            new BlockPos(-1972, -54, -1944),
            new BlockPos(-1970, -55, -1959),
            new BlockPos(-1989, -55, -1982),
            new BlockPos(-2008, -55, -1963),
            new BlockPos(-1979, -56, -1979),
            new BlockPos(-2045, -55, -2045),
            new BlockPos(-2052, -49, -2042),
            new BlockPos(-2053, -55, -2042)
    };

    private WardenHelper() {
        super("WardenHelper", Category.RENDER, "xxx");
    }

    private static class WardenChest {
        BlockPos pos;
        boolean hasTimer;
        String timerText = "";
        int secondsRemaining = 999999;
        double distanceToPlayer;
    }

    private class Tracker extends BlockLocationTracker.BlockPos2State<Color> {
        @Override
        public @Nullable Color getStateFor(BlockPos pos, BlockState state) {
            if (!isInWardenArea(pos)) return null;
            if (state.isOf(net.minecraft.block.Blocks.CHEST) || state.isOf(net.minecraft.block.Blocks.TRAPPED_CHEST)) {
                return new Color(0, 255, 200);
            }
            return null;
        }
    }

    private boolean isInWardenArea(BlockPos pos) {
        return pos.getX() >= 1943 && pos.getX() <= 2070
                && pos.getZ() >= 1936 && pos.getZ() <= 2063
                && pos.getY() >= -58 && pos.getY() <= -40;
    }

    @Override
    protected void onEnable() {
        parsedChests.clear();
        currentTargetChest = null;
        ChunkScanner scanner = Client.ChunkScanner;
        if (scanner != null) {
            scanner.subscribe(tracker);
        }
    }

    @Override
    protected void onDisable() {
        ChunkScanner scanner = Client.ChunkScanner;
        if (scanner != null) {
            scanner.unsubscribe(tracker);
        }
        tracker.clearAllChunks();
        parsedChests.clear();
        currentTargetChest = null;
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) {
            onTick();
        } else if (event instanceof Event3D e) {
            render3D(e);
        } else if (event instanceof Event2D e) {
            render2D(e);
        }
    };

    private void onTick() {
        if (mc.world == null || mc.player == null) return;

        List<WardenChest> tempChests = new ArrayList<>();
        Vec3d playerPos = mc.player.getEntityPos();

        Set<BlockPos> allChestPositions = new HashSet<>();
        Collections.addAll(allChestPositions, PREDEFINED_CHESTS);
        allChestPositions.addAll(tracker.streamPositions().toList());

        for (BlockPos chestPos : allChestPositions) {
            WardenChest chest = new WardenChest();
            chest.pos = chestPos;
            chest.distanceToPlayer = playerPos.distanceTo(Vec3d.ofCenter(chestPos));

            parseHolograms(chest);
            tempChests.add(chest);
        }

        parsedChests = tempChests;

        currentTargetChest = parsedChests.stream()
                .min((c1, c2) -> {
                    if (c1.hasTimer != c2.hasTimer) {
                        return c1.hasTimer ? 1 : -1;
                    }
                    if (!c1.hasTimer) {
                        return Double.compare(c1.distanceToPlayer, c2.distanceToPlayer);
                    }
                    if (c1.secondsRemaining != c2.secondsRemaining) {
                        return Integer.compare(c1.secondsRemaining, c2.secondsRemaining);
                    }
                    return Double.compare(c1.distanceToPlayer, c2.distanceToPlayer);
                })
                .orElse(null);
    }

    private void parseHolograms(WardenChest chest) {
        if (mc.world == null) return;

        double searchRadius = 4.0;
        double bestDist = searchRadius;
        String foundText = "";

        Vec3d center = Vec3d.ofCenter(chest.pos);
        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (entity.getCustomName() == null && !(entity instanceof net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity)) {
                continue;
            }

            String text = "";
            if (entity.getCustomName() != null) {
                text = entity.getCustomName().getString();
            } else if (entity instanceof net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity textDisplay) {
                Text rawText = textDisplay.getText();
                if (rawText != null) {
                    text = rawText.getString();
                }
            }

            if (text.isEmpty()) continue;

            double dist = entity.getEntityPos().distanceTo(center);
            if (dist < bestDist) {
                if (isTimerText(text)) {
                    bestDist = dist;
                    foundText = text;
                }
            }
        }

        if (!foundText.isEmpty()) {
            chest.hasTimer = true;
            chest.timerText = foundText;
            chest.secondsRemaining = parseSeconds(foundText);
        } else {
            chest.hasTimer = false;
            chest.timerText = "";
            chest.secondsRemaining = 999999;
        }
    }

    private boolean isTimerText(String text) {
        String clean = Formatting.strip(text).toLowerCase().trim();
        if (!clean.matches(".*\\d+.*")) return false;
        return clean.contains(":") || clean.contains("сек") || clean.contains("с") || clean.contains("мин") || clean.contains("м") || clean.contains("sec") || clean.contains("s");
    }

    private int parseSeconds(String text) {
        String clean = Formatting.strip(text).toLowerCase().trim();

        Pattern p1 = Pattern.compile("(\\d+):(\\d+)");
        Matcher m1 = p1.matcher(clean);
        if (m1.find()) {
            try {
                return Integer.parseInt(m1.group(1)) * 60 + Integer.parseInt(m1.group(2));
            } catch (Exception ignored) {}
        }

        Pattern p2 = Pattern.compile("(\\d+)\\s*(сек|с\\b|s\\b)");
        Matcher m2 = p2.matcher(clean);
        if (m2.find()) {
            try {
                return Integer.parseInt(m2.group(1));
            } catch (Exception ignored) {}
        }

        Pattern p3 = Pattern.compile("(\\d+)\\s*(мин|м\\b|m\\b)");
        Matcher m3 = p3.matcher(clean);
        if (m3.find()) {
            try {
                return Integer.parseInt(m3.group(1)) * 60;
            } catch (Exception ignored) {}
        }

        return 99999;
    }

    private void render3D(Event3D event) {
        if (mc.world == null || mc.player == null || parsedChests.isEmpty()) return;

        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        MatrixStack stack = event.stack;

        if (renderESP.get()) {
            stack.push();
            stack.translate(-cam.x, -cam.y, -cam.z);
            VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(allocator);

            for (WardenChest chest : parsedChests) {
                Box box = new Box(chest.pos);
                Color boxColor = chest.hasTimer ? new Color(255, 100, 0, 140) : new Color(100, 255, 100, 140);
                drawOutlineBox(stack, imm, box, boxColor);
            }
            imm.draw();
            stack.pop();
        }

        if (currentTargetChest != null) {
            renderGPSPath(stack, cam);
        }
    }

    private void render2D(Event2D event) {
        if (mc.world == null || mc.player == null || parsedChests.isEmpty()) return;

        for (WardenChest chest : parsedChests) {
            if (!chest.hasTimer) continue;

            Vec3d worldPos = Vec3d.ofCenter(chest.pos).add(0, 0.65, 0);

            Vec3d screenPos = MathUtility.worldSpaceToScreenSpace(worldPos);

            if (screenPos.z > 0 && screenPos.z < 1) {
                renderNametag(screenPos, chest);
            }
        }
    }

    private void renderNametag(Vec3d screenPos, WardenChest chest) {
        float x = (float) Math.round(screenPos.x);
        float y = (float) Math.round(screenPos.y);

        MutableText finalText = Text.literal(chest.timerText);
        Color color = new Color(39, 39, 39);

        String cleanString = Formatting.strip(finalText.getString()).trim();
        float fontScale = textScale.get();

        float textWidth = Client.RENDERER.textWidth(cleanString, TextureUse.SFMEDIUM, fontScale);
        float textWidthOffset = textWidth + 6F;
        float labelHeight = fontScale + 4F;

        float bgX = x - textWidthOffset / 2F + 1;
        float bgY = y - labelHeight;

        float currentAlpha = Client.RENDERER.getCrenderSystem().alpha();

      //  Client.RENDERER.blur(bgX, bgY, textWidthOffset, labelHeight, new Vector4f(2), 4, 0);

        Client.RENDERER.getCrenderSystem().alpha(0.35F);
        Client.RENDERER.rect(bgX, bgY, textWidthOffset, labelHeight, new Vector4f(2), 1, color, color, color, color);

        Client.RENDERER.getCrenderSystem().alpha(currentAlpha);
        Client.RENDERER.text(finalText, x - textWidth / 2F, bgY + 2F, TextureUse.SFMEDIUM, fontScale);
    }

    private void renderGPSPath(MatrixStack stack, Vec3d cam) {
        RenderLayer particleLayer = ClientPipelines.PARTICLES.apply(GLOW_TEXTURE);
        VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(allocator);
        VertexConsumer buf = imm.getBuffer(particleLayer);

        stack.push();
        stack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = stack.peek().getPositionMatrix();

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        double px = MathHelper.lerp(tickDelta, mc.player.lastX, mc.player.getX());
        double py = MathHelper.lerp(tickDelta, mc.player.lastY, mc.player.getY());
        double pz = MathHelper.lerp(tickDelta, mc.player.lastZ, mc.player.getZ());

        Vec3d start = new Vec3d(px, py + 0.05, pz);
        Vec3d end = Vec3d.ofCenter(currentTargetChest.pos).add(0, -0.4, 0);

        Vec3d diff = end.subtract(start);
        double totalDist = diff.length();
        double spacing = dotSpacing.get();
        int pointsCount = (int) (totalDist / spacing);

        double movementProgress = (System.currentTimeMillis() % 1200) / 1200.0;
        int pathColor = themeColor.get() ? ClientSettings.INSTANCE.getColor(0).getRGB() : (currentTargetChest.hasTimer ? 0xFFFF7700 : 0xFF00FFCC);

        for (int i = 0; i < pointsCount; i++) {
            double t = (i + movementProgress) / pointsCount;
            if (t > 1.0) continue;

            Vec3d point = start.add(diff.multiply(t));
            double floorY = getFloorY(point.x, point.y, point.z);
            Vec3d finalPoint = new Vec3d(point.x, floorY, point.z);

            float alphaMult = (float) (1.0 - t);
            int finalColor = (pathColor & 0xFFFFFF) | ((int) (alphaMult * 255) << 24);

            drawFlatQuad(buf, mat, finalPoint, dotSize.get(), finalColor);
        }

        imm.draw(particleLayer);
        stack.pop();
    }

    private double getFloorY(double x, double y, double z) {
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        for (int i = 0; i < 5; i++) {
            BlockState state = mc.world.getBlockState(pos);
            if (!state.isAir() && !state.isLiquid()) {
                return pos.getY() + 1.03;
            }
            pos = pos.down();
        }
        return y + 0.03;
    }

    private void drawFlatQuad(VertexConsumer buf, Matrix4f mat, Vec3d pos, float size, int color) {
        float h = size * 0.5f;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >>> 24) & 0xFF;

        float px = (float) pos.x;
        float py = (float) pos.y;
        float pz = (float) pos.z;

        buf.vertex(mat, px - h, py, pz - h).texture(0, 1).color(r, g, b, a);
        buf.vertex(mat, px + h, py, pz - h).texture(1, 1).color(r, g, b, a);
        buf.vertex(mat, px + h, py, pz + h).texture(1, 0).color(r, g, b, a);
        buf.vertex(mat, px - h, py, pz + h).texture(0, 0).color(r, g, b, a);
    }

    private void drawOutlineBox(MatrixStack stack, VertexConsumerProvider.Immediate imm, Box b, Color c) {
        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE);
        Matrix4f mat = stack.peek().getPositionMatrix();
        int r = c.getRed(), g = c.getGreen(), b1 = c.getBlue(), a = c.getAlpha();
        float fx1 = (float) b.minX, fy1 = (float) b.minY, fz1 = (float) b.minZ;
        float fx2 = (float) b.maxX, fy2 = (float) b.maxY, fz2 = (float) b.maxZ;

        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b1, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b1, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b1, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b1, a);
        buf.vertex(mat, fx1, fy1, fz1).color(r, g, b1, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b1, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b1, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b1, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b1, a);
        buf.vertex(mat, fx1, fy2, fz1).color(r, g, b1, a);
        buf.vertex(mat, fx1, fy2, fz2).color(r, g, b1, a);
        buf.vertex(mat, fx1, fy1, fz2).color(r, g, b1, a);
        buf.vertex(mat, fx2, fy1, fz2).color(r, g, b1, a);
        buf.vertex(mat, fx2, fy2, fz2).color(r, g, b1, a);
        buf.vertex(mat, fx2, fy2, fz1).color(r, g, b1, a);
        buf.vertex(mat, fx2, fy1, fz1).color(r, g, b1, a);
    }
}