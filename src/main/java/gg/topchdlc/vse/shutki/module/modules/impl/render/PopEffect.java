package gg.topchdlc.vse.shutki.module.modules.impl.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventChangeWorld;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PopEffect extends Module {
    public static final PopEffect INSTANCE = new PopEffect();

    public enum Mode {
        Lightning("Молния"),
        Beam("Луч"),
        Soul("Душа");

        private final String name;
        Mode(String n) { this.name = n; }
        @Override public String toString() { return name; }
    }
    private final Group rendergroup = group("render");
    private final Group sosiskigroup = group("from");
    private final EnumSetting<Mode> mode = rendergroup.enumSetting("Режим", Mode.Lightning);
    private final CheckBox onKill = sosiskigroup.checkbox("При убийстве", true);
    private final CheckBox theme = rendergroup.checkbox("Цвет от темы",true);
    private final ColorSetting killColor = rendergroup.colorSetting("Цвет убийства", new Color(0x63FFFF))
            .visible(()-> onKill.get()&& !theme.get());
    private final CheckBox onTotem = sosiskigroup.checkbox("При тотеме", true);
    private final ColorSetting totemColor =rendergroup.colorSetting("Цвет тотема", new Color(0xAAFF44))
            .visible(()->onTotem.get()&&!theme.get());
    private final SliderSetting soulAlpha = rendergroup.sliderSetting("Прозрачность", 0.8f, 0.1f, 1.0f)
            .increment(0.05f)
            .visible(() -> mode.is(Mode.Soul));
   /* private final Group soulEspGroup          = group("SoulESP");
    private final CheckBox soulEspEnabled     = soulEspGroup.checkbox("Включить", true);
    private final SliderSetting soulEspDuration  = soulEspGroup.sliderSetting("Длительность", 3.0f, 1.0f, 8.0f)
            .increment(0.5f).visible(soulEspEnabled::get);
    private final SliderSetting soulEspHeight    = soulEspGroup.sliderSetting("Высота", 3.5f, 1.0f, 8.0f)
            .increment(0.5f).visible(soulEspEnabled::get);
    private final CheckBox soulEspCustomColor    = soulEspGroup.checkbox("Custom color", false)
            .visible(soulEspEnabled::get);
    private final ColorSetting soulEspColor1     = soulEspGroup.colorSetting("Цвет 1", new Color(0x00FFFF))
            .visible(() -> soulEspEnabled.get() && soulEspCustomColor.get());
    private final ColorSetting soulEspColor2     = soulEspGroup.colorSetting("Цвет 2", new Color(0xFF00FF))
            .visible(() -> soulEspEnabled.get() && soulEspCustomColor.get());
    private final CheckBox soulEspOnDeath        = soulEspGroup.checkbox("При смерти", true)
            .visible(soulEspEnabled::get);
    private final CheckBox soulEspOnTotem        = soulEspGroup.checkbox("При тотеме", true)
            .visible(soulEspEnabled::get);

    private final java.util.List<GhostSoul> ghostSouls = new ArrayList<>();
    private BufferAllocator soulAllocator;
    private VertexConsumerProvider.Immediate soulImm;*/

    private final Group chamsGroup = group("Chams");
    public final CheckBox chamsEnabled    = chamsGroup.checkbox("Включить", true);
    public final CheckBox chamsOnKill     = chamsGroup.checkbox("При убийстве", true).visible(chamsEnabled::get);
    public final CheckBox chamsOnTotem    = chamsGroup.checkbox("При тотеме", true).visible(chamsEnabled::get);
    public final CheckBox chamsUseCustomColor = chamsGroup.checkbox("Свой цвет", false).visible(chamsEnabled::get);
    public final ColorSetting chamsColor  = chamsGroup.colorSetting("Цвет", new Color(0x63FFFF))
            .visible(() -> chamsEnabled.get() && chamsUseCustomColor.get());
    public final SliderSetting chamsLife  = chamsGroup.sliderSetting("Время (мс)", 1200, 300, 3000)
            .visible(chamsEnabled::get);
    public static final CopyOnWriteArrayList<ChamGhost> CHAMS_GHOSTS = new CopyOnWriteArrayList<>();

    public static class ChamGhost {
        public final int entityId;
        public final long createdAt;
        public final long lifeMs;
        public final int color;


        public ChamGhost(int entityId, long createdAt, long lifeMs, int color) {
            this.entityId  = entityId;
            this.createdAt = createdAt;
            this.lifeMs    = lifeMs;
            this.color     = color;
        }
        public boolean isFinished() { return System.currentTimeMillis() - createdAt >= lifeMs; }
        public float progress() { return Math.min(1f, (System.currentTimeMillis() - createdAt) / (float) lifeMs); }
        public float riseY() {
            return easeOutCubic(progress()) * 2.0f;
        }

        public float alpha() {
            return 1f - progress();
        }


        private float easeOutCubic(float t) { return 1f - (float) Math.pow(1 - t, 3); }

    }
    private static final Identifier BLOOM_TEXTURE = Identifier.of("topchdlc", "images/world/bloom.png");
    private static final long DEATH_DEDUP_MS = 250L;
    private static final long DEATH_CACHE_MS = 1500L;
    private static final long EFFECT_LIFE_MS = 1200L;
    private static final long SOUL_LIFE_MS = 1000L;
    private static final float SOUL_RISE = 1.25f;
    private final List<EffectEntry> effects = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Map<Integer, Long> recentDeaths = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, Float> entityHealthCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Random random = new java.util.Random();

    private PopEffect() {
        super("PopEffect", Category.RENDER, "да");
    }

    @Override
    protected void onDisable() {
        clearAll();
        //ghostSouls.clear();
       // if (soulAllocator != null) { soulAllocator.close(); soulAllocator = null; soulImm = null; }
    }
    EventBus<Event> bus = event -> {
        if (event instanceof EventChangeWorld) {
            clearAll();
       //     ghostSouls.clear();
        }
        if (event instanceof EventGameTick) {
            onTick();
        }
        if (event instanceof EventReceivePacket pkt) {
            onPacket(pkt);
        }
        if (event instanceof Event3D e) {
            onRender3D(e);
        }
    };

    private void onPacket(EventReceivePacket pkt) {
        if (mc.world == null) return;
        if (pkt.getPacket() instanceof EntityStatusS2CPacket status) {
            byte s = status.getStatus();
            boolean isTotemPkt = s == EntityStatuses.USE_TOTEM_OF_UNDYING;
            boolean isDeathPkt = s == 3;

            if (isTotemPkt) {
                Entity entity = status.getEntity(mc.world);
                if (entity instanceof LivingEntity living) {
                    Vec3d pos = living.getEntityPos().add(0, living.getHeight() * 0.5, 0);
                    long now = System.currentTimeMillis();
                    int effectColor = theme.get() ? ClientSettings.INSTANCE.getColor(0).getRGB() : totemColor.get().getRGB();
                    if (onTotem.get()) spawnEffect(pos, effectColor, now);
                    if (chamsEnabled.get() && chamsOnTotem.get()) spawnChamGhost(living, now);
                //    if (soulEspEnabled.get() && soulEspOnTotem.get() && entity instanceof PlayerEntity player && player != mc.player) {
                //        ghostSouls.add(new GhostSoul(player.getPos(), player.getBodyYaw(), player.isSneaking(), player.age));
                  //  }
                }
            }

            if (isDeathPkt /*&&  soulEspEnabled.get() && soulEspOnDeath.get()*/) {
                Entity entity = status.getEntity(mc.world);
                if (entity instanceof PlayerEntity player && player != mc.player) {
                    //ghostSouls.add(new GhostSoul(player.getPos(), player.getBodyYaw(), player.isSneaking(), player.age));
                }
            }
        }
    }

    private void onTick() {
        if (mc.world == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        cleanupRecentDeaths(now);
        CHAMS_GHOSTS.removeIf(ChamGhost::isFinished);

        if (!onKill.get() && !(chamsEnabled.get() && chamsOnKill.get())) return;

        for (LivingEntity entity : mc.world.getEntitiesByClass(LivingEntity.class,
                mc.player.getBoundingBox().expand(64), e -> e != mc.player)) {

            int id = entity.getId();
            float prevHealth = entityHealthCache.getOrDefault(id, entity.getHealth());
            float curHealth = entity.getHealth();

            if (curHealth <= 0f && prevHealth > 0f) {
                Long lastDeath = recentDeaths.put(id, now);
                if (lastDeath == null || now - lastDeath > DEATH_DEDUP_MS) {
                    Vec3d pos = entity.getEntityPos().add(0, entity.getHeight() * 0.5, 0);

                    if (onKill.get()) {
                        int effectColor = theme.get() ? ClientSettings.INSTANCE.getColor(0).getRGB() : killColor.get().getRGB();spawnEffect(pos, effectColor, now);
                    }
                    if (chamsEnabled.get() && chamsOnKill.get()) spawnChamGhost(entity, now);
                }
            }

            if (curHealth > 0f) {
                entityHealthCache.put(id, curHealth);
            } else {
                entityHealthCache.remove(id);
            }
        }

        entityHealthCache.entrySet().removeIf(e -> {
            var ent = mc.world.getEntityById(e.getKey());
            return ent == null;
        });
    }

    private void spawnEffect(Vec3d pos, int rgb, long now) {
        switch (mode.get()) {
            case Lightning -> effects.add(new LightningEffect(pos, rgb, random, now));
            case Beam      -> effects.add(new BeamEffect(pos, rgb, random, now));
            case Soul      -> effects.add(new SoulEffect(pos, rgb, now));
        }
    }

    private void spawnChamGhost(LivingEntity entity, long now) {
        int rgb = chamsUseCustomColor.get()
                ? chamsColor.get().getRGB()
                : ClientSettings.INSTANCE.getColor(0).getRGB();
        CHAMS_GHOSTS.add(new ChamGhost(entity.getId(), now, (long) chamsLife.get(), rgb));
    }
    private void onRender3D(Event3D event) {
        if (mc.world == null) { clearAll(); return; }

        long now = System.currentTimeMillis();
        effects.removeIf(e -> e.isFinished(now));
        if (!effects.isEmpty()) {
            MatrixStack stack = event.stack;
            BufferAllocator allocator = new BufferAllocator(1 << 20);
            VertexConsumerProvider.Immediate imm = VertexConsumerProvider.immediate(allocator);

            try {
                RenderLayer layer = ClientPipelines.PARTICLES.apply(BLOOM_TEXTURE);
                VertexConsumer buf = imm.getBuffer(layer);

                for (EffectEntry e : effects) {
                    float am = (e instanceof SoulEffect) ? soulAlpha.get() : 1f;
                    e.render(stack, buf, am, now);
                }

                imm.draw();
            } finally {
                allocator.close();
            }
        }

        /*if (soulEspEnabled.get() && !ghostSouls.isEmpty()) {
            float durMs = soulEspDuration.get() * 1000f;
            ghostSouls.removeIf(g -> (now - g.time) >= durMs);

            if (!ghostSouls.isEmpty()) {
                if (soulAllocator == null) {
                    soulAllocator = new BufferAllocator(1 << 20);
                    soulImm = VertexConsumerProvider.immediate(soulAllocator);
                }

                MatrixStack m = event.stack;
                Vec3d cam = mc.gameRenderer.getCamera().getPos();
                VertexConsumer buf = soulImm.getBuffer(ClientPipelines.BLOCK_ESP_FILL);

                for (GhostSoul g : ghostSouls) {
                    float t = (now - g.time) / durMs;
                    if (t >= 1f) continue;

                    float alpha = (1f - t) * 0.6f;
                    float rise  = soulEspHeight.get() * ghostSoulEase(t);

                    Color c1 = soulEspCustomColor.get() ? soulEspColor1.get() : ClientSettings.INSTANCE.getColor(0);
                    Color c2 = soulEspCustomColor.get() ? soulEspColor2.get() : ClientSettings.INSTANCE.getColor(180);
                    Color blended = ColorUtility.blend(c1, c2, t);

                    float r  = blended.getRed()   / 255f;
                    float gr = blended.getGreen() / 255f;
                    float b  = blended.getBlue()  / 255f;

                    m.push();
                    m.translate(g.pos.x - cam.x, g.pos.y - cam.y + rise, g.pos.z - cam.z);
                    m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - g.yaw));
                    m.scale(-1f, -1f, 1f);
                    m.translate(0, -1.5, 0);

                    if (g.sneak) {
                        m.translate(0, 0.2, 0);
                        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28f));
                    }

                    float u = 1f / 16f;
                    float swing = MathHelper.sin(g.phase * 0.6662f) * 0.6f;

                    ghostSoulBox(buf, m, -4*u, 0,    -2*u, 8*u, 12*u, 4*u, r, gr, b, alpha);
                    ghostSoulBox(buf, m, -4*u, -8*u, -4*u, 8*u,  8*u, 8*u, r, gr, b, alpha);

                    m.push();
                    m.translate(-6*u, 2*u, 0);
                    m.multiply(RotationAxis.POSITIVE_X.rotation(-swing));
                    m.translate(6*u, -2*u, 0);
                    ghostSoulBox(buf, m, -8*u, -2*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
                    m.pop();

                    m.push();
                    m.translate(6*u, 2*u, 0);
                    m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
                    m.translate(-6*u, -2*u, 0);
                    ghostSoulBox(buf, m, 4*u, -2*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
                    m.pop();

                    m.push();
                    m.translate(-2*u, 12*u, 0);
                    m.multiply(RotationAxis.POSITIVE_X.rotation(swing));
                    m.translate(2*u, -12*u, 0);
                    ghostSoulBox(buf, m, -4*u, 12*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
                    m.pop();

                    m.push();
                    m.translate(2*u, 12*u, 0);
                    m.multiply(RotationAxis.POSITIVE_X.rotation(-swing));
                    m.translate(-2*u, -12*u, 0);
                    ghostSoulBox(buf, m, 0, 12*u, -2*u, 4*u, 12*u, 4*u, r, gr, b, alpha);
                    m.pop();

                    m.pop();
                }

                soulImm.draw();
            }
        }*/
    }

    private void ghostSoulBox(VertexConsumer buf, MatrixStack m,
                               float x, float y, float z,
                               float sx, float sy, float sz,
                               float r, float g, float b, float a) {
        Matrix4f mat = m.peek().getPositionMatrix();
        float x2 = x + sx, y2 = y + sy, z2 = z + sz;
        int ri = (int)(r*255), gi = (int)(g*255), bi = (int)(b*255), ai = (int)(a*255);
        buf.vertex(mat, x,  y,  z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y,  z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y,  z).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y,  z).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x, y,  z).color(ri, gi, bi, ai);
        buf.vertex(mat, x, y,  z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x, y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x, y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y,  z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y,  z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y2, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y, z).color(ri, gi, bi, ai);
        buf.vertex(mat, x2, y, z2).color(ri, gi, bi, ai);
        buf.vertex(mat, x,  y, z2).color(ri, gi, bi, ai);
    }

    private float ghostSoulEase(float t) {
        return 1f - (float) Math.pow(1f - MathHelper.clamp(t, 0f, 1f), 3);
    }

    private static class GhostSoul {
        final Vec3d pos;
        final float yaw;
        final boolean sneak;
        final float phase;
        final long time = System.currentTimeMillis();

        GhostSoul(Vec3d pos, float yaw, boolean sneak, float phase) {
            this.pos   = pos;
            this.yaw   = yaw;
            this.sneak = sneak;
            this.phase = phase;
        }
    }

    private void cleanupRecentDeaths(long now) {
        recentDeaths.entrySet().removeIf(e -> now - e.getValue() > DEATH_CACHE_MS);
    }

    private void clearAll() {
        effects.clear();
        recentDeaths.clear();
        entityHealthCache.clear();
        CHAMS_GHOSTS.clear();
      //  ghostSouls.clear();
    }
    private abstract static class EffectEntry {
        final Vec3d pos;
        final int color;
        final long createdAt;
        final long lifeMs;

        EffectEntry(Vec3d pos, int color, long createdAt, long lifeMs) {
            this.pos = pos;
            this.color = color;
            this.createdAt = createdAt;
            this.lifeMs = lifeMs;
        }

        boolean isFinished(long now) { return now - createdAt >= lifeMs; }
        float progress(long now) { return Math.min(1f, (now - createdAt) / (float) lifeMs); }

        abstract void render(MatrixStack stack, VertexConsumer buf, float alphaMult, long now);

        void emitBillboard(MatrixStack stack, VertexConsumer buf, Vec3d worldPos, float size, int argb) {
            if (size <= 0.001f || (argb >>> 24) == 0) return;
            stack.push();
            Client.RENDERER.setupOrientationMatrix(stack, (float) worldPos.x, (float) worldPos.y, (float) worldPos.z);
            stack.multiply(net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
            Matrix4f mat = stack.peek().getPositionMatrix();
            float h = size * 0.5f;
            int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF, a = (argb >>> 24);
            buf.vertex(mat, -h, -h, 0).texture(0, 1).color(r, g, b, a);
            buf.vertex(mat,  h, -h, 0).texture(1, 1).color(r, g, b, a);
            buf.vertex(mat,  h,  h, 0).texture(1, 0).color(r, g, b, a);
            buf.vertex(mat, -h,  h, 0).texture(0, 0).color(r, g, b, a);
            stack.pop();
        }

        int withAlpha(int rgb, int a) {
            return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0x00FFFFFF);
        }
    }

    private static class LightningEffect extends EffectEntry {
        private final List<Bolt> bolts = new ArrayList<>();

        LightningEffect(Vec3d pos, int color, java.util.Random rng, long now) {
            super(pos, color, now, EFFECT_LIFE_MS);
            int boltCount = 12 + rng.nextInt(7);
            for (int i = 0; i < boltCount; i++) {
                bolts.add(new Bolt(pos, rng));
            }
        }

        @Override
        void render(MatrixStack stack, VertexConsumer buf, float alphaMult, long now) {
            float t = progress(now);
            float fade = t < 0.12f ? t / 0.12f : 1f - (t - 0.12f) / 0.88f;
            float alpha = fade * alphaMult;
            if (alpha <= 0.001f) return;

            for (Bolt bolt : bolts) {
                bolt.render(stack, buf, color, alpha, this);
            }
        }

        static class Bolt {
            final List<Vec3d> points = new ArrayList<>();

            Bolt(Vec3d origin, java.util.Random rng) {
                double theta = rng.nextDouble() * Math.PI * 2;
                double phi   = Math.acos(1 - 2 * rng.nextDouble());
                double spread = 0.5 + rng.nextDouble() * 1.0;

                Vec3d dir = new Vec3d(
                        Math.sin(phi) * Math.cos(theta),
                        Math.cos(phi),
                        Math.sin(phi) * Math.sin(theta)
                );

                int segments = 7 + rng.nextInt(5);
                Vec3d cur = origin;
                points.add(cur);
                for (int i = 0; i < segments; i++) {
                    double jitter = spread * 0.18;
                    double dx = dir.x * spread * 0.22 + rng.nextGaussian() * jitter;
                    double dy = dir.y * spread * 0.22 + rng.nextGaussian() * jitter;
                    double dz = dir.z * spread * 0.22 + rng.nextGaussian() * jitter;
                    cur = cur.add(dx, dy, dz);
                    points.add(cur);
                }
            }

            void render(MatrixStack stack, VertexConsumer buf, int color, float alpha, EffectEntry e) {
                for (int i = 0; i < points.size() - 1; i++) {
                    Vec3d a = points.get(i);
                    float segAlpha = alpha * (1f - (float) i / points.size() * 0.55f);
                    e.emitBillboard(stack, buf, a, 0.24f, e.withAlpha(color, (int)(segAlpha * 65)));
                    e.emitBillboard(stack, buf, a, 0.08f, e.withAlpha(0xFFFFFF, (int)(segAlpha * 230)));
                }

                Vec3d tip = points.get(points.size() - 1);
                e.emitBillboard(stack, buf, tip, 0.38f, e.withAlpha(color, (int)(alpha * 85)));
                e.emitBillboard(stack, buf, tip, 0.13f, e.withAlpha(0xFFFFFF, (int)(alpha * 210)));
            }
        }
    }


    private static class BeamEffect extends EffectEntry {
        private static final long BEAM_LIFE = 2800L;
        private static final float FADE_IN  = 0.12f;
        private static final float FADE_OUT = 0.55f;

        private final List<Vec3d> curve = new ArrayList<>();

        BeamEffect(Vec3d pos, int color, java.util.Random rng, long now) {
            super(pos, color, now, BEAM_LIFE);
            buildCurve(pos, rng);
        }

        private void buildCurve(Vec3d target, java.util.Random rng) {
            double height = 20 + rng.nextDouble() * 15;
            double offX = (rng.nextDouble() - 0.5) * 12;
            double offZ = (rng.nextDouble() - 0.5) * 12;
            Vec3d start = target.add(offX, height, offZ);

            int segments = 28;
            for (int i = 0; i <= segments; i++) {
                float t = (float) i / segments;
                Vec3d base = start.lerp(target, t);
                double bend = Math.sin(t * Math.PI) * (1.5 + rng.nextDouble() * 1.5);
                double bx = rng.nextGaussian() * 0.4 * bend;
                double bz = rng.nextGaussian() * 0.4 * bend;
                curve.add(base.add(bx, 0, bz));
            }
        }

        @Override
        void render(MatrixStack stack, VertexConsumer buf, float alphaMult, long now) {
            float t = progress(now);
            float fadeIn  = t < FADE_IN  ? t / FADE_IN  : 1f;
            float fadeOut = t > FADE_OUT ? 1f - (t - FADE_OUT) / (1f - FADE_OUT) : 1f;
            fadeOut = easeInOutSine(fadeOut);
            float alpha = fadeIn * fadeOut * alphaMult;
            if (alpha <= 0.001f) return;

            for (int i = 0; i < curve.size(); i++) {
                Vec3d p = curve.get(i);
                float posT = (float) i / (curve.size() - 1);
                float brightness = (float)(Math.sin(posT * Math.PI) * 0.6 + 0.4);
                emitBillboard(stack, buf, p, 0.80f, withAlpha(color,   (int)(alpha * brightness * 40)));
                emitBillboard(stack, buf, p, 0.40f, withAlpha(color,   (int)(alpha * brightness * 110)));
                emitBillboard(stack, buf, p, 0.16f, withAlpha(color,   (int)(alpha * brightness * 200)));
                emitBillboard(stack, buf, p, 0.05f, withAlpha(0xFFFFFF, (int)(alpha * brightness * 255)));
            }

            Vec3d impact = curve.get(curve.size() - 1);
            emitBillboard(stack, buf, impact, 1.2f,  withAlpha(color,   (int)(alpha * 55)));
            emitBillboard(stack, buf, impact, 0.55f, withAlpha(color,   (int)(alpha * 130)));
            emitBillboard(stack, buf, impact, 0.20f, withAlpha(0xFFFFFF, (int)(alpha * 255)));
        }

        private float easeInOutSine(float t) {
            return -(float)(Math.cos(Math.PI * t) - 1) / 2f;
        }
    }


    private static class SoulEffect extends EffectEntry {
        SoulEffect(Vec3d pos, int color, long now) {
            super(pos, color, now, SOUL_LIFE_MS);
        }

        @Override
        void render(MatrixStack stack, VertexConsumer buf, float alphaMult, long now) {
            float t = progress(now);
            float rise = easeOutSine(t) * SOUL_RISE;
            float fade = 1f - easeInOutQuad(t);
            float alpha = MathHelper.clamp(fade * alphaMult, 0f, 1f);
            if (alpha <= 0.001f) return;

            Vec3d risePos = pos.add(0, rise, 0);

            emitBillboard(stack, buf, risePos, 0.9f,  withAlpha(color,   (int)(alpha * 60)));
            emitBillboard(stack, buf, risePos, 0.5f,  withAlpha(color,   (int)(alpha * 120)));
            emitBillboard(stack, buf, risePos, 0.25f, withAlpha(0xFFFFFF, (int)(alpha * 200)));

            long elapsed = now - createdAt;
            for (int i = 0; i < 4; i++) {
                double angle = elapsed * 0.004 + i * Math.PI * 0.5;
                double r = 0.3 * (1f - t * 0.5f);
                Vec3d spark = risePos.add(
                        Math.cos(angle) * r,
                        Math.sin(elapsed * 0.003 + i) * 0.15,
                        Math.sin(angle) * r);
                emitBillboard(stack, buf, spark, 0.12f, withAlpha(color,   (int)(alpha * 150)));
                emitBillboard(stack, buf, spark, 0.05f, withAlpha(0xFFFFFF, (int)(alpha * 220)));
            }
        }

        private float easeOutSine(float t) {
            return (float) Math.sin(t * Math.PI * 0.5);
        }

        private float easeInOutQuad(float t) {
            return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
        }
    }
}
