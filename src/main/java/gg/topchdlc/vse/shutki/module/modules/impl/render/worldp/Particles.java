package gg.topchdlc.vse.shutki.module.modules.impl.render.worldp;

import gg.topchdlc.api.events.list.EventReceivePacket;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.other.StopWatch;
import gg.topchdlc.vse.utils.player.PlayerUtility;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Create by daun kvass
 */
public class Particles extends Module {

    public enum ParticleType {
        BLOOM("bloom"), STAR("star"), SNOW("snowflake"), HEART("heart"), DOLLAR("dollar"), TRIANGLE("triangle"), GENSHIN("genshin"), RHOMBUS("rhombus"),CROSS("cross");
        final Identifier texture; ParticleType(String name) { this.texture = Identifier.of("topchdlc", "images/world/pt/" + name + ".png"); }
    }

    public enum SpawnEvent { Attack("Атаке"), Throw("Бросок"), World("В мире"), Totem("Тотем");
        private final String name; SpawnEvent(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }
    public enum TypeMode { Single("Один"), Mix("Микс"), Random("Рандом");
        private final String name; TypeMode(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }
    public enum ParticleMode { BLOOM, STAR, SNOW, HEART, DOLLAR, TRIANGLE, GENSHIN, RHOMBUS }
    public enum ColorMode { Theme("От темы"), Random("Рандом");
        private final String name; ColorMode(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }
    public enum PhysicsMode { Boom("Взрыв"), Smooth("Зависание");
        private final String name; PhysicsMode(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }
    public enum WorldPhysics { Snowfall("Снегопад"), Hail("Град"), Vortex("Вихрь"), Wind("Ветер"), Fly("Воздух");
        private final String name; WorldPhysics(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }
    public enum WindDirection { Forward("От себя"), Backward("На себя"), Left("Влево"), Right("Вправо");
        private final String name; WindDirection(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }

    private static final ParticleType[] ALL_TYPES = ParticleType.values();
    private static final RenderLayer[] PARTICLE_RENDER_LAYERS = new RenderLayer[ALL_TYPES.length];
    static { for (ParticleType type : ALL_TYPES) PARTICLE_RENDER_LAYERS[type.ordinal()] = ClientPipelines.PARTICLES.apply(type.texture); }

    public static final Particles INSTANCE = new Particles();

    private final MultiEnumSetting<SpawnEvent> events = multiEnumSetting("Спавнить при", SpawnEvent.Attack, SpawnEvent.Throw);
    private final EnumSetting<TypeMode> typeMode = enumSetting("Режим типа", TypeMode.Single);
    private final EnumSetting<ParticleMode> particleMode = enumSetting("Тип", ParticleMode.BLOOM).visible(() -> typeMode.get() == TypeMode.Single);
    private final MultiEnumSetting<ParticleMode> mixTypes = multiEnumSetting("Микс типов", ParticleMode.BLOOM, ParticleMode.STAR).visible(() -> typeMode.get() == TypeMode.Mix);
    private final EnumSetting<ColorMode> colorMode = enumSetting("Цвет", ColorMode.Theme);
    private final SliderSetting size = sliderSetting("Размер", 0.5f, 0.0f, 1.0f).increment(0.05f);

    private final Group attackGroup = group("Атака").visible(() -> events.get(SpawnEvent.Attack));
    private final SliderSetting attackCount    = attackGroup.sliderSetting("Кол-во", 12, 3, 50);
    private final SliderSetting attackLifespan = attackGroup.sliderSetting("Время жизни", 4000, 1000, 10000);
    private final EnumSetting<PhysicsMode> attackPhysics = attackGroup.enumSetting("Физика", PhysicsMode.Boom);
    private final CheckBox attackCritEffect    = attackGroup.checkbox("Крит эффект", true);
    private final Group throwGroup = group("Бросок").visible(() -> events.get(SpawnEvent.Throw));
    private final SliderSetting throwCount    = throwGroup.sliderSetting("Кол-во", 5, 1, 15);
    private final SliderSetting throwLifespan = throwGroup.sliderSetting("Время жизни", 3000, 500, 6000);

    private final Group worldGroup = group("Мир").visible(() -> events.get(SpawnEvent.World));
    private final SliderSetting worldCount    = worldGroup.sliderSetting("Кол-во", 10, 1, 60);
    private final SliderSetting worldLifespan = worldGroup.sliderSetting("Время жизни", 10000, 2000, 20000);
    private final EnumSetting<WorldPhysics> worldPhys = worldGroup.enumSetting("Режим", WorldPhysics.Snowfall);
    private final EnumSetting<WindDirection> windDir = worldGroup.enumSetting("Направление", WindDirection.Forward).visible(() -> worldPhys.get() == WorldPhysics.Wind);

    private final Group totemGroup = group("Тотем").visible(() -> events.get(SpawnEvent.Totem));
    private final SliderSetting totemCount        = totemGroup.sliderSetting("Кол-во", 60, 15, 200);
    private final SliderSetting totemLifespan     = totemGroup.sliderSetting("Время жизни", 6000, 2000, 12000);

    private final List<Particle> targetParticles = new ArrayList<>();
    private final List<Particle> worldParticles  = new ArrayList<>();
    private final List<Particle> flameParticles  = new ArrayList<>();
    private final List<Particle> totemParticles  = new ArrayList<>();

    private final List<ParticleType> mixCache = new ArrayList<>();
    private long lastUpdateTime = System.nanoTime();
    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate imm;
    private final ArrayDeque<Particle> particlePool = new ArrayDeque<>(4000);
    private final List<Particle>[] renderBuckets = new List[ALL_TYPES.length];

    private Particles() {
        super("Particles", Category.RENDER, "Красивые частицы в мире");
        for (int i = 0; i < renderBuckets.length; i++) renderBuckets[i] = new ArrayList<>(1024);
    }

    private Particle obtainParticle(ParticleType type, double px, double py, double pz, double vx, double vy, double vz, int rotate, int color, float particleSize, long lifespan, double gravity, boolean isSmooth, boolean collision) {
        Particle p = particlePool.pollFirst();
        if (p != null) { p.reset(type, px, py, pz, vx, vy, vz, rotate, color, particleSize, lifespan, gravity, isSmooth, collision); return p; }
        return new Particle(type, px, py, pz, vx, vy, vz, rotate, color, particleSize, lifespan, gravity, isSmooth, collision);
    }
    private void recycleParticle(Particle p) { if (particlePool.size() < 4000) particlePool.addLast(p); }

    @Override public void toggle() { super.toggle(); clear(); if (!isEnabled() && allocator != null) { allocator.close(); allocator = null; imm = null; } }

    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) onTick();
        else if (event instanceof Event3D e) onRender3D(e);
        else if (event instanceof EventAttack a) onAttack(a);
        else if (event instanceof EventReceivePacket pkt) onPacket(pkt);
    };

    private void updateMixCache() {
        mixCache.clear();
        for (ParticleMode mode : ParticleMode.values()) if (mixTypes.get(mode)) mixCache.add(ALL_TYPES[mode.ordinal()]);
        if (mixCache.isEmpty()) mixCache.add(ParticleType.BLOOM);
    }

    private ParticleType getRandType() {
        TypeMode mode = typeMode.get();
        if (mode == TypeMode.Random) return ALL_TYPES[ThreadLocalRandom.current().nextInt(ALL_TYPES.length)];
        if (mode == TypeMode.Mix) return mixCache.get(ThreadLocalRandom.current().nextInt(mixCache.size()));
        return ALL_TYPES[particleMode.get().ordinal()];
    }

    private void onPacket(EventReceivePacket pkt) {
        if (mc.world == null || !events.get(SpawnEvent.Totem)) return;
        if (pkt.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
            Entity entity = status.getEntity(mc.world);
            if (entity instanceof LivingEntity living) spawnTotemExplosion(living);
        }
    }

    private void onAttack(EventAttack attack) {
        if (!events.get(SpawnEvent.Attack) || mc.player == null || !(attack.target instanceof LivingEntity target)) return;
        boolean isCrit = attackCritEffect.get() && mc.player.fallDistance > 0 && !mc.player.isOnGround();
        int count = (int) (attackCount.getInt() * (isCrit ? 1.5 : 1.0));

        PhysicsMode mode = attackPhysics.get();

        float force = (mode == PhysicsMode.Boom) ? 7.0f : 0.4f;
        double gravity = (mode == PhysicsMode.Boom) ? 0.005 : 0.0;

        for (int i = 0; i < count; i++) {
            double t = rngD(0, Math.PI * 2), p = Math.acos(rngD(-1, 1)), f = force * rngD(0.7, 1.3);

            double vx = f * Math.sin(p) * Math.cos(t);
            double vy = (f * Math.cos(p)) * 0.5 + (f * 0.4);
            double vz = f * Math.sin(p) * Math.sin(t);

            spawnParticleRaw(targetParticles, target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ(),
                    vx, vy, vz, 1.0f, getParticleColor(i), attackLifespan.getInt(),
                    gravity, mode == PhysicsMode.Smooth, true);
        }
    }

    private void spawnTotemExplosion(LivingEntity entity) {
        float force = 6.0f;
        for (int i = 0; i < totemCount.getInt(); i++) {
            double t = rngD(0, Math.PI * 2), p = Math.acos(rngD(-1, 1)), f = force * rngD(0.5, 1.5);
            double vx = f * Math.sin(p) * Math.cos(t), vy = f * Math.cos(p), vz = f * Math.sin(p) * Math.sin(t);
            spawnParticleRaw(totemParticles, entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ(), vx, vy, vz, 1.4f, getTotemColor(), totemLifespan.getInt(), 0, true, true);
        }
    }

    private void spawnParticleRaw(List<Particle> list, double px, double py, double pz, double vx, double vy, double vz, float sizeMul, int color, long lifespan, double gravity, boolean isSmooth, boolean collision) {
        synchronized (list) {
            float pSize = (0.05F + size.get() * 0.2F) * sizeMul;
            list.add(obtainParticle(getRandType(), px, py, pz, vx, vy, vz, (int)rng(0,360), color, pSize, lifespan, gravity, isSmooth, collision));
        }
    }

    private void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (typeMode.get() == TypeMode.Mix) updateMixCache();

        if (events.get(SpawnEvent.Throw)) {
            for (Entity e : mc.world.getEntities()) {
                if ((e instanceof EnderPearlEntity || e instanceof ArrowEntity || e instanceof TridentEntity) && (e.getX() != e.lastX)) {
                    for (int i = 0; i < throwCount.getInt(); i++) {
                        double f = 0.3;
                        spawnParticleRaw(flameParticles, e.getX(), e.getY(), e.getZ(), rngD(-f, f), rngD(-f, f), rngD(-f, f), 1.0f, getParticleColor(i), throwLifespan.getInt(), 0, true, true);
                    }
                }
            }
        }
        if (events.get(SpawnEvent.World)) {
            WorldPhysics phys = worldPhys.get();
            int rad = 60;
            for (int i = 0; i < worldCount.getInt(); i++) {
                double vx = 0, vy = 0, vz = 0, grav = 0.001; boolean coll = true;
                double px = mc.player.getX() + rng(-rad, rad), pz = mc.player.getZ() + rng(-rad, rad), py = mc.player.getY() + rng(5, 40);

                switch (phys) {
                    case Snowfall -> { vy = -0.06; vx = rngD(-0.05, 0.05); vz = rngD(-0.05, 0.05); }
                    case Hail -> { py = mc.player.getY() + 30; vy = -0.5; grav = 0.008; }
                    case Vortex -> {
                        double ang = rngD(0, Math.PI * 2), dist = rngD(5, rad - 5);
                        px = mc.player.getX() + Math.cos(ang) * dist; pz = mc.player.getZ() + Math.sin(ang) * dist;
                        py = mc.player.getY() - 3.0; vy = rngD(0.2, 0.5); grav = -0.003; coll = false;
                    }
                    case Wind -> {
                        float yaw = (float) Math.toRadians(mc.player.getYaw());
                        double speed = 1.5;
                        double dx = Math.sin(yaw), dz = -Math.cos(yaw), sideOff = rng(-rad, rad);
                        switch(windDir.get()) {
                            case Forward -> { px = mc.player.getX() - dx * 65 + dz * sideOff; pz = mc.player.getZ() - dz * 65 - dx * sideOff; vx = dx * speed; vz = dz * speed; }
                            case Backward -> { px = mc.player.getX() + dx * 65 + dz * sideOff; pz = mc.player.getZ() + dz * 65 - dx * sideOff; vx = -dx * speed; vz = -dz * speed; }
                            case Left -> { px = mc.player.getX() - dz * 65 + dx * sideOff; pz = mc.player.getZ() + dx * 65 + dz * sideOff; vx = dz * speed; vz = -dx * speed; }
                            case Right -> { px = mc.player.getX() + dz * 65 + dx * sideOff; pz = mc.player.getZ() - dx * 65 + dz * sideOff; vx = -dz * speed; vz = dx * speed; }
                        }
                        py = mc.player.getY() + rng(-rad/2, rad); grav = 0; coll = false;
                    }
                    case Fly -> { py = mc.player.getY() + rng(-20, 35); grav = 0; coll = false; }
                }
                spawnParticleRaw(worldParticles, px, py, pz, vx, vy, vz, 1.0f, getParticleColor(i), worldLifespan.getInt(), grav, (phys == WorldPhysics.Wind || phys == WorldPhysics.Fly), coll);
            }
        }
        recycleExpired(targetParticles); recycleExpired(flameParticles); recycleExpired(worldParticles); recycleExpired(totemParticles);
    }

    private int getTotemColor() { return rng(0, 1) > 0.5f ? Color.HSBtoRGB(0.12f, 0.8f, 1.0f) : Color.HSBtoRGB(0.25f, 0.9f, 1.0f); }
    private int getParticleColor(int index) { return colorMode.get() == ColorMode.Random ? Color.HSBtoRGB(rng(0, 1), 0.7f, 1.0f) : ClientSettings.INSTANCE.getColor(index * 100).getRGB(); }

    private void recycleExpired(List<Particle> list) {
        synchronized (list) { list.removeIf(p -> { if (p.time.finished(p.lifespan)) { recycleParticle(p); return true; } return false; }); }
    }

    private void onRender3D(Event3D event) {
        if (allocator == null) { allocator = new BufferAllocator(2097152); imm = VertexConsumerProvider.immediate(allocator); }
        MatrixStack matrix = event.stack;
        double dt = (System.nanoTime() - lastUpdateTime) / 1.0E9;
        lastUpdateTime = System.nanoTime();
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

        for (List<Particle> bucket : renderBuckets) bucket.clear();
        fillBuckets(targetParticles, dt); fillBuckets(flameParticles, dt); fillBuckets(worldParticles, dt); fillBuckets(totemParticles, dt);

        for (int i = 0; i < ALL_TYPES.length; i++) {
            List<Particle> bucket = renderBuckets[i];
            if (bucket.isEmpty()) continue;
            VertexConsumer buf = imm.getBuffer(PARTICLE_RENDER_LAYERS[i]);
            for (Particle p : bucket) {
                float alpha = p.getAlpha(); float scale = p.getScale();
                double dx = p.px - cam.x, dy = p.py - cam.y, dz = p.pz - cam.z;
                if (alpha <= 0.01f || dx * dx + dy * dy + dz * dz > 28900) continue;

                matrix.push();
                Client.RENDERER.setupOrientationMatrix(matrix, (float) p.px, (float) p.py, (float) p.pz);
                matrix.multiply(mc.gameRenderer.getCamera().getRotation());
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rotate + p.time.elapsedTime() * p.rotationSpeed));
                Matrix4f mat = matrix.peek().getPositionMatrix();
                float s = p.size * scale;
                int cr = (p.color >> 16) & 0xFF, cg = (p.color >> 8) & 0xFF, cb = p.color & 0xFF, a = (int)(alpha * 255);
                buf.vertex(mat, -s, -s, 0).texture(0, 1).color(cr, cg, cb, a);
                buf.vertex(mat, s, -s, 0).texture(1, 1).color(cr, cg, cb, a);
                buf.vertex(mat, s, s, 0).texture(1, 0).color(cr, cg, cb, a);
                buf.vertex(mat, -s, s, 0).texture(0, 0).color(cr, cg, cb, a);
                matrix.pop();
            }
        }
        imm.draw();
    }

    private void fillBuckets(List<Particle> list, double dt) {
        synchronized (list) { for (Particle p : list) { p.update(dt); renderBuckets[p.type.ordinal()].add(p); } }
    }

    private void clear() {
        targetParticles.forEach(this::recycleParticle); flameParticles.forEach(this::recycleParticle);
        worldParticles.forEach(this::recycleParticle); totemParticles.forEach(this::recycleParticle);
        targetParticles.clear(); flameParticles.clear(); worldParticles.clear(); totemParticles.clear();
    }

    private static float rng(float min, float max) { return min + ThreadLocalRandom.current().nextFloat() * (max - min); }
    private static double rngD(double min, double max) { return min + ThreadLocalRandom.current().nextDouble() * (max - min); }

    static class Particle {
        ParticleType type; double px, py, pz, vx, vy, vz, gravity; int rotate, color; float size, rotationSpeed; long lifespan; boolean isSmooth, collision; final StopWatch time = new StopWatch();
        boolean grounded = false;

        Particle(ParticleType type, double px, double py, double pz, double vx, double vy, double vz, int rotate, int color, float size, long lifespan, double gravity, boolean isSmooth, boolean collision) {
            reset(type, px, py, pz, vx, vy, vz, rotate, color, size, lifespan, gravity, isSmooth, collision);
        }

        void reset(ParticleType type, double px, double py, double pz, double vx, double vy, double vz, int rotate, int color, float size, long lifespan, double gravity, boolean isSmooth, boolean collision) {
            this.type = type; this.px = px; this.py = py; this.pz = pz;
            this.vx = vx * 0.1; this.vy = vy * 0.1; this.vz = vz * 0.1;
            this.rotate = rotate; this.color = color; this.size = size; this.lifespan = lifespan; this.gravity = -gravity;
            this.isSmooth = isSmooth; this.collision = collision; this.rotationSpeed = (rng(-1, 1)) * 0.15f;
            this.time.reset(); this.grounded = false;
        }

        float getScale() {
            float p = (float) time.elapsedTime() / lifespan;
            return p > 0.85f ? (1.0f - (p - 0.85f) / 0.15f) : 1.0f;
        }

        float getAlpha() {
            float p = (float) time.elapsedTime() / lifespan;
            return p > 0.7f ? (1.0f - (p - 0.7f) / 0.3f) : 1.0f;
        }

        void update(double dt) {
            if (grounded) return;
            double dm = dt * 60;
            vy += gravity * dm;

            double friction = (gravity == 0) ? 0.999 : (isSmooth ? 0.89 : 0.95);
            vx *= friction; vy *= friction; vz *= friction;

            px += vx * dm; py += vy * dm; pz += vz * dm;

            if (collision && PlayerUtility.isBlockSolid(px, py, pz)) {
                if (gravity < -0.003) {
                    vy = Math.abs(vy) * 0.5; vx *= 0.6; vz *= 0.6;
                    if (Math.abs(vy) < 0.01) { grounded = true; lifespan = Math.min(lifespan, time.elapsedTime() + 2500); }
                } else { vx = 0; vy = 0; vz = 0; grounded = true; }
            }
        }
    }
}