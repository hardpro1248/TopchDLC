package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Arrows;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

import java.awt.*;
import java.util.List;

public class GpsCommand extends Command {
    public static GpsCommand INSTANCE = new GpsCommand();

    public GpsCommand() {
        super("gps", "ну гпс по кордам");

        Client.EVENTS.register(this);
    }

    private float ANGLE = 0;

    private Vec3d current = null;

    public void setTarget(double x, double z) {
        current = new Vec3d(x, 0, z);
    }

    public void clearTarget() {
        current = null;
    }

    EventBus<Event> events = event -> {
        if (event instanceof Event2D) {
            if (current == null) {
                return;
            }

            float tick = mc.getRenderTickCounter().getTickProgress(true);
            ANGLE = MathHelper.lerpAngleDegrees(tick, ANGLE, ANGLE + MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - ANGLE) * 0.1f);
            Vec3d lLerp = mc.player.getLerpedPos(tick);

            CRenderSystem cRenderSystem = Client.RENDERER.getCrenderSystem();

            MatrixStack stack = Client.RENDERER.getStack();

            List<AbstractClientPlayerEntity> players = mc.world.getPlayers();

            boolean showDistance = players.size() < 20;

            float F = mc.gameRenderer.getCamera().getPitch();

            stack.push();

            Color color = ClientColors.FORE_COLOR;

            float fY = Arrows.INSTANCE.arrows3d.get() ? F / 90F : 1;
            double dist = Arrows.INSTANCE.arrows3d.get() ? current.distanceTo(lLerp) : 1;
            dist = Math.max(1, dist / 50F);

            double d = lLerp.x - current.x;
            double e = lLerp.z - current.z;
            double angle = Math.atan2(d, e) + ANGLE * MathUtility.TO_RADIANS;
            float x = (float) (mc.getWindow().getScaledWidth() / 2f + Math.sin(angle) * 45);
            float y = (float) (120 + Math.cos(angle) * 45 * fY);

            stack.translate(MathUtility.scaledX(x), MathUtility.scaledY(y), 0);
            stack.multiply(RotationAxis.NEGATIVE_Z.rotation((float) (angle - Math.PI / 2)));
            stack.translate(-MathUtility.scaledX(x), -MathUtility.scaledY(y), 0);


            Client.RENDERER.texture(Identifier.of("topchdlc", "images/ui/triangle.png"), x - 21, y - 21 + fY, 42, 42, 0, new Vector4f(0),
                    color, color, color, color);
            stack.pop();

            if (showDistance) {
                String text = String.format("%s", Math.ceil(current.distanceTo(lLerp) * 10.F) / 10.F);
                Client.RENDERER.textCentered(text, x, y + 4, TextureUse.SFMEDIUM, 6, ClientColors.FORE_COLOR);
            }
        }
    };


    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            if (args[1].equals("off")) {
                ChatUtility.send("gps disabled.");
                current = null;
            }
        } else if (args.length == 3) {
            double x = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            current = new Vec3d(x, 0, z);
            ChatUtility.send(String.format("gps set to %s", current));
        } else {
            error("usage .gps <off | x, z>");
        }
    }
}
