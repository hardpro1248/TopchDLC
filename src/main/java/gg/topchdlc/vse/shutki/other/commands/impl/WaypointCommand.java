package gg.topchdlc.vse.shutki.other.commands.impl;

import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class WaypointCommand extends Command {
    public static final WaypointCommand INSTANCE = new WaypointCommand();
    public static final WaypointRenderer RENDERER = new WaypointRenderer();

    public WaypointCommand() {
        super("waypoint", "вейпонт штучка", "list", "remove", "clear");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) { printUsage(); return; }
        switch (args[1].toLowerCase()) {
            case "list" -> {
                if (RENDERER.waypoints.isEmpty()) { ChatUtility.send("Нету вейпоинтов"); return; }
                for (int i = 0; i < RENDERER.waypoints.size(); i++) {
                    Waypoint wp = RENDERER.waypoints.get(i);
                    ChatUtility.send(String.format("[%d] %s -> %.1f %.1f %.1f", i, wp.name, wp.pos.x, wp.pos.y, wp.pos.z));
                }
            }
            case "clear" -> { RENDERER.waypoints.clear(); ChatUtility.send("Все вейпоинты удалены"); }
            case "remove" -> {
                if (args.length < 3) { error("usage: .waypoint remove <id>"); return; }
                try {
                    int id = Integer.parseInt(args[2]);
                    if (id < 0 || id >= RENDERER.waypoints.size()) { error("Неверный id. пропиши .waypoint list и увидишь id там типо вот так пишет [id] "); return; }
                    ChatUtility.send("Удалён: " + RENDERER.waypoints.remove(id).name);
                } catch (NumberFormatException ex) { error("id должен быть числом."); }
            }
            default -> {
                if (args.length < 4) { printUsage(); return; }
                try {
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    String name = args.length >= 5 ? args[4] : ("wp" + (RENDERER.waypoints.size() + 1));
                    int color = Color.HSBtoRGB((RENDERER.waypoints.size() * 0.37f) % 1f, 1.0f, 1.0f);
                    RENDERER.waypoints.add(new Waypoint(new Vec3d(x, y, z), name, color));
                    ChatUtility.send(String.format("Вейпоинт '%s': %.1f %.1f %.1f", name, x, y, z));
                } catch (NumberFormatException ex) { error("Координаты должны быть числами."); }
            }
        }
    }

    private void printUsage() {
        ChatUtility.send(".waypoint x y z name  |  list  |  remove <id>  |  clear");
    }
// мне стало лень в отдельный класс так что пусть буде сдесь
    public record Waypoint(Vec3d pos, String name, int color) {}
    public static class WaypointRenderer extends Module {
        private static final Identifier RHOMBUS = Identifier.of("topchdlc", "images/world/wp/waypoint.png");
        private static final RenderLayer RHOMBUS_LAYER = ClientPipelines.PARTICLES.apply(RHOMBUS);
        private static final float ICON_SIZE = 1.4f;
        private static final double BEAM_DIST = 100.0;

        public final List<Waypoint> waypoints = new ArrayList<>();
        private BufferAllocator allocator;
        private VertexConsumerProvider.Immediate imm;

        public WaypointRenderer() {
            super("WaypointRenderer", Category.RENDER, "x");
            nonActivatable();
        }

        EventBus<Event> bus = event -> {
            if (mc.player == null || mc.world == null || waypoints.isEmpty()) return;
            if (event instanceof Event3D e) {
                if (allocator == null) {
                    allocator = new BufferAllocator(524288);
                    imm = VertexConsumerProvider.immediate(allocator);
                }
                MatrixStack matrix = e.stack;
                Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
                float time = (System.currentTimeMillis() % 10000) / 1000f;
                float pulse = 0.85f + 0.15f * (float) Math.sin(time * 3f);

                for (Waypoint wp : waypoints) {
                    double dist = cam.distanceTo(wp.pos);
                    int alpha = (int)(255 * pulse);
                    int r = (wp.color >> 16) & 0xFF;
                    int g = (wp.color >> 8)  & 0xFF;
                    int b =  wp.color        & 0xFF;
                    matrix.push();
                    Client.RENDERER.setupOrientationMatrix(matrix,
                            (float) wp.pos.x, (float) wp.pos.y, (float) wp.pos.z);
                    matrix.multiply(mc.gameRenderer.getCamera().getRotation());
                    Matrix4f mat = matrix.peek().getPositionMatrix();
                    VertexConsumer buf = imm.getBuffer(RHOMBUS_LAYER);
                    float s = ICON_SIZE;
                    buf.vertex(mat, -s, -s, 0).texture(0, 1).color(r, g, b, alpha);
                    buf.vertex(mat,  s, -s, 0).texture(1, 1).color(r, g, b, alpha);
                    buf.vertex(mat,  s,  s, 0).texture(1, 0).color(r, g, b, alpha);
                    buf.vertex(mat, -s,  s, 0).texture(0, 0).color(r, g, b, alpha);
                    matrix.pop();

                    if (dist > BEAM_DIST) {
                        renderBeam(matrix, wp.pos, cam, r, g, b, alpha);
                    }
                }
                imm.draw();
            }
            if (event instanceof Event2D) {
                Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
                int sw = mc.getWindow().getScaledWidth();
                int sh = mc.getWindow().getScaledHeight();
                float fov = mc.options.getFov().getValue();
                float fovRad = (float) Math.toRadians(fov);
                float fovX = fovRad * ((float) sw / sh);
                float camYaw   = (float) Math.toRadians(mc.player.getYaw());
                float camPitch = (float) Math.toRadians(mc.player.getPitch());

                for (Waypoint wp : waypoints) {
                    double dist = cam.distanceTo(wp.pos);
                    String distStr = String.format("%.0fm", dist);

                    int r = (wp.color >> 16) & 0xFF;
                    int g = (wp.color >> 8)  & 0xFF;
                    int b =  wp.color        & 0xFF;
                    Color wpColor = new Color(r, g, b, 255);
                    Color white   = new Color(255, 255, 255, 255);
                    double dx = wp.pos.x - cam.x;
                    double dy = wp.pos.y - cam.y;
                    double dz = wp.pos.z - cam.z;
                    double targetYaw   = Math.atan2(-dx, dz);
                    double hDist       = Math.sqrt(dx * dx + dz * dz);
                    double targetPitch = -Math.atan2(dy, hDist);
                    double dYaw = targetYaw - camYaw;
                    while (dYaw >  Math.PI) dYaw -= 2 * Math.PI;
                    while (dYaw < -Math.PI) dYaw += 2 * Math.PI;
                    double dPitch = targetPitch - camPitch;
                    boolean inFront = Math.abs(dYaw) < Math.PI / 2 && Math.abs(dPitch) < Math.PI / 2;
                    double nx = dYaw   / (fovX / 2.0);
                    double ny = dPitch / (fovRad / 2.0);

                    float margin = 18f;
                    float halfW = sw / 2f - margin;
                    float halfH = sh / 2f - margin;

                    float sx, sy;
                    boolean onScreen;

                    if (inFront && Math.abs(nx) <= 1.0 && Math.abs(ny) <= 1.0) {
                        sx = (float)(sw / 2f + nx * halfW);
                        sy = (float)(sh / 2f + ny * halfH);
                        onScreen = true;
                    } else {
                        onScreen = false;
                        double ex = inFront ? nx : (dYaw >= 0 ? 1.0 : -1.0);
                        double ey = ny;
                        double scale = Math.max(Math.abs(ex), Math.abs(ey));
                        if (scale < 0.001) continue;
                        ex /= scale; ey /= scale;
                        sx = (float)(sw / 2f + ex * halfW);
                        sy = (float)(sh / 2f + ey * halfH);
                    }

                    float textScale = 6.5f;
                    float nameW = Client.RENDERER.textWidth(wp.name, TextureUse.SFMEDIUM, textScale);
                    float distW = Client.RENDERER.textWidth(" " + distStr, TextureUse.SFMEDIUM, textScale);
                    float totalW = nameW + distW + 6;
                    float boxH = 10f;

                    float bx = sx - totalW / 2f;
                    float by = sy - boxH;
                    Client.RENDERER.blur(bx, by, totalW, boxH, new Vector4f(0), 5, 0);
                    float prevAlpha = Client.RENDERER.getCrenderSystem().alpha();
                    Client.RENDERER.getCrenderSystem().alpha(0.45f);
                    Color bg = new Color(0, 0, 0, 180);
                    Client.RENDERER.rect(bx, by, totalW, boxH, new Vector4f(0), 1, bg, bg, bg, bg);
                    Client.RENDERER.getCrenderSystem().alpha(prevAlpha);
                    Client.RENDERER.text(wp.name, bx + 3, by + 1, TextureUse.SFMEDIUM, textScale, white);
                    Client.RENDERER.text(" " + distStr, bx + 3 + nameW, by + 1, TextureUse.SFMEDIUM, textScale, wpColor);
                }
            }
        };

        private void renderBeam(MatrixStack matrix, Vec3d pos, Vec3d cam, int r, int g, int b, int alpha) {
            float relX = (float)(pos.x - cam.x);
            float topY = (float)(pos.y - cam.y);
            float botY = topY - 64f;
            float relZ = (float)(pos.z - cam.z);

            double hDist = Math.sqrt(relX * relX + relZ * relZ);
            float nx = hDist > 0.001 ? (float)(relZ / hDist) : 1f;
            float nz = hDist > 0.001 ? (float)(-relX / hDist) : 0f;
            float w = 0.1f;
            int beamAlpha = Math.max(40, alpha / 3);

            matrix.push();
            Matrix4f mat = matrix.peek().getPositionMatrix();
            VertexConsumer qbuf = imm.getBuffer(RHOMBUS_LAYER);
            float x0 = relX + nx * w, z0 = relZ + nz * w;
            float x1 = relX - nx * w, z1 = relZ - nz * w;
            qbuf.vertex(mat, x0, topY, z0).texture(0, 0).color(r, g, b, beamAlpha);
            qbuf.vertex(mat, x1, topY, z1).texture(1, 0).color(r, g, b, beamAlpha);
            qbuf.vertex(mat, x1, botY, z1).texture(1, 1).color(r, g, b, 0);
            qbuf.vertex(mat, x0, botY, z0).texture(0, 1).color(r, g, b, 0);
            matrix.pop();
        }
    }
}
