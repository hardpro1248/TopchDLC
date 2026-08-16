package gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.utilities;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventSendPacket;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.ElytraAura;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.math.ElytraAuraResolve;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPositionEntity;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static gg.topchdlc.vse.shutki.module.modules.impl.combat.ElytraAura.FakeLagCondition.TargetIsntGliding;

@UtilityClass
public class ElytraAuraDefensive implements MinecraftHolder {
    ElytraAura elytraAura = ElytraAura.INSTANCE;
    private TimeUtility blinkTime = new TimeUtility();
    private TimeUtility motionTime = new TimeUtility();
    public boolean blinking = false;

    final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    @Getter
    boolean defensiving = false;
    public boolean isMotion = false;

    Vec3d lastBlinked = Vec3d.ZERO;


    ArrayList<Vec3d> points = new ArrayList<>();

    public void handleEvent(Event event) {
        LivingEntity entity = TargetsUtility.getTarget();
        if (entity != null) {
            isMotion = !entity.isGliding();
        }
        if (event instanceof Event3D e) {
            e.stack.push();
            Client.RENDERER.toCamera(e.stack);
            VertexConsumer consumer = e.buffer.getBuffer(ClientPipelines.OUTLINE);
            Vec3d pos = lastBlinked;
            Vec3d pos2 = mc.player.getLerpedPos(mc.getRenderTickCounter().getTickProgress(true));

            Vector3f v = MathUtility.getNormal((float)pos.x, (float)pos.y, (float)pos.z, (float)pos2.x, (float)pos2.y, (float)pos2.z);
            consumer.vertex(e.stack.peek(), (float)pos.x, (float)pos.y, (float)pos.z).color(Color.GREEN.getRGB()).normal(e.stack.peek(), v);
            consumer.vertex(e.stack.peek(), (float)pos2.x, (float)pos2.y, (float)pos2.z).color(Color.GREEN.getRGB()).normal(e.stack.peek(), v);
            Box box =new Box(pos.subtract(0.1f), pos.add(0.1f));
            VertexRendering.drawOutline(e.stack, consumer, VoxelShapes.cuboid(box), 1, 1, 0, 1,1);
            e.stack.pop();
        }

        if (!ElytraAura.INSTANCE.defensive.get() ||
                (ElytraAura.INSTANCE.isAirStack.get() && InputUtil.isKeyPressed(window, ElytraAura.INSTANCE.bindAirStackSetting.getBind()))) {
            blinking = false;
            blink();
            return;
        }

        if (event instanceof EventAttack e) {
            if (e.target == TargetsUtility.getTarget() && !ElytraAura.INSTANCE.targetIsLeave(entity) && !elytraAura.antiAimIsActive) {
                blinkTime.reset();
            }
        }

        if (ElytraAura.INSTANCE.targetIsLeave(entity) || !isMotion) {
            blinking = false;
            blink();
            return;
        }

        if (entity == null) return;
        if (event instanceof EventGameTick) {


            if (mc.player.hurtTime > 0
                    || mc.player.getEntityPos().distanceTo(mc.player.getLastRenderPos()) < 0.15
                    || blinkTime.reached(600)
                    || mc.player.distanceTo(entity) > 5
                    || ElytraAura.INSTANCE.targetIsLeave(entity)) {
                blink();
                blinkTime.reset();
            }

            blinking = isMotion;

            if (entity.getEntityPos().distanceTo(entity.getLastRenderPos()) != 0)
                motionTime.reset();

            points.add(((ResolvedPositionEntity)entity).hachclientport$getResolvedPos());
            if (points.size() > 10) {
                points.removeFirst();
            }
        }

        if (event instanceof EventSendPacket e) {
            if (e.packet instanceof KeepAliveC2SPacket) return;

            boolean work =
                    !elytraAura.ifWorkitFakeLagSetting.is(TargetIsntGliding) || (!entity.isGliding() || entity.isOnGround() || ElytraAuraResolve.isStoyak(entity));

            if (elytraAura.isFakeLag.get() && work) {
                packetQueue.add(e.getPacket());
                e.cancel();
                blinking = true;
            }
        }
    }

    private void blink() {
        for (Packet<?> packet : packetQueue) {
            if (packet instanceof PlayerMoveC2SPacket move) {
                lastBlinked = new Vec3d(move.getX(mc.player.getX()), move.getY(mc.player.getY()), move.getZ(mc.player.getZ()));
            }
            NetworkUtility.sendWithoutEvent(packet);
        }
        packetQueue.clear();
        blinking = false;
    }

    public void onDisable() {
        blinking = false;
        blink();
    }
}