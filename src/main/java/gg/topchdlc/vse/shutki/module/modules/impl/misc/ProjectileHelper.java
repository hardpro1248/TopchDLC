package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.AuraModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RotationUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectileHelper extends Module {
    public static final ProjectileHelper INSTANCE = new ProjectileHelper();

    public enum Weapon implements EnumChoice {
        TRIDENT("Трезубец", true), BOW("Лук", true), CROSSBOW("Арбалет", true);
        final String label; final boolean def;
        Weapon(String l, boolean d) { label = l; def = d; }
        @Override public String getRenderName() { return label; }
        @Override public boolean isDefaultEnabled() { return def; }
    }

    public enum Throwable implements EnumChoice {
        SNOWBALL("Снежок", true), EGG("Яйцо", true), ENDER_PEARL("Эндер-жемчуг", true), SPLASH_POTION("Сплеш-зелье", true), LINGERING_POTION("Длящееся зелье", true);
        final String label; final boolean def;
        Throwable(String l, boolean d) { label = l; def = d; }
        @Override public String getRenderName() { return label; }
        @Override public boolean isDefaultEnabled() { return def; }
    }

    private final Group aimbotGroup = group("Аимбот").toggleable(true);
    private final MultiEnumSetting<Weapon> weapons = aimbotGroup.add(new MultiEnumSetting<>("Оружие", Weapon.TRIDENT, Weapon.BOW, Weapon.CROSSBOW));
    private final SliderSetting range = aimbotGroup.sliderSetting("Дальность", 20f, 5f, 60f).increment(1f);
    private final SliderSetting predictTicks = aimbotGroup.sliderSetting("Предикт", 0.5f, 0f, 1f).increment(0.05f);

    private final Group displayGroup = group("Отображение").toggleable(true);
    private final CheckBox showTrajectory = displayGroup.checkbox("Линия траектории", true);
    private final CheckBox showLanding = displayGroup.checkbox("Точка приземления", true);
    private final CheckBox showInfo = displayGroup.checkbox("Инфо в руке", true);
    private final ColorSetting lineColor = displayGroup.colorSetting("Цвет линии", new Color(100, 200, 255, 100));
    private final ColorSetting landingColor = displayGroup.colorSetting("Цвет точки", new Color(255, 80, 80, 140));
    private final SliderSetting maxSimTicks = displayGroup.sliderSetting("Макс. тиков симуляции", 200, 50, 500);

    private final Group throwableGroup = group("Кидабельные").toggleable(true);
    private final MultiEnumSetting<Throwable> throwables = throwableGroup.add(new MultiEnumSetting<>("Предметы", Throwable.SNOWBALL, Throwable.EGG, Throwable.ENDER_PEARL, Throwable.SPLASH_POTION, Throwable.LINGERING_POTION));
    private final CheckBox showThrowTrajectory = throwableGroup.checkbox("Траектория броска", true);
    private final CheckBox showThrowLanding = throwableGroup.checkbox("Точка падения", true);
    private final CheckBox showPotionRadius = throwableGroup.checkbox("Радиус зелья", true);
    private final ColorSetting throwLineColor = throwableGroup.colorSetting("Цвет линии броска", new Color(180, 255, 120, 100));
    private final ColorSetting splashRadiusColor = throwableGroup.colorSetting("Цвет радиуса сплеш", new Color(255, 100, 200, 120));
    private final ColorSetting lingeringRadiusColor = throwableGroup.colorSetting("Цвет радиуса длящегося", new Color(200, 100, 255, 80));
    private static final Color TARGET_HIT_COLOR = new Color(255, 40, 40, 220);
    private static final Color TARGET_MARKER_COLOR = new Color(255, 30, 30, 255);

    private SimResult currentSim = null;

    private ProjectileHelper() {
        super("ProjectileHelper", Category.Misc, "Помощник стрельбы и бросков");
    }

    public void onEvent(Event event) {
        if (event instanceof EventGameTick) onTick();
        if (event instanceof Event3D e) onRender3D(e);
        if (event instanceof Event2D e) onRender2D(e);
    }

    private void onTick() {
        if (nullCheck()) return;

        ItemStack held = mc.player.getMainHandStack();
        boolean charging = isCharging(held);
        boolean active = aimbotGroup.enabled.get();

        if (!active || !charging) {
            if (TargetsUtility.getTarget() != null && !isKillAuraActive()) {
                TargetsUtility.reset();
            }
            return;
        }

        LivingEntity target = TargetsUtility.find(range.get(), TargetsUtility.Sort.Distance);
        if (target == null) return;

        Vec3d targetPos = predictTargetPos(target, mc.player.getEyePos(), held);
        Angle aim = calcProjectileAngle(mc.player.getEyePos(), targetPos, held);
        if (aim != null) {
            Client.ROTATION.rotate(DefaultRotation.INSTANCE, aim, 3, false, MovementCorrection.SILENT, Priorities.AIMBOT);
        }
    }

    private boolean isKillAuraActive() {
        var aura = AuraModule.INSTANCE;
        return aura != null && aura.isEnabled() && TargetsUtility.getTarget() != null;
    }

    private void onRender3D(Event3D event) {
        if (mc.player == null || mc.world == null) return;

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d origin = mc.player.getCameraPosVec(tickDelta);
        Vec3d playerVel = mc.player.getVelocity();
        Angle currentAngle = Angle.fromPlayer();
        ItemStack held = mc.player.getMainHandStack();

        MatrixStack stack = event.stack;
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        BufferAllocator alloc = new BufferAllocator(262144);
        VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(alloc);

        try {
            stack.push();
            stack.translate(-cam.x, -cam.y, -cam.z);

            if (displayGroup.enabled.get() && isEnabledWeapon(held)) {
                if (held.isOf(Items.CROSSBOW) && hasMultishot(held)) {
                    Angle leftAngle = new Angle(currentAngle.getYaw() - 10f, currentAngle.getPitch());
                    Angle rightAngle = new Angle(currentAngle.getYaw() + 10f, currentAngle.getPitch());

                    SimResult simCenter = simulate(origin, initialVelocity(currentAngle, held, playerVel), held);
                    SimResult simLeft   = simulate(origin, initialVelocity(leftAngle, held, playerVel), held);
                    SimResult simRight  = simulate(origin, initialVelocity(rightAngle, held, playerVel), held);

                    renderSingleSim(stack, imm, simCenter);
                    renderSingleSim(stack, imm, simLeft);
                    renderSingleSim(stack, imm, simRight);

                    currentSim = simCenter.hitEntity ? simCenter : (simLeft.hitEntity ? simLeft : (simRight.hitEntity ? simRight : simCenter));
                } else {
                    SimResult sim = simulate(origin, initialVelocity(currentAngle, held, playerVel), held);
                    currentSim = sim;
                    renderSingleSim(stack, imm, sim);
                }
            } else { currentSim = null; }

            if (throwableGroup.enabled.get() && isEnabledThrowable(held)) {
                SimResult tsim = simulateThrowable(origin, throwableInitialVelocity(currentAngle, held, playerVel), held);
                if (currentSim == null) currentSim = tsim;

                if (!tsim.points.isEmpty()) {
                    Color curThrowLine = tsim.hitEntity ? TARGET_HIT_COLOR : throwLineColor.get();

                    if (showThrowTrajectory.get()) drawSmoothLine(stack, imm, tsim.points, curThrowLine);
                    if (showThrowLanding.get() && tsim.landing != null) {
                        drawMarker(stack, imm, tsim.landing, tsim.landingNormal, tsim.hitEntity ? TARGET_MARKER_COLOR : throwLineColor.get(), 0.15f);
                    }

                    if (showPotionRadius.get() && tsim.landing != null) {
                        Color curRadius = tsim.hitEntity ? TARGET_HIT_COLOR : splashRadiusColor.get();
                        if (held.isOf(Items.SPLASH_POTION)) drawRadiusCircle(stack, imm, tsim.landing, 2.0f, curRadius);
                        else if (held.isOf(Items.LINGERING_POTION)) {
                            drawRadiusCircle(stack, imm, tsim.landing, 3.0f, lingeringRadiusColor.get());
                            drawRadiusDisc(stack, imm, tsim.landing, 3.0f, lingeringRadiusColor.get());
                        }
                    }
                }
            }

            stack.pop();
            imm.draw();
        } finally { alloc.close(); }
    }

    private void renderSingleSim(MatrixStack stack, VertexConsumerProvider.Immediate imm, SimResult sim) {
        if (sim == null || sim.points.isEmpty()) return;

        Color curLineColor = sim.hitEntity ? TARGET_HIT_COLOR : lineColor.get();
        Color curLandingColor = sim.hitEntity ? TARGET_MARKER_COLOR : landingColor.get();

        if (showTrajectory.get()) {
            drawSmoothLine(stack, imm, sim.points, curLineColor);
        }
        if (showLanding.get() && sim.landing != null) {
            drawMarker(stack, imm, sim.landing, sim.landingNormal, curLandingColor, sim.hitEntity ? 0.22f : 0.18f);
        }
    }

    private void onRender2D(Event2D event) {
        if (currentSim != null && currentSim.landing != null && showInfo.get()) {
            renderLandingInfo(Client.RENDERER.getDrawContext(), currentSim, mc.player.getMainHandStack());
        }
    }

    private SimResult simulate(Vec3d origin, Vec3d vel, ItemStack held) {
        return simulateWorld(origin, vel, 0.05f, 0.99f);
    }

    private SimResult simulateThrowable(Vec3d origin, Vec3d vel, ItemStack held) {
        float gravity = (held.isOf(Items.SPLASH_POTION) || held.isOf(Items.LINGERING_POTION)) ? 0.05f : 0.03f;
        return simulateWorld(origin, vel, gravity, 0.99f);
    }

    private SimResult simulateWorld(Vec3d origin, Vec3d vel, float gravity, float drag) {
        List<Vec3d> pts = new ArrayList<>();
        double px = origin.x, py = origin.y, pz = origin.z;
        double vx = vel.x, vy = vel.y, vz = vel.z;
        pts.add(origin);

        for (int i = 0; i < (int) maxSimTicks.get(); i++) {
            vx *= drag; vy *= drag; vz *= drag;
            vy -= gravity;

            Vec3d current = new Vec3d(px, py, pz);
            Vec3d next = new Vec3d(px + vx, py + vy, pz + vz);
            var blockHit = mc.world.raycast(new RaycastContext(
                    current, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
            ));

            Vec3d checkEndPos = next;
            if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
                checkEndPos = blockHit.getPos();
            }

            EntityHitResult entityHit = findEntityHit(current, checkEndPos);

            if (entityHit != null) {
                pts.add(entityHit.pos);
                return new SimResult(pts, entityHit.pos, entityHit.normal, i + 1, true, entityHit.entity);
            }

            if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
                pts.add(blockHit.getPos());
                return new SimResult(pts, blockHit.getPos(), new Vec3d(blockHit.getSide().getUnitVector()), i + 1, false, null);
            }

            pts.add(next);
            px = next.x; py = next.y; pz = next.z;
            if (py < mc.world.getBottomY() - 10) break;
        }

        return new SimResult(pts, pts.get(pts.size() - 1), new Vec3d(0, 1, 0), pts.size(), false, null);
    }

    private record EntityHitResult(Entity entity, Vec3d pos, Vec3d normal, double distSq) {}

    /**
     * Математически точный Raycast отрезка по граням AABB-коробки сущности.
     * Возвращает точный вектор нормали задетой плоскости (как для блоков!).
     */
    private EntityHitResult findEntityHit(Vec3d start, Vec3d end) {
        if (mc.world == null || mc.player == null) return null;

        Box searchBox = new Box(start, end).expand(0.5);
        List<Entity> candidates = mc.world.getOtherEntities(mc.player, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player);

        EntityHitResult closestHit = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : candidates) {
            Box box = entity.getBoundingBox().expand(0.05);
            EntityBoxHit hit = raycastBox(box, start, end);
            if (hit != null) {
                if (hit.distanceSq < closestDistance) {
                    closestDistance = hit.distanceSq;
                    closestHit = new EntityHitResult(entity, hit.pos, hit.normal, hit.distanceSq);
                }
            }
        }

        return closestHit;
    }

    private record EntityBoxHit(Vec3d pos, Vec3d normal, double distanceSq) {}

    /**
     * Точный Raycast по 6 граням AABB-коробки.
     */
    private EntityBoxHit raycastBox(Box box, Vec3d start, Vec3d end) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;

        double minT = Double.MAX_VALUE;
        Vec3d bestNormal = null;
        Vec3d bestPos = null;

        if (Math.abs(dx) > 1e-7) {
            double t = (box.minX - start.x) / dx;
            if (t >= 0 && t <= 1 && t < minT) {
                double y = start.y + t * dy;
                double z = start.z + t * dz;
                if (y >= box.minY && y <= box.maxY && z >= box.minZ && z <= box.maxZ) {
                    minT = t;
                    bestNormal = new Vec3d(-1, 0, 0);
                    bestPos = new Vec3d(box.minX, y, z);
                }
            }
            t = (box.maxX - start.x) / dx;
            if (t >= 0 && t <= 1 && t < minT) {
                double y = start.y + t * dy;
                double z = start.z + t * dz;
                if (y >= box.minY && y <= box.maxY && z >= box.minZ && z <= box.maxZ) {
                    minT = t;
                    bestNormal = new Vec3d(1, 0, 0);
                    bestPos = new Vec3d(box.maxX, y, z);
                }
            }
        }

        if (Math.abs(dy) > 1e-7) {
            double t = (box.minY - start.y) / dy;
            if (t >= 0 && t <= 1 && t < minT) {
                double x = start.x + t * dx;
                double z = start.z + t * dz;
                if (x >= box.minX && x <= box.maxX && z >= box.minZ && z <= box.maxZ) {
                    minT = t;
                    bestNormal = new Vec3d(0, -1, 0);
                    bestPos = new Vec3d(x, box.minY, z);
                }
            }
            t = (box.maxY - start.y) / dy;
            if (t >= 0 && t <= 1 && t < minT) {
                double x = start.x + t * dx;
                double z = start.z + t * dz;
                if (x >= box.minX && x <= box.maxX && z >= box.minZ && z <= box.maxZ) {
                    minT = t;
                    bestNormal = new Vec3d(0, 1, 0);
                    bestPos = new Vec3d(x, box.maxY, z);
                }
            }
        }

        if (Math.abs(dz) > 1e-7) {
            double t = (box.minZ - start.z) / dz;
            if (t >= 0 && t <= 1 && t < minT) {
                double x = start.x + t * dx;
                double y = start.y + t * dy;
                if (x >= box.minX && x <= box.maxX && y >= box.minY && y <= box.maxY) {
                    minT = t;
                    bestNormal = new Vec3d(0, 0, -1);
                    bestPos = new Vec3d(x, y, box.minZ);
                }
            }
            t = (box.maxZ - start.z) / dz;
            if (t >= 0 && t <= 1 && t < minT) {
                double x = start.x + t * dx;
                double y = start.y + t * dy;
                if (x >= box.minX && x <= box.maxX && y >= box.minY && y <= box.maxY) {
                    minT = t;
                    bestNormal = new Vec3d(0, 0, 1);
                    bestPos = new Vec3d(x, y, box.maxZ);
                }
            }
        }

        if (bestPos != null) {
            return new EntityBoxHit(bestPos, bestNormal, start.squaredDistanceTo(bestPos));
        }
        return null;
    }

    private Vec3d initialVelocity(Angle angle, ItemStack held, Vec3d pVel) {
        float speed = launchSpeed(held);
        double pitch = Math.toRadians(-angle.getPitch()), yaw = Math.toRadians(angle.getYaw() + 90.0);
        return new Vec3d(Math.cos(pitch) * Math.cos(yaw) * speed, Math.sin(pitch) * speed, Math.cos(pitch) * Math.sin(yaw) * speed).add(pVel.x, mc.player.isOnGround() ? 0 : pVel.y, pVel.z);
    }

    private Vec3d throwableInitialVelocity(Angle angle, ItemStack held, Vec3d pVel) {
        float speed = throwableSpeed(held);
        double pitch = Math.toRadians(-angle.getPitch()), yaw = Math.toRadians(angle.getYaw() + 90.0);
        return new Vec3d(Math.cos(pitch) * Math.cos(yaw) * speed, Math.sin(pitch) * speed, Math.cos(pitch) * Math.sin(yaw) * speed).add(pVel);
    }

    private float launchSpeed(ItemStack s) {
        if (s.isOf(Items.TRIDENT)) return 2.5f;
        if (s.isOf(Items.CROSSBOW)) return 3.15f;
        if (s.isOf(Items.BOW)) {
            int used = s.getMaxUseTime(mc.player) - mc.player.getItemUseTimeLeft();
            float f = used / 20f;
            return MathHelper.clamp((f * f + f * 2f) / 3f, 0f, 1f) * 3f;
        }
        return 3f;
    }

    private float throwableSpeed(ItemStack s) {
        if (s.isOf(Items.ENDER_PEARL)) return 2.4f;
        if (s.isOf(Items.SPLASH_POTION) || s.isOf(Items.LINGERING_POTION)) return 0.5f;
        return 1.5f;
    }

    private void drawSmoothLine(MatrixStack stack, VertexConsumerProvider.Immediate imm, List<Vec3d> pts, Color c) {
        if (pts.size() < 2) return;
        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE);
        MatrixStack.Entry entry = stack.peek();
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        for (int i = 0; i < pts.size() - 1; i++) {
            float alpha = (float) (c.getAlpha() * (1.0 - (double) i / pts.size()));
            buf.vertex(entry, (float) pts.get(i).x, (float) pts.get(i).y, (float) pts.get(i).z).color(r, g, b, (int) alpha).normal(entry, 0, 1, 0);
            buf.vertex(entry, (float) pts.get(i + 1).x, (float) pts.get(i + 1).y, (float) pts.get(i + 1).z).color(r, g, b, (int) alpha).normal(entry, 0, 1, 0);
        }
    }

    private void drawMarker(MatrixStack stack, VertexConsumerProvider.Immediate imm, Vec3d pos, Vec3d normal, Color c, float size) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        MatrixStack.Entry entry = stack.peek();

        Vec3d rp = pos.add(normal.multiply(0.01));

        Vec3d tan = Math.abs(normal.y) > 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d v1 = normal.crossProduct(tan).normalize().multiply(size);
        Vec3d v2 = normal.crossProduct(v1).normalize().multiply(size);

        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE_NO);

        int segments = 4;
        for (int i = 0; i <= segments; i++) {
            double angle = i * Math.PI * 2 / segments;
            float x = (float) (rp.x + Math.cos(angle) * v1.x + Math.sin(angle) * v2.x);
            float y = (float) (rp.y + Math.cos(angle) * v1.y + Math.sin(angle) * v2.y);
            float z = (float) (rp.z + Math.cos(angle) * v1.z + Math.sin(angle) * v2.z);

            buf.vertex(entry, x, y, z)
                    .color(r, g, b, a)
                    .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
        }
    }

    private void drawRadiusCircle(MatrixStack stack, VertexConsumerProvider.Immediate imm, Vec3d pos, float radius, Color c) {
        MatrixStack.Entry entry = stack.peek();
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha();
        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE);
        for (int i = 0; i < 48; i++) {
            double a1 = i * Math.PI * 2 / 48, a2 = (i + 1) * Math.PI * 2 / 48;
            buf.vertex(entry, (float)(pos.x + Math.cos(a1)*radius), (float)pos.y + 0.05f, (float)(pos.z + Math.sin(a1)*radius)).color(r, g, b, a).normal(entry, 0, 1, 0);
            buf.vertex(entry, (float)(pos.x + Math.cos(a2)*radius), (float)pos.y + 0.05f, (float)(pos.z + Math.sin(a2)*radius)).color(r, g, b, a).normal(entry, 0, 1, 0);
        }
    }

    private void drawRadiusDisc(MatrixStack stack, VertexConsumerProvider.Immediate imm, Vec3d pos, float radius, Color c) {
        MatrixStack.Entry entry = stack.peek();
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue(), a = c.getAlpha() / 4;
        VertexConsumer buf = imm.getBuffer(ClientPipelines.OUTLINE);
        for (int i = 0; i < 48; i++) {
            double a1 = i * Math.PI * 2 / 48;
            buf.vertex(entry, (float)pos.x, (float)pos.y + 0.04f, (float)pos.z).color(r, g, b, a).normal(entry, 0, 1, 0);
            buf.vertex(entry, (float)(pos.x + Math.cos(a1)*radius), (float)pos.y + 0.04f, (float)(pos.z + Math.sin(a1)*radius)).color(r, g, b, a).normal(entry, 0, 1, 0);
        }
    }

    private void renderLandingInfo(DrawContext ctx, SimResult sim, ItemStack held) {
        Vec3d screen = MathUtility.worldSpaceToScreenSpace(sim.landing);
        if (screen == null || screen.z <= 0 || screen.z >= 1) return;

        String name = getItemName(held);
        if (sim.hitEntity && sim.targetEntity != null) {
            name = sim.targetEntity.getName().getString();
        }

        float textSize = 7f;
        float iconSize = textSize + 4f;
        float nameW = Client.RENDERER.textWidth(name, TextureUse.SFMEDIUM, textSize);
        float timeW = Client.RENDERER.textWidth(" " , TextureUse.SFMEDIUM, textSize);
        float w = iconSize + nameW + timeW + 10f;
        float h = 12f;

        float x = (float) screen.x - w / 2f;
        float y = (float) screen.y - h / 2f;

        Color bg = sim.hitEntity ? new Color(180, 30, 30, 180) : new Color(0, 0, 0, 120);
        Client.RENDERER.rect(x, y, w, h, new Vector4f(2), 1f, bg, bg, bg, bg);

        var mat = ctx.getMatrices();
        mat.pushMatrix();
        mat.translate(x, y);
        mat.scale(iconSize / 16f, iconSize / 16f);
        ctx.drawItem(mc.player, held, 0, 0, 0);
        mat.popMatrix();

        Client.RENDERER.text(name, x + 11, y + 2, TextureUse.SFMEDIUM, textSize, Color.WHITE);
    }

    private boolean isEnabledWeapon(ItemStack s) { return (s.isOf(Items.TRIDENT) && weapons.get(Weapon.TRIDENT)) || (s.isOf(Items.BOW) && weapons.get(Weapon.BOW)) || (s.isOf(Items.CROSSBOW) && weapons.get(Weapon.CROSSBOW)); }
    private boolean isEnabledThrowable(ItemStack s) { return (s.isOf(Items.SNOWBALL) && throwables.get(Throwable.SNOWBALL)) || (s.isOf(Items.EGG) && throwables.get(Throwable.EGG)) || (s.isOf(Items.ENDER_PEARL) && throwables.get(Throwable.ENDER_PEARL)) || (s.isOf(Items.SPLASH_POTION) && throwables.get(Throwable.SPLASH_POTION)) || (s.isOf(Items.LINGERING_POTION) && throwables.get(Throwable.LINGERING_POTION)); }

    private boolean isCharging(ItemStack s) {
        if (s.isEmpty()) return false;

        if (s.isOf(Items.BOW) || s.isOf(Items.TRIDENT)) {
            return mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND;
        }

        if (s.isOf(Items.CROSSBOW)) {
            return CrossbowItem.isCharged(s) && mc.options.useKey.isPressed();
        }

        return false;
    }

    private String getItemName(ItemStack s) { return s.getItem().getName().getString(); }

    private Angle calcProjectileAngle(Vec3d origin, Vec3d targetPos, ItemStack held) {
        float speed = launchSpeed(held); if (speed < 0.1f) return null;
        Vec3d diff = targetPos.subtract(origin);
        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        Angle base = RotationUtility.calcRotate(targetPos, true);
        float lo = -89f, hi = 89f, best = Float.NaN;
        for (int i = 0; i < 64; i++) {
            float mid = (lo + hi) / 2f;
            if (simulateY(mid, speed, distXZ) >= diff.y) { best = mid; hi = mid; } else lo = mid;
        }
        return Float.isNaN(best) ? base : new Angle(base.getYaw(), -best);
    }

    private double simulateY(float elev, float speed, double tXZ) {
        double vxz = Math.cos(Math.toRadians(elev)) * speed, vy = Math.sin(Math.toRadians(elev)) * speed, xz = 0, y = 0;
        for (int i = 0; i < 1000; i++) {
            vxz *= 0.99; vy *= 0.99; vy -= 0.05;
            xz += vxz; y += vy;
            if (xz >= tXZ) return y;
        }
        return -999;
    }

    private Vec3d predictTargetPos(LivingEntity target, Vec3d origin, ItemStack held) {
        Vec3d pos = target.getEyePos(), vel = target.getVelocity();
        double dist = pos.distanceTo(origin);
        int ticks = (int) (dist / launchSpeed(held));
        return pos.add(vel.multiply(ticks * predictTicks.get()));
    }

    private boolean hasMultishot(ItemStack stack) {
        if (stack == null || !stack.isOf(Items.CROSSBOW) || mc.world == null) return false;
        try {
            var registry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            var entry = registry.getEntry(Enchantments.MULTISHOT.getValue()).orElse(null);
            if (entry != null) {
                return EnchantmentHelper.getLevel(entry, stack) > 0;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static class SimResult {
        final List<Vec3d> points;
        final Vec3d landing, landingNormal;
        final int ticksToLand;
        final boolean hitEntity;
        final Entity targetEntity;

        SimResult(List<Vec3d> p, Vec3d l, Vec3d n, int t, boolean hitEntity, Entity targetEntity) {
            this.points = p;
            this.landing = l;
            this.landingNormal = n;
            this.ticksToLand = t;
            this.hitEntity = hitEntity;
            this.targetEntity = targetEntity;
        }
    }
}