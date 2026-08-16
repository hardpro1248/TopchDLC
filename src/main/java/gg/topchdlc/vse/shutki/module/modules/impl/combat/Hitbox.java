package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.*;
/**
 * Create by daun kvass
 */
public class Hitbox extends Module {
    public static final Hitbox INSTANCE = new Hitbox();
    private Hitbox() {
        super("Hitbox", Category.COMBAT, "Увеличивает хитбокс игрока");
    }
    public final SliderSetting expand = sliderSetting("Размер", 0.2f, 0.0f, 2.0f).increment(0.05f);
    public final CheckBox ignFr = checkbox("Игнор друзей", true);
    //$$$$$$NAP $ysem
    public final CheckBox legitSnap = checkbox("Включить Легит снапы", false);
    public final SliderSetting snapSpeed = sliderSetting("Скорость", 2f, 1f, 10f).increment(0.5f).visible(legitSnap::get);
    public final CheckBox snapOnlyYaw = checkbox("Только Yaw (XZ)", false).visible(legitSnap::get);
    // хуйня для отспут чисто для спуки где в края дай бох зарегает и хз
    public final SliderSetting snapInset = sliderSetting("Отступ от края", 0.05f, 0.0f, 0.2f).increment(0.01f).visible(legitSnap::get);
    public final CheckBox snapSmoothReturn = checkbox("Плавный возврат", false).visible(legitSnap::get);
    public final SliderSetting snapReturnTicks = sliderSetting("Время возврата (тики)", 5, 2, 15).visible(legitSnap::get);
    public final SliderSetting snapReturnDelay = sliderSetting("Задержка (тики)", 1, 0, 10).visible(legitSnap::get);
   
    public final CheckBox recoil = checkbox("Включить Отводку", false);
    public final CheckBox recoilUseYaw = checkbox("Yaw (XZ)", true).visible(recoil::get);
    public final SliderSetting recoilYawStrength = sliderSetting("Сила Yaw", 8f, 2f, 25f).increment(0.5f).visible(recoil::get);
    public final CheckBox recoilUsePitch = checkbox("Pitch (Y)", true).visible(recoil::get);
    public final SliderSetting recoilPitchStrength = sliderSetting("Сила Pitch", 4f, 1f, 20f).increment(0.5f).visible(recoil::get);
    public final SliderSetting recoilRandom = sliderSetting("Разброс", 5f, 0f, 15f).increment(0.5f).visible(recoil::get);
    public final SliderSetting recoilHoldTicks = sliderSetting("Держать (тики)", 2, 1, 6).visible(recoil::get);
    public final SliderSetting recoilReturnTicks = sliderSetting("Возврат (тики)", 4, 1, 10).visible(recoil::get);
    public final CheckBox showExpandedHitbox = checkbox("Показать хитбокс", false);
    public final CheckBox hitboxFill = checkbox("Заливка", true).visible(showExpandedHitbox::get);
    public final CheckBox hitboxOutline = checkbox("Обводка", true).visible(showExpandedHitbox::get);
    public final ColorSetting hitboxColor = colorSetting("Цвет", new Color(255, 100, 100, 80)).visible(()->hitboxFill.get() && showExpandedHitbox.get());
    public final ColorSetting hitboxOutlineColor = colorSetting("Цвет обводки", new Color(255, 100, 100, 150)).visible(()->hitboxOutline.get() && showExpandedHitbox.get());
    public final SliderSetting hitboxLineWidth = sliderSetting("Толщина Обводки", 1.5f, 0.5f, 5f).increment(0.1f).visible(()->hitboxOutline.get() && showExpandedHitbox.get());

    private RecoilState activeRecoil = null;
    private Angle preSnapAngle = null;
    private float snapDeltaYaw = 0;
    private float snapDeltaPitch = 0;
    private SnapReturnState snapReturn = null;
    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) onTick();
        else if (event instanceof Event3D e) onRender3D(e);
    };
    private void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (activeRecoil != null) {
            tickRecoil();
            return;
        }
        if (snapReturn != null) {
            tickSnapReturn();
        }
    }

    private void tickRecoil() {
        activeRecoil.ticksElapsed++;
        int holdT = recoilHoldTicks.getInt();
        int returnT = recoilReturnTicks.getInt();
        int totalTicks = holdT + returnT;
        if (activeRecoil.ticksElapsed > totalTicks) {
            activeRecoil = null;
            preSnapAngle = null;
            snapReturn = null;
            return;
        }
        if (activeRecoil.ticksElapsed <= holdT) {
            float progress = (float) activeRecoil.ticksElapsed / holdT;
            float eased = easeOutQuad(progress);
            float yawOffset = activeRecoil.totalYawOffset * eased;
            float pitchOffset = activeRecoil.totalPitchOffset * eased;
            Angle recoiled = new Angle(
                    activeRecoil.snapAngle.getYaw() + yawOffset,
                    MathHelper.clamp(activeRecoil.snapAngle.getPitch() + pitchOffset, -90, 90)
            );
            Client.ROTATION.rotate(
                    DefaultRotation.INSTANCE,
                    recoiled, 1, false,
                    MovementCorrection.SILENT, 95
            );
        } else {
            int returnTick = activeRecoil.ticksElapsed - holdT;
            float returnProgress = (float) returnTick / returnT;
            float eased = easeOutCubic(returnProgress);
            Angle recoiledFull = new Angle(
                    activeRecoil.snapAngle.getYaw() + activeRecoil.totalYawOffset,
                    MathHelper.clamp(activeRecoil.snapAngle.getPitch() + activeRecoil.totalPitchOffset, -90, 90)
            );
            Angle returnTo = activeRecoil.returnAngle;
            float lerpYaw = recoiledFull.getYaw()
                    + MathHelper.wrapDegrees(returnTo.getYaw() - recoiledFull.getYaw()) * eased;
            float lerpPitch = recoiledFull.getPitch()
                    + (returnTo.getPitch() - recoiledFull.getPitch()) * eased;
            Angle returning = new Angle(lerpYaw, MathHelper.clamp(lerpPitch, -90, 90));
            Client.ROTATION.rotate(
                    DefaultRotation.INSTANCE,
                    returning, 1, false,
                    MovementCorrection.SILENT, 95
            );
        }
    }

    private void tickSnapReturn() {
        snapReturn.ticksElapsed++;
        int delay = snapReturnDelay.getInt();
        int returnT = snapReturnTicks.getInt();
        if (snapReturn.ticksElapsed <= delay) {
            Client.ROTATION.rotate(
                    DefaultRotation.INSTANCE,
                    snapReturn.snappedAngle, 1, false,
                    MovementCorrection.SILENT, 90
            );
            return;
        }
        int returnTick = snapReturn.ticksElapsed - delay;
        if (returnTick > returnT) {
            snapReturn = null;
            preSnapAngle = null;
            return;
        }
        float progress = (float) returnTick / returnT;
        float eased = easeOutCubic(progress);
        Angle from = snapReturn.snappedAngle;
        Angle to = snapReturn.returnAngle;
        float lerpYaw = from.getYaw()
                + MathHelper.wrapDegrees(to.getYaw() - from.getYaw()) * eased;
        float lerpPitch = from.getPitch()
                + (to.getPitch() - from.getPitch()) * eased;
        Angle current = new Angle(lerpYaw, MathHelper.clamp(lerpPitch, -90, 90));
        Client.ROTATION.rotate(
                DefaultRotation.INSTANCE,
                current, 1, false,
                MovementCorrection.SILENT, 90
        );
    }
    private void onRender3D(Event3D event) {
        if (!showExpandedHitbox.get() || mc.player == null || mc.world == null) return;
        MatrixStack stack = event.stack;
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        BufferAllocator allocator = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(allocator);
        try {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof PlayerEntity player)) continue;
                if (entity == mc.player) continue;
                if (ignFr.get() && Client.FRIENDS.isFriend(player)) continue;
                float expandVal = expand.get();
                if (expandVal <= 0) continue;
                double x = entity.getX();
                double y = entity.getY();
                double z = entity.getZ();
                float w = entity.getWidth() / 2f;
                float h = entity.getHeight();
                double minX = x - w - expandVal - cam.x;
                double minY = y - cam.y;
                double minZ = z - w - expandVal - cam.z;
                double maxX = x + w + expandVal - cam.x;
                double maxY = y + h - cam.y;
                double maxZ = z + w + expandVal - cam.z;
                if (hitboxFill.get()) {
                    drawFilledBox(stack, imm, minX, minY, minZ, maxX, maxY, maxZ, hitboxColor.get());
                }
                if (hitboxOutline.get()) {
                    drawOutlineBox(stack, imm, minX, minY, minZ, maxX, maxY, maxZ, hitboxOutlineColor.get());
                }
            }
            imm.draw();
        } finally {
            allocator.close();
        }
    }
    // итак ебаный снапы как в системе вроде
    public void trySnap(Entity target) {
        if (!isEnabled()) return;
        if (mc.player == null) return;
        if (!(target instanceof LivingEntity)) return;
        preSnapAngle = Client.ROTATION.getRotate();
        if (!legitSnap.get()) {
            snapDeltaYaw = 0;
            snapDeltaPitch = 0;
            return;
        }
        float expandVal = expand.get();
        if (expandVal <= 0) {
            snapDeltaYaw = 0;
            snapDeltaPitch = 0;
            return;
        }
        Vec3d eyes = mc.player.getEyePos();
        Angle currentRot = Client.ROTATION.getRotate();
        Vec3d lookDir = getLookVec(currentRot);
        double reach = mc.player.isCreative() ? 5.0 : 3.0;
        Vec3d lookEnd = eyes.add(lookDir.multiply(reach));
        Box realBox = target.getBoundingBox();
        Box expandedBox = realBox.expand(expandVal);
        var realHit = realBox.raycast(eyes, lookEnd);
        if (realHit.isPresent()) {
            snapDeltaYaw = 0;
            snapDeltaPitch = 0;
            return;
        }
        var expandedHit = expandedBox.raycast(eyes, lookEnd);
        if (expandedHit.isEmpty()) {
            snapDeltaYaw = 0;
            snapDeltaPitch = 0;
            return;
        }
        Vec3d snapPoint = findBestSnapPoint(eyes, realBox, currentRot);
        Angle targetAngle = RotationUtility.calculate(snapPoint);
        if (snapOnlyYaw.get()) {
            targetAngle = new Angle(targetAngle.getYaw(), currentRot.getPitch());
        }
        snapDeltaYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - currentRot.getYaw());
        snapDeltaPitch = snapOnlyYaw.get() ? 0 : (targetAngle.getPitch() - currentRot.getPitch());
        boolean didSnap = Math.abs(snapDeltaYaw) > 0.1f || Math.abs(snapDeltaPitch) > 0.1f;
        Client.ROTATION.rotate(
                DefaultRotation.INSTANCE,
                targetAngle,
                (int) snapSpeed.get(),
                false,
                MovementCorrection.SILENT,
                100
        );
        if (didSnap && snapSmoothReturn.get()) {
            snapReturn = new SnapReturnState(targetAngle, currentRot);
        }
    }

    // итак мулти хуи
    private Vec3d findBestSnapPoint(Vec3d eyes, Box realBox, Angle currentRot) {
        float inset = snapInset.get();
        double inMinX = realBox.minX + inset;
        double inMaxX = realBox.maxX - inset;
        double inMinY = realBox.minY + inset;
        double inMaxY = realBox.maxY - inset;
        double inMinZ = realBox.minZ + inset;
        double inMaxZ = realBox.maxZ - inset;
        if (inMinX >= inMaxX || inMinY >= inMaxY || inMinZ >= inMaxZ) {
            return new Vec3d(
                    (realBox.minX + realBox.maxX) * 0.5,
                    (realBox.minY + realBox.maxY) * 0.5,
                    (realBox.minZ + realBox.maxZ) * 0.5
            );
        }

        boolean onlyYaw = snapOnlyYaw.get();
        Vec3d bestPoint = null;
        float bestAngleDist = Float.MAX_VALUE;
        int steps = 5;
        for (int xi = 0; xi <= steps; xi++) {
            for (int yi = 0; yi <= steps; yi++) {
                for (int zi = 0; zi <= steps; zi++) {
                    boolean onSurface = (xi == 0 || xi == steps ||
                            yi == 0 || yi == steps ||
                            zi == 0 || zi == steps);
                    if (!onSurface) continue;
                    double px = inMinX + (inMaxX - inMinX) * ((double) xi / steps);
                    double py = inMinY + (inMaxY - inMinY) * ((double) yi / steps);
                    double pz = inMinZ + (inMaxZ - inMinZ) * ((double) zi / steps);
                    Vec3d point = new Vec3d(px, py, pz);
                    Angle pointAngle = RotationUtility.calculate(point);
                    float angleDist;
                    if (onlyYaw) {
                        angleDist = Math.abs(MathHelper.wrapDegrees(pointAngle.getYaw() - currentRot.getYaw()));
                    } else {
                        float dYaw = MathHelper.wrapDegrees(pointAngle.getYaw() - currentRot.getYaw());
                        float dPitch = pointAngle.getPitch() - currentRot.getPitch();
                        angleDist = (float) Math.sqrt(dYaw * dYaw + dPitch * dPitch);
                    }

                    if (angleDist < bestAngleDist) {
                        bestAngleDist = angleDist;
                        bestPoint = point;
                    }
                }
            }
        }
        if (onlyYaw && bestPoint != null) {
            Vec3d lookDir = getLookVec(currentRot);
            double dx = bestPoint.x - eyes.x;
            double dz = bestPoint.z - eyes.z;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            double lookY = eyes.y + lookDir.y * (horizDist / Math.max(
                    Math.sqrt(lookDir.x * lookDir.x + lookDir.z * lookDir.z), 0.001));
            double clampedY = MathHelper.clamp(lookY, inMinY, inMaxY);
            bestPoint = new Vec3d(bestPoint.x, clampedY, bestPoint.z);
        }

        if (bestPoint == null) {
            bestPoint = new Vec3d(
                    (realBox.minX + realBox.maxX) * 0.5,
                    (realBox.minY + realBox.maxY) * 0.5,
                    (realBox.minZ + realBox.maxZ) * 0.5
            );
        }

        return bestPoint;
    }

    // люти отводка все ач в ахуе с этой хуити
    public void onAttack(Entity target) {
        if (!isEnabled() || !recoil.get()) return;
        if (mc.player == null) return;
        boolean useYaw = recoilUseYaw.get();
        boolean usePitch = recoilUsePitch.get();
        if (!useYaw && !usePitch) return;
        snapReturn = null;
        Angle snapAngle = Client.ROTATION.getRotate();
        float baseYawDir, basePitchDir;
        if (Math.abs(snapDeltaYaw) > 0.5f || Math.abs(snapDeltaPitch) > 0.5f) {
            float snapLen = (float) Math.sqrt(snapDeltaYaw * snapDeltaYaw + snapDeltaPitch * snapDeltaPitch);
            baseYawDir = snapDeltaYaw / snapLen;
            basePitchDir = snapDeltaPitch / snapLen;
        } else {
            double randAngle = Math.random() * Math.PI * 2;
            baseYawDir = (float) Math.sin(randAngle);
            basePitchDir = (float) Math.cos(randAngle) * 0.5f;
        }
        float randYaw = (float) (Math.random() - 0.5) * 2f * recoilRandom.get();
        float randPitch = (float) (Math.random() - 0.5) * 2f * recoilRandom.get() * 0.5f;
        float totalYaw = 0;
        float totalPitch = 0;
        if (useYaw) {
            totalYaw = baseYawDir * recoilYawStrength.get() + randYaw;
        }
        if (usePitch) {
            totalPitch = basePitchDir * recoilPitchStrength.get() + randPitch;
        }
        Angle returnTo = preSnapAngle != null ? preSnapAngle : snapAngle;
        activeRecoil = new RecoilState(snapAngle, returnTo, totalYaw, totalPitch);
    }

    private Vec3d getLookVec(Angle angle) {
        float yaw = angle.getYaw();
        float pitch = angle.getPitch();
        float f = pitch * (float) (Math.PI / 180.0);
        float g = -yaw * (float) (Math.PI / 180.0);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    private float easeOutQuad(float x) {
        return 1 - (1 - x) * (1 - x);
    }

    private float easeOutCubic(float x) {
        return 1 - (float) Math.pow(1 - x, 3);
    }

    private void drawFilledBox(MatrixStack stack, VertexConsumerProvider.Immediate imm,
                               double x1, double y1, double z1,
                               double x2, double y2, double z2, Color c) {
        VertexConsumer buf = imm.getBuffer(RenderLayers.debugQuads());
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        Matrix4f mat = stack.peek().getPositionMatrix();
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
        VertexConsumer buf = imm.getBuffer(RenderLayers.linesTranslucent());
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        Matrix4f mat = stack.peek().getPositionMatrix();
        float fx1 = (float) x1, fy1 = (float) y1, fz1 = (float) z1;
        float fx2 = (float) x2, fy2 = (float) y2, fz2 = (float) z2;
        float lw = hitboxLineWidth.get();

        drawLine(buf, mat, fx1, fy1, fz1, fx2, fy1, fz1, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy1, fz1, fx2, fy1, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy1, fz2, fx1, fy1, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx1, fy1, fz2, fx1, fy1, fz1, r, g, b, a, lw);

        drawLine(buf, mat, fx1, fy2, fz1, fx2, fy2, fz1, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy2, fz1, fx2, fy2, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy2, fz2, fx1, fy2, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx1, fy2, fz2, fx1, fy2, fz1, r, g, b, a, lw);

        drawLine(buf, mat, fx1, fy1, fz1, fx1, fy2, fz1, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy1, fz1, fx2, fy2, fz1, r, g, b, a, lw);
        drawLine(buf, mat, fx2, fy1, fz2, fx2, fy2, fz2, r, g, b, a, lw);
        drawLine(buf, mat, fx1, fy1, fz2, fx1, fy2, fz2, r, g, b, a, lw);
    }

    private void drawLine(VertexConsumer buf, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a, float lw) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.0001f) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(dx, dy, dz).lineWidth(lw);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(dx, dy, dz).lineWidth(lw);
    }

    @Override
    public void toggle() {
        super.toggle();
        activeRecoil = null;
        preSnapAngle = null;
        snapReturn = null;
    }
    private static class RecoilState {
        final Angle snapAngle;
        final Angle returnAngle;
        final float totalYawOffset;
        final float totalPitchOffset;
        int ticksElapsed = 0;

        RecoilState(Angle snap, Angle returnTo, float yawOff, float pitchOff) {
            this.snapAngle = snap;
            this.returnAngle = returnTo;
            this.totalYawOffset = yawOff;
            this.totalPitchOffset = pitchOff;
        }
    }

    private static class SnapReturnState {
        final Angle snappedAngle;
        final Angle returnAngle;
        int ticksElapsed = 0;

        SnapReturnState(Angle snapped, Angle returnTo) {
            this.snappedAngle = snapped;
            this.returnAngle = returnTo;
        }
    }
}