package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import cc.snais.Sosiski;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.*;
import gg.topchdlc.mixin.accessor.ILocalPlayer;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.RotationSettings;
import gg.topchdlc.vse.rotation.RotationTiming;
import gg.topchdlc.vse.rotation.all.ISnapRotation;
import gg.topchdlc.vse.rotation.all.impl.FTSnap;
import gg.topchdlc.vse.rotation.all.impl.HolyWorld;
import gg.topchdlc.vse.rotation.all.impl.SlothRotation;
import gg.topchdlc.vse.rotation.point.PointTracker;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.Tag;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.attack.AttackHandler;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.attack.AttackUtility;
import gg.topchdlc.vse.utils.attack.ShieldBreakerUtility;
import gg.topchdlc.vse.utils.client.get.DebugUtility;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RayTraceUtility;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Vec3d;

@Sosiski
public class AuraModule extends Module {
    public static final AuraModule INSTANCE = new AuraModule();

    private AuraModule() {
        super("Aura", Category.COMBAT, "Автоматически наводится и бьет противника", Tag.Sosiski);
    }

    public SliderSetting attackRangeSetting = sliderSetting("Attack Range", 3f, 0f, 6f).increment(0.1f);
    public SliderSetting preAttackRangeSetting = sliderSetting("Target Range", 5f, 0f, 7f).increment(0.1f);
    public EnumSetting<TargetsUtility.Sort> sortTargetSetting = enumSetting("Sort Target", TargetsUtility.Sort.Adaptive);
    private final RotationSettings rotations = add(new RotationSettings("Rotation Settings", Priorities.AURA));
    public CheckBox checkCrit = checkbox("Check critical", true);
    public CheckBox shieldBreak = checkbox("ShieldBreak", false);
    public CheckBox smartCrits = checkbox("Smart Crit", false).visible(() -> checkCrit.get());
    public CheckBox noAttackUseItem = checkbox("No attack use item", true);
    public CheckBox wallCheck = checkbox("Wall check", true);
    public CheckBox wallRotation = checkbox("Wall rotation", true).visible(() -> wallCheck.get());
    public CheckBox raytrace = checkbox("Raytrace", true);
    public CheckBox tpsSync = checkbox("TPS Sync", true);

    public MultiEnumSetting<Feature> auraSetting = multiEnumSetting("Setting", Feature.class);
    public EnumSetting<ResetSprint> resetSprint = enumSetting("Type Reset Sprint", ResetSprint.Legit);
    public EnumSetting<AntiCheat> antiCheat = enumSetting("AntiCheat", AntiCheat.None);

    private final PointTracker point = add(new PointTracker());
    private final EnumSetting<RotationTiming> rotationTiming = rotations.enumSetting("Timing", RotationTiming.Normal).visible(() -> rotations.Customization.get());
    private final SliderSetting snapProgress = rotations.sliderSetting("Snap progress", 0.8f, 0, 1).increment(0.01f).visible(() -> rotations.Customization.get(), () -> rotationTiming.is(RotationTiming.Snap));
    private final CheckBox snapFollow = rotations.checkbox("Snap follow target", true).visible(() -> rotations.Customization.get(), () -> rotationTiming.is(RotationTiming.Snap));
    public CheckBox sensitivityBypass = checkbox("Sensitivity Bypass", false);
    public CheckBox logMiss = checkbox("Log misses", Client.IS_DEBUG).visible(() -> Client.IS_DEBUG);
    public CheckBox randomFallDistanceSetting = checkbox("Random Fall Distance", true);
    public final AttackUtility attack = add(new AttackUtility());

    private boolean shouldRecoverSprint = false;
    private Angle angle;
    private ISnapRotation currentSnapRotation = null;
    private Vec3d lastTargetPos;
    private Angle lasttargetangle;
    private boolean needBackrotate = false;

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (nullCheck()) return;

            updateTarget();
            if (rotations.mode.get() instanceof ISnapRotation) {
                currentSnapRotation = (ISnapRotation) rotations.mode.get();
                currentSnapRotation.tick();
                if (currentSnapRotation.shouldReturn()) {
                    Angle returnAngle = currentSnapRotation.getReturnAngle();
                    if (returnAngle != null) {
                        rotations.rotate(returnAngle);
                    }
                }
            } else {
                currentSnapRotation = null;
            }

            ElytraTarget.INSTANCE.tick(currentSnapRotation, rotations);

            LivingEntity target = TargetsUtility.getTarget();

            if (target == null) {
                if (needBackrotate) {
                    if (rotations.backroation.get() && lasttargetangle != null && mc.player != null) {
                        mc.player.setYaw(lasttargetangle.getYaw());
                        mc.player.setPitch(lasttargetangle.getPitch());
                        NetworkUtility.sendAngle(lasttargetangle);
                    }
                    needBackrotate = false;
                    lastTargetPos = null;
                    lasttargetangle = null;
                }
                return;
            }

            needBackrotate = true;
            lastTargetPos = target.getBoundingBox().getCenter();
            lasttargetangle = RotationUtility.calcRotate(lastTargetPos, true);

            if (auraSetting.get(Feature.DisableOnDeath) && !mc.player.isAlive()) {
                toggle();
                return;
            }

            Vec3d p = ElytraTarget.INSTANCE.getTargetPoint(target, point.getPoint(target));

            DebugUtility.trace("Aura point", p.toString());
            angle = RotationUtility.calcRotate(p, true);
            DebugUtility.trace("Aura angle", angle.toString());
            DebugUtility.trace("Aura attack progress", attack.getProgress());
            boolean canSeeTarget = canSeeTarget(target);

            boolean shouldRotate = !wallCheck.get() || canSeeTarget || wallRotation.get();
            boolean isSnapRotation = currentSnapRotation != null;
            boolean isFTSnap = rotations.mode.get() instanceof FTSnap;

            if (shouldRotate) {
                if (isSnapRotation && !isFTSnap) {
                } else if (isFTSnap) {
                    rotations.rotate(angle);
                } else {
                    boolean snapApplied = ElytraTarget.INSTANCE.applyRotation(angle, currentSnapRotation, rotations);

                    if (!snapApplied) {
                        if (rotationTiming.is(RotationTiming.Tick)) {
                            if (!Client.ROTATION.getRotate().equals(angle))
                                NetworkUtility.sendAngle(angle);
                        } else if (!rotationTiming.is(RotationTiming.Snap) || attack.getProgress() >= snapProgress.get()) {
                            rotations.rotate(angle);
                        }
                    }
                }
            }

            if (shieldBreaker() || isCheckWeapon()) return;

            if (!updateAttack(Client.ROTATION.getRotate())) {
            }

            if (rotationTiming.is(RotationTiming.Tick) && !Client.ROTATION.getRotate().equals(angle)) {
                NetworkUtility.sendAngle(Client.ROTATION.getRotate());
            }
        }

        if (event instanceof EventInput eventInput) {
            if (angle != null && rotationTiming.is(RotationTiming.Snap) && snapFollow.get() && attack.getProgress() < snapProgress.get()) {
                MoveUtility.silentCorrectRaw(eventInput, angle.getYaw());
            }

            if (TargetsUtility.getTarget() != null) {
                boolean resetSprint = AttackHandler.shouldResetSprinting();
                if (resetSprint && !mc.player.isTouchingWater() && eventInput.getForward() > 0) {
                    if (this.resetSprint.is(ResetSprint.Legit)) {
                        eventInput.setForward(0);
                        eventInput.setSprint(false);
                        shouldRecoverSprint = true;
                        return;
                    }
                }
            }
            if (shouldRecoverSprint) {
                eventInput.setForward(1);
                eventInput.setSprint(true);
                shouldRecoverSprint = false;
            }
        }

        if (event instanceof Event3D e) {
            if (TargetsUtility.getTarget() != null) {
                ElytraTarget.INSTANCE.renderPredictBox(e, TargetsUtility.getTarget());
            }
        }
    };

    public float getAttackRange() {
        return attackRangeSetting.get();
    }

    public float getTargetRange() {
        return ElytraTarget.INSTANCE.getTargetRange(preAttackRangeSetting.get());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        TargetsUtility.reset();
        needBackrotate = false;
        lasttargetangle = null;
        lastTargetPos = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (rotations.backroation.get() && lasttargetangle != null && needBackrotate) {
            if (mc.player != null) {
                mc.player.setYaw(lasttargetangle.getYaw());
                mc.player.setPitch(lasttargetangle.getPitch());
                NetworkUtility.sendAngle(lasttargetangle);
            }
        }
        needBackrotate = false;
        lasttargetangle = null;
        lastTargetPos = null;
        TargetsUtility.reset();
        ElytraTarget.INSTANCE.reset();
    }

    public void updateTarget() {
        TargetsUtility.find(getAttackRange() + getTargetRange(), sortTargetSetting.get());
    }

    public boolean isCheckWeapon() {
        return auraSetting.get(Feature.OnlyWeapon) && !(mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem().equals(Items.NETHERITE_SWORD) || mc.player.getMainHandStack().getItem().equals(Items.DIAMOND_SWORD) || mc.player.getMainHandStack().getItem().equals(Items.IRON_SWORD) || mc.player.getMainHandStack().getItem().equals(Items.STONE_SWORD) || mc.player.getMainHandStack().getItem().equals(Items.GOLDEN_SWORD) || mc.player.getMainHandStack().getItem().equals(Items.WOODEN_SWORD));
    }

    public boolean updateAttack(Angle angle) {
        if (TargetsUtility.getTarget() == null) return false;
        Angle aim = this.angle != null ? this.angle : angle;
        if (tpsSync.get()) {
            float tps = NetworkUtility.getTpsFactor();
            if (tps > 0 && tps < 19.5f) {
                if (mc.player.getAttackCooldownProgress(0.0f) < 1.0f) {
                    return false;
                }
            } else if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) {
                return false;
            }
        }
        if (antiCheat.is(AntiCheat.FunTime) || antiCheat.is(AntiCheat.SpookyTime)) {
            if (mc.player.getAttackCooldownProgress(0.0f) < 1.0f) {
                return false;
            }
        }
        if (!AttackHandler.shouldAttack()) return false;

        LivingEntity target = TargetsUtility.getTarget();

        if (!checkAttackDistance(target)) return false;
        if (wallCheck.get() && !canSeeTarget(target)) return false;
        if (noAttackUseItem.get() && mc.player.isUsingItem()) {
            return false;
        }

        if (raytrace.get()) {
            if (!raycast(aim)) {
                if (logMiss.get()) ChatUtility.send("[Aura] Raytrace miss (not aimed at target)!");
                if (rotations.mode.get() instanceof HolyWorld holyWorld) {
                    holyWorld.triggerMissDrift();
                }
                return false;
            }
        }

        boolean onElytra = mc.player.isGliding();
        boolean isSnapRotation = currentSnapRotation != null;

        boolean isElytraSnapMode = onElytra && ElytraTarget.INSTANCE.isElytraFlying() &&
                ElytraTarget.INSTANCE.rotationMode.is(ElytraTarget.RotationMode.Default);
        if (onElytra && ElytraTarget.INSTANCE.shouldCheckRaycast() && !isElytraSnapMode) {
            if (!raycast(this.angle)) {
                if (logMiss.get()) ChatUtility.send("[Aura] Elytra raycast miss!");
                return false;
            }
        }
        if (isSnapRotation && !onElytra) {
            if (!raycast(this.angle)) {
                if (logMiss.get()) ChatUtility.send("[Aura] Snap miss!");
                return false;
            }
        }

        if (resetSprint.is(ResetSprint.Legit) && ((ILocalPlayer) mc.player).serverSprintState() && !mc.player.isTouchingWater()) {
            if (logMiss.get()) ChatUtility.send("[Aura] blocked: serverSprint");
            return false;
        }

        int clicks = attack.getClicks();
        if (clicks == 0) return false;
        if (isSnapRotation) {
            if (!currentSnapRotation.isSnapped()) {
                currentSnapRotation.prepareSnap(Angle.fromPlayer());
                rotations.rotate(this.angle);
                return false;
            }
            if (!currentSnapRotation.isAimedAt(Client.ROTATION.getRotate(), this.angle, 30f)) {
                rotations.rotate(this.angle);
                return false;
            }
        }

        boolean sprint = NetworkUtility.serverSprinting();
        if (resetSprint.is(ResetSprint.Packet) && sprint) {
            mc.getNetworkHandler().sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.player.setSprinting(false);
        }
        if (resetSprint.is(ResetSprint.LegitFast) && sprint) {
            NetworkUtility.sendInputPacket(
                    mc.player.input.playerInput.forward(),
                    mc.player.input.playerInput.backward(),
                    mc.player.input.playerInput.left(),
                    mc.player.input.playerInput.right(),
                    mc.player.input.playerInput.jump(),
                    mc.player.input.playerInput.sneak(),
                    false
            );
        }

        Client.EVENTS.post(EventAttack.build(target));
        if (antiCheat.is(AntiCheat.FunTime) || antiCheat.is(AntiCheat.SpookyTime)) {
            NetworkUtility.sendLook(aim);
            clicks = Math.min(clicks, 1);
        }
        for (int i = 0; i < clicks; i++) {
            AttackHandler.attackEntity(target);
        }
        if (rotations.mode.get() instanceof SlothRotation slothRotation) {
            slothRotation.onAttack();
        }
        if (rotations.mode.get() instanceof HolyWorld holyWorld) {
            holyWorld.registerHit();
        }

        if (resetSprint.is(ResetSprint.Packet) && sprint) {
            mc.getNetworkHandler().sendPacket(
                    new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            mc.player.setSprinting(true);
        }
        if (resetSprint.is(ResetSprint.LegitFast) && sprint) {
            NetworkUtility.sendInputPacket(
                    mc.player.input.playerInput.forward(),
                    mc.player.input.playerInput.backward(),
                    mc.player.input.playerInput.left(),
                    mc.player.input.playerInput.right(),
                    mc.player.input.playerInput.jump(),
                    mc.player.input.playerInput.sneak(),
                    true
            );
        }

        return true;
    }

    private boolean checkAttackDistance(LivingEntity target) {
        if (mc.player.isGliding() && ElytraTarget.INSTANCE.isElytraFlying()) {
            return true;
        }
        if (RotationUtility.getStrictDistance(target) > attackRange()) {
            return false;
        }

        return true;
    }

    public float attackRange() {
        if (Resolver.INSTANCE.isFakeLagging())
            return 12.F;
        return getAttackRange();
    }

    public boolean raycast(Angle angle) {
        if (TargetsUtility.getTarget() == null) return false;
        return RayTraceUtility.rayTrace(angle.toVector(), attackRange(), TargetsUtility.getTarget().getBoundingBox());
    }

    private boolean shieldBreaker() {
        if (!shieldBreak.get() || TargetsUtility.getTarget() == null) {
            return false;
        }

        if (TargetsUtility.getTarget().isBlocking()) {
            return ShieldBreakerUtility.shieldBreaker(1);
        }

        return false;
    }

    private boolean canSeeTarget(LivingEntity target) {
        if (target == null || mc.player == null || mc.world == null) return false;
        return mc.player.canSee(target);
    }

    public enum ResetSprint {
        Legit,
        LegitFast,
        Packet,
        None
    }

    @AllArgsConstructor
    @Getter
    public enum Feature implements EnumChoice {
        DisableOnDeath("Turn off on death", true),
        OnlyWeapon("Only Weapon", false),
        KeepSprint("Keep Sprint", false);
        final String renderName;
        final boolean defaultEnabled;
    }

    public enum AntiCheat {
        None,
        FunTime,
        SpookyTime,
        NCP
    }
}