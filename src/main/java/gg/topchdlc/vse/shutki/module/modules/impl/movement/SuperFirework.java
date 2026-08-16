package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventBoost;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.ElytraAura;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SuperFirework extends Module {
    public static final SuperFirework INSTANCE = new SuperFirework();
    private SuperFirework() {
        super("Super Firework", Category.MOVEMENT, "ускоряет скорость даваемую от фееров");
    }

    public enum Mode {
        BravoHvH, Custom
    }

    public EnumSetting<Mode> mode = enumSetting("Boost mode", Mode.BravoHvH);
    public CheckBox distill = checkbox("Slowdown if target <1.5", true);
    public CheckBox boostOnDefensive = checkbox("Boost on Defensive(Elytraura)", true);
    public enum DefensiveMode { Randomize, TargetSpeed, Disable }
    public EnumSetting<DefensiveMode> defensiveMode = enumSetting("Boost mode on Defensive", DefensiveMode.TargetSpeed).visible(boostOnDefensive::get);
    public SliderSetting multiplier = sliderSetting("Multiplier", 0.3f, 0, 2.5f).increment(0.1f).visible(() -> mode.is(Mode.Custom));
    public SliderSetting multiplierTimer = sliderSetting("Multiplier Timer", 1.0f, 1f, 1.2f).increment(0.01f);
    public SliderSetting multiplierMotion = sliderSetting("Multiplier if bps <45", 1.0f, 1f, 1.2f).increment(0.01f);

    private final TimeUtility speedtopTimer = new TimeUtility();
    private boolean activatedBooster = false;
    public float speedtop = 0.0F;

    @Override
    protected void onDisable() {
        super.onDisable();
        Client.TIMER = 1;
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventBoost ctx) {
            LivingEntity entity = TargetsUtility.getTarget();
            float pitch = Client.ROTATION.getRotate().getPitch();
            float yaw = Client.ROTATION.getRotate().getYaw();

            float normalizedYaw = yaw % 180.0F;
            if (normalizedYaw > 90.0F) {
                normalizedYaw -= 180.0F;
            } else if (normalizedYaw < -90.0F) {
                normalizedYaw += 180.0F;
            }

            if (mc.player.isGliding())
                Client.TIMER = multiplierTimer.get();

            if (mc.player.isGliding())

                if (MoveUtility.getBPS(mc.player) < 45)
                    mc.player.setVelocity(mc.player.getVelocity().multiply(multiplierMotion.get()));

            switch (mode.get()) {
                case Custom -> {
                    Vec3d look = Client.ROTATION.getRotate().toVector();

                    double ay = Math.max(0.2f, Math.abs(look.y));

                    double angleOffsetFactor = 1.f - (Math.abs(MathHelper.wrapDegrees(45 - mc.player.getYaw() % 90)) / 45.f);
                    double boost = 1.5 + (multiplier.get() * Math.pow(angleOffsetFactor, 2));

                    ctx.speedx = boost + ay * 0.735;
                    ctx.speedz = ctx.speedx;
                    ctx.speedy = ay * 1.3;
                }

                case BravoHvH -> {
                    ctx.speedx = Math.max(getSpeedForPitch(normalizedYaw) * 0.995, getSpeedForPitch1(pitch, normalizedYaw) * 0.985f); ctx.speedz = ctx.speedx;
                    ctx.speedy = Math.max(getSpeedYForPitch(pitch), getSpeedYForPitch1(pitch));
                }
            }

            if (boostOnDefensive.get() && (ElytraAura.INSTANCE.antiAimIsActive) && ElytraAura.INSTANCE.defensive.get() && ElytraAura.INSTANCE.isEnabled()) {
                if (TargetsUtility.getTarget() != null) {
                    switch (defensiveMode.get()) {
                        case Randomize -> {
                            float r = MathUtility.random(0.97f, 1f);
                            ctx.speedx *= r;
                            ctx.speedz = ctx.speedx;
                            ctx.speedy *= r;
                        }

                        case Disable -> {
                            ctx.speedx = 1.5f;
                            ctx.speedz = 1.5f;
                            ctx.speedy = 1.5f;
                        }

                        case TargetSpeed -> {
                            float speedtarget = (float) Math.max(bpstarget(entity) / 20, 1.6);
                            ctx.speedx = speedtarget;
                            ctx.speedz = speedtarget;
                            ctx.speedy = speedtarget;
                        }
                    }
                }
            }

            LivingEntity target = TargetsUtility.getTarget();
            if (distill.get() && target != null && ElytraAura.INSTANCE.isEnabled() && ElytraAura.INSTANCE.targetIsLeave(target)) {
                Vec3d distillPos = target.getLerpedPos(mc.getRenderTickCounter().getTickProgress(true))
                        .add(target.getRotationVec(mc.getRenderTickCounter().getTickProgress(true)).multiply(8));
                if (mc.player.getEntityPos().distanceTo(distillPos) < 1.5) {
                    ctx.speedx = 1.3f;
                    ctx.speedz = 1.3f;
                    ctx.speedy = 1.3f;
                }
            }
        }

        if (event instanceof EventReceivePacket e) {
            if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
                this.activatedBooster = true;
            }
        }

        if (event instanceof EventGameTick gameTick) {
            if (this.speedtopTimer.reached(40L) && mc.player.isGliding()) {
                if (!this.activatedBooster) {
                    this.speedtop += 1.0E-4F;
                }

                if (mc.player.age % 10 == 0) {
                    this.speedtop -= 5.0E-4F;
                }

                if (this.speedtop > 0.0011F) {
                    this.speedtop = 0.0F;
                }

                if (this.activatedBooster) {
                    this.speedtop = 0.0F;
                }

                this.speedtopTimer.reset();
            }
        }
    };

    public static float bpstarget(LivingEntity entity) {
        if (entity == null) return 1.47f;
        double distance = Math.sqrt(Math.pow(entity.getX() - entity.lastRenderX, 2.0D) + Math.pow(entity.getY() - entity.lastRenderY, 2.0D) + Math.pow(entity.getZ() - entity.lastRenderZ, 2.0D));
        float bps = (float)(distance * 20.0D);
        return (float)Math.round(bps * 10.0F) / 10.2F;
    }

    private float getSpeedForPitch1(float pitch, float yaw) {
        if (Math.abs(pitch) >= 38.0F && Math.abs(pitch) <= 52.0F) {
            return 1.99F;
        } else if (Math.abs(pitch) >= 32.0F && Math.abs(pitch) <= 58.0F) {
            return 1.97F;
        } else if (Math.abs(pitch) >= 28.0F && Math.abs(pitch) <= 62.0F) {
            return 1.95F;
        } else if (Math.abs(yaw) >= 29.0F && Math.abs(yaw) <= 61.0F || Math.abs(pitch) >= 29.0F && Math.abs(pitch) <= 61.0F) {
            return 1.961F;
        } else if ((!(Math.abs(yaw) >= 27.0F) || !(Math.abs(yaw) <= 63.0F)) && (!(Math.abs(pitch) >= 27.0F) || !(Math.abs(pitch) <= 63.0F))) {
            if ((!(Math.abs(yaw) >= 22.0F) || !(Math.abs(yaw) <= 68.0F)) && (!(Math.abs(pitch) >= 22.0F) || !(Math.abs(pitch) <= 68.0F))) {
                if (Math.abs(yaw) >= 15.0F && Math.abs(yaw) <= 75.0F || Math.abs(pitch) >= 15.0F && Math.abs(pitch) <= 75.0F) {
                    return 1.7F;
                } else if ((!(Math.abs(yaw) >= 13.0F) || !(Math.abs(yaw) <= 77.0F)) && (!(Math.abs(pitch) >= 13.0F) || !(Math.abs(pitch) <= 77.0F))) {
                    if ((!(Math.abs(yaw) >= 12.0F) || !(Math.abs(yaw) <= 78.0F)) && (!(Math.abs(pitch) >= 12.0F) || !(Math.abs(pitch) <= 78.0F))) {
                        if (Math.abs(yaw) >= 8.0F && Math.abs(yaw) <= 82.0F || Math.abs(pitch) >= 11.0F && Math.abs(pitch) <= 79.0F) {
                            return 1.66F;
                        } else {
                            return (!(Math.abs(yaw) >= 5.0F) || !(Math.abs(yaw) <= 85.0F)) && (!(Math.abs(pitch) >= 8.0F) || !(Math.abs(pitch) <= 82.0F)) ? 1.621F : 1.63F;
                        }
                    } else {
                        return 1.68F;
                    }
                } else {
                    return 1.68F;
                }
            } else {
                return 1.77F;
            }
        } else {
            return 1.87F;
        }
    }


    private float getSpeedForPitch(float yaw) {
        yaw = Math.abs(yaw);
        if (yaw > 0 && yaw < 5) return 1.65f;
        else if (yaw < 90 && yaw > 85) return 1.65f;

        else if (yaw > 5 && yaw < 10) return 1.66f;
        else if (yaw > 80 && yaw < 85) return 1.66f;

        else if (yaw > 10 && yaw < 15) return 1.67f;
        else if (yaw > 75 && yaw < 80) return 1.67f;

        else if (yaw > 15 && yaw < 20) return 1.75f;
        else if (yaw > 70 && yaw < 75) return 1.75f;

        else if (yaw > 20 && yaw < 25) return 1.76f;
        else if (yaw > 65 && yaw < 70) return 1.76f;

        else if (yaw > 25 && yaw < 30) return 1.83f;
        else if (yaw > 60 && yaw < 65) return 1.83f;

        else if (yaw > 30 && yaw < 35) return 1.93f;
        else if (yaw > 55 && yaw < 60) return 1.93f;

        else if (yaw > 35 && yaw < 40) return 1.95f;
        else if (yaw > 50 && yaw < 55) return 1.95f;

        else if (yaw > 40 && yaw < 45) return 1.99f;
        else if (yaw > 45 && yaw < 50) return 1.99f;
        else return 1.6f;
    }

    private float getSpeedYForPitch1(float yaw) {
        yaw = Math.abs(yaw);
        if (yaw > 0 && yaw < 5) return 1.59f;
        else if (yaw > 5 && yaw < 10) return 1.61f;
        else if (yaw > 10 && yaw < 15) return 1.7f;
        else if (yaw > 15 && yaw < 20) return 1.68f;
        else if (yaw > 20 && yaw < 25) return 1.8f;
        else if (yaw > 25 && yaw < 30) return 1.82f;
        else if (yaw > 30 && yaw < 35) return 1.95f;
        else if (yaw > 35 && yaw < 40) return 1.99f;
        else if (yaw > 40 && yaw < 45) return 1.98f;
        else return 1.6f;
    }

    private float getSpeedYForPitch(float pitch) {
        if (Math.abs(pitch) >= 37.0F && Math.abs(pitch) <= 38.0F) {
            return 1.98F;
        } else if (Math.abs(pitch) >= 20.0F && Math.abs(pitch) <= 37.0F) {
            return 1.95F;
        } else if (Math.abs(pitch) >= 25.0F && Math.abs(pitch) <= 30.0F) {
            return 1.95F;
        } else if (Math.abs(pitch) >= 35.0F && Math.abs(pitch) <= 45.0F) {
            return 1.98F;
        } else if (Math.abs(pitch) >= 40.0F && Math.abs(pitch) <= 50.0F) {
            return 1.96F;
        } else if (Math.abs(pitch) >= 50.0F && Math.abs(pitch) <= 60.0F) {
            return 1.95F;
        } else if (Math.abs(pitch) >= 51.0F && Math.abs(pitch) <= 61.0F) {
            return 1.86F;
        } else if (Math.abs(pitch) >= 52.0F && Math.abs(pitch) <= 65.0F) {
            return 1.7F;
        } else {
            return 1.62f;
        }
    }
}
