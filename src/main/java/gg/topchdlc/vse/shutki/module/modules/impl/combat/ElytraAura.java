package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.*;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.Tag;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.attack.ElytraAuraAttackHandler;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.defensive.ElytraAuraDef;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.math.ElytraAuraResolve;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.utilities.ElytraAuraDefensive;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.utilities.ElytraAuraUtility;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver.BackTrackPosResolver;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.text.TextSetting;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPositionEntity;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.player.PlayerUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.awt.*;
import java.util.Random;

public class ElytraAura extends Module {
    public static final ElytraAura INSTANCE = new ElytraAura();
    private ElytraAura() {
        super("Elytra Aura HVH ", Category.COMBAT, "Pizdec na ELytra", Tag.Sosiski);
    }


    public SliderSetting attackRangeSetting = sliderSetting("Attack Range", 3.2f, 3f, 4f).increment(.1f);
    public SliderSetting preAttackRangeSetting = sliderSetting("Pre Range", 50f, 15f, 60f).increment(1f);
    public CheckBox autoStartSetting =checkbox("Auto Start", true);
    public CheckBox autoFireWorkSetting =checkbox("Auto FireWork", true);
    public CheckBox smartUseFireWorkSetting =checkbox("Smart Use", true).visible(autoFireWorkSetting::get);
    public SliderSetting delayUseFireWorkSetting =sliderSetting("Delay Use", 400, 200, 800).increment(50).visible(() -> autoFireWorkSetting.get() && !smartUseFireWorkSetting.get());
    @AllArgsConstructor @Getter public enum TypeAim implements EnumChoice {
        Resolve("Resolve pos."),
        FireWork("FireWork Pos."),
        Middle("Middle Pos."),
        Default("Default Pos.");
        final String renderName;
    }
    public EnumSetting<TypeAim> typeAim = enumSetting("Type Aim", TypeAim.FireWork).desc("Where should target");
    public CheckBox predictYawandPitchSetting = checkbox("Predict for Target Rotation", true);
    public CheckBox isAirStack = checkbox("Air Stuck", false).desc("Freeze local player");
    public CheckBox autoAirStackSetting = checkbox("Auto Mode", false).visible(isAirStack::get);
    public MultiEnumSetting<Feature> ifworkAitStack = multiEnumSetting("Conditions", Feature.class).desc("When should freeze local player automatically").visible(autoAirStackSetting::get);
    public KeybindSetting bindAirStackSetting = keybindSetting("Key on Active", -1).visible(isAirStack::get, ()-> !autoAirStackSetting.get());
    public final CheckBox defensive = checkbox("Defensive",false);
    public CheckBox smartDefensiveTimeSetting = checkbox("Smart Time", false).desc("Automatically calculate time to fly in").visible(defensive::get);
    public SliderSetting antiAimDelayDelaySetting = sliderSetting("Defensive time", 250, 150f, 500f).desc("Flying off time").increment(25f).visible(() -> !smartDefensiveTimeSetting.get()).visible(defensive::get);
    public CheckBox timer = checkbox("Timer", true).desc("Speeding up game to distill target").visible(defensive::get);
    public CheckBox aimWhenTargetAttack = checkbox("Attack when target pushing", false).desc("Starting aim at target when target trying to push").visible(defensive::get);

    @AllArgsConstructor @Getter public enum DefensiveCondition implements EnumChoice {
        TargetNotGliding("If the target doesn't fly"),
        Always("Always");
        final String renderName;
    }
    public EnumSetting<DefensiveCondition> ifWorkAntiAimSetting = enumSetting("Defensive conditions", DefensiveCondition.TargetNotGliding).visible(defensive::get);
    @AllArgsConstructor @Getter public enum DefensiveType implements EnumChoice {
        Random("Random vector"),
        Horizontal("Horizontal"),
        Unilateral("Unilateral"),
        Roll("Roll");
        final String renderName;
    }

    public EnumSetting<DefensiveType> defensiveType = enumSetting("Defensive Type", DefensiveType.Horizontal).desc("Flying off mode").visible(defensive::get);
    public SliderSetting strenghtRollSetting = sliderSetting("Strength Roll", 24f, 8f, 32f).increment(1f).visible(() -> defensiveType.is(DefensiveType.Roll)).visible(defensive::get);

    public CheckBox jitterOnDefensiveSetting = checkbox("Jitter on Defensive", true).visible(defensive::get);
    public TextSetting offsetsYOnDefensiveSetting = add(new TextSetting("Offsets on Y (separated by commas)", "-18").desc("Offsets")).visible(defensive::get);

    public CheckBox rangeByter = checkbox("Range byter", false).visible(defensive::get);

    // * Fake Rotation (BravoHvH в ахуе)
    private final CheckBox fakeRotationGroup = checkbox("Fake Rotation",false);
    public SliderSetting yawValue = sliderSetting("Yaw range", 3, 0, 90).increment(1).visible(fakeRotationGroup::get);
    public SliderSetting pitchValue = sliderSetting("Pitch range", 3, 0, 90).increment(1).visible(fakeRotationGroup::get);
    public CheckBox isFakeLag = checkbox("FakeLag", true);
    @AllArgsConstructor @Getter
    public enum FakeLagCondition implements EnumChoice {
        Always("Always"), TargetIsntGliding("Target isn't elytra flying"); final String renderName;
    }
    public EnumSetting<FakeLagCondition> ifWorkitFakeLagSetting = enumSetting("Conditions", FakeLagCondition.TargetIsntGliding).visible(isFakeLag::get);

    TimeUtility sendDelay = new TimeUtility();
    TimeUtility leaveTime = new TimeUtility();
    TimeUtility hitTargetTime = new TimeUtility();
    public TimeUtility antiAimTimer = new TimeUtility();
    public Angle defVec = new Angle(0,0);
    public float scale = 1f;
    public double prevY;
    public int missAmount = 0;

    public boolean hit = false;
    public boolean isd = false;
    public boolean antiAimIsActive = false;
    public boolean lastAntiaim = false;
    public Vec3d lastPosTarget;

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null) return;

            updateTarget();
            if (TargetsUtility.getTarget() == null) {
                antiAimIsActive = false;
                antiAimTimer.reset();
            } else {
                prevY = mc.player.getY();

                Vec3d getPosTarget = ElytraAuraResolve.getPointOnTarget(TargetsUtility.getTarget());
                if (TargetsUtility.getTarget() != null && mc.player.isGliding()) {
                    if (TargetsUtility.getTarget() instanceof AbstractClientPlayerEntity target) {
                        boolean isWeapon = target.getMainHandStack().getItem() instanceof AxeItem ||
                                target.getMainHandStack().getItem().equals(Items.NETHERITE_SWORD) ||
                                target.getMainHandStack().getItem().equals(Items.DIAMOND_SWORD) ||
                                target.getMainHandStack().getItem().equals(Items.GOLDEN_SWORD) ||
                                target.getMainHandStack().getItem().equals(Items.WOODEN_SWORD) ||
                                target.getMainHandStack().getItem().equals(Items.IRON_SWORD) ||
                                target.getMainHandStack().getItem().equals(Items.STONE_SWORD);

                        if (target.handSwinging)
                            leaveTime.reset();

                        if (target != TargetsUtility.getLastTarget())
                            missAmount = 0;

                        if (target.handSwinging && isWeapon && mc.player.getEntityPos().distanceTo(getPosTarget) < 4 && mc.player.hurtTime == 0 && hitTargetTime.reached(300, true)) {
                            Client.NOTIFIES.add(Text.of((target.getName()).copy().append(Text.of(" missed hit due resolver issue").copy().withColor(Color.RED.getRGB()))), IconUse.WARN, 5000);
                            missAmount += 1;
                        }
                    }
                }
                if (TargetsUtility.getTarget() != null && mc.player.isGliding()) {
                    LivingEntity entity = TargetsUtility.getTarget();
                    Angle vec2f = RotationUtility.calcRotateOnElytra(ElytraAuraResolve.getFinalTargetVector(entity, false), true);

                    float yaw = Client.ROTATION.getRotate().getYaw();
                    if (jitterOnDefensiveSetting.get() && antiAimIsActive) yaw += MathUtility.random(-20f, 20f);
                    if (defensiveType.is(DefensiveType.Roll) && antiAimIsActive) {
                        yaw += strenghtRollSetting.get() * (hit ? -1 : 1);
                    }

                    Angle rotVe = antiAimIsActive ? new Angle(defensiveType.is(DefensiveType.Roll) ? yaw : defVec.getYaw(), defVec.getPitch()) : vec2f;
                    Client.ROTATION.rotate(DefaultRotation.INSTANCE, rotVe, 1, false, MovementCorrection.STRICT, Priorities.ELYTRATARGET);
                }
                ElytraAuraUtility.useFireWork(TargetsUtility.getTarget());
                if (TargetsUtility.getTarget() != null) {
                    if (Resolver.INSTANCE.isEnabled() && BackTrackPosResolver.INSTANCE.isEnabled()) BackTrackPosResolver.INSTANCE.resolvePlayers();
                    ElytraAuraAttackHandler.updateAttack(TargetsUtility.getTarget());
                    if (Resolver.INSTANCE.isEnabled() && BackTrackPosResolver.INSTANCE.isEnabled()) BackTrackPosResolver.INSTANCE.restorePlayers();
                }

                lastPosTarget = ((ResolvedPositionEntity) TargetsUtility.getTarget()).hachclientport$getResolvedPos();
            }
        }

        if (event instanceof EventMove move) {
            if (fakeRotationGroup.get()) {
                if (!antiAimIsActive && mc.player.isGliding() && TargetsUtility.getTarget() != null) {
                    move.yaw = Client.ROTATION.getRotate().getYaw() + (yawValue.get() * (hit ? 1 : -1));
                    move.pitch = MathHelper.clamp(Client.ROTATION.getRotate().getPitch() + (pitchValue.get() *  (hit ? 1 : -1)), -90, 90);
                }
            }
        }

        if (event instanceof EventInput e) {
            if (autoStartSetting.get() && TargetsUtility.getTarget() != null) {
                e.setJump(mc.player.age % 3 == 0);
            }
        }

        if (event instanceof EventAttack attack) {
            if (mc.player == null || mc.world == null || TargetsUtility.getTarget() == null || attack.target != TargetsUtility.getTarget()) return;
            hit = new Random().nextBoolean();
            if (activeAntiAim(TargetsUtility.getTarget())) {
                if (!isLeave(TargetsUtility.getTarget())) {
                    if (attack.target == TargetsUtility.getTarget()) {
                        if (!isAirStack.get() || !InputUtil.isKeyPressed(window, bindAirStackSetting.getBind())) {
                            if (!antiAimIsActive) {
                                antiAimIsActive = true;
                                antiAimTimer.reset();
                                defVec.setYaw((float) ElytraAuraDef.updateAntiAimRotation(TargetsUtility.getTarget(), antiAimVectorYaw(),
                                        ElytraAuraUtility.parsePitchOffsets(offsetsYOnDefensiveSetting.getText())).x);
                                defVec.setPitch((float) ElytraAuraDef.updateAntiAimRotation(TargetsUtility.getTarget(), antiAimVectorYaw(),
                                        ElytraAuraUtility.parsePitchOffsets(offsetsYOnDefensiveSetting.getText())).y);
                                lastAntiaim = true;
                            }
                        }
                    }
                }
            }
        }

        if (event instanceof EventPostTick postTick) {
            if (mc.player == null || mc.world == null) return;
            if (TargetsUtility.getTarget() == null)
                updateTarget();
        }

        if (event instanceof EventTravel e) {
            if (isAirStack.get() && bindAirStackSetting.getBind() != -1) {
                boolean key = InputUtil.isKeyPressed(window, bindAirStackSetting.getBind());
                if (TargetsUtility.getTarget() != null && mc.player.isGliding() && mc.player.getEntityPos().getY() > TargetsUtility.getTarget().getEntityPos().getY()) {
                    float getDist = mc.player.distanceTo(TargetsUtility.getTarget());
                    LivingEntity target = TargetsUtility.getTarget();
                    boolean isWeapon = target.getMainHandStack().getItem() instanceof AxeItem ||
                            target.getMainHandStack().getItem().equals(Items.NETHERITE_SWORD) ||
                            target.getMainHandStack().getItem().equals(Items.DIAMOND_SWORD) ||
                            target.getMainHandStack().getItem().equals(Items.GOLDEN_SWORD) ||
                            target.getMainHandStack().getItem().equals(Items.WOODEN_SWORD) ||
                            target.getMainHandStack().getItem().equals(Items.IRON_SWORD) ||
                            target.getMainHandStack().getItem().equals(Items.STONE_SWORD);

                    if (autoAirStackSetting.get()) {
                        if (ifworkAitStack.get(Feature.IF_TARGET_IS_RANGE)) {
                            if (getDist < (ElytraAuraAttackHandler.attackDistance(TargetsUtility.getTarget())) && !ElytraAuraResolve.isStoyak(TargetsUtility.getTarget()) && !isLeave(target)) {
                                isd = true;
                                e.cancel();
                            } else {
                                isd = false;
                            }
                        }
                        if (ifworkAitStack.get(Feature.IF_TARGET_ON_STOYAK)) {
                            if (getDist > 6 && isWeapon && (lastPosTarget.distanceTo(((ResolvedPositionEntity) target).hachclientport$getResolvedPos()) < 0.5) && ElytraAuraResolve.isStoyak(TargetsUtility.getTarget()) && !target.isOnGround() && !isLeave(target)) {
                                isd = true;
                                e.cancel();
                            } else {
                                isd = false;
                            }
                        }
                    } else {
                        if (getDist <= ElytraAuraAttackHandler.attackDistance(target)) {
                            if (key) {
                                e.cancel();
                                isd = true;
                            } else {
                                isd = false;
                            }
                        }
                    }
                }
            }
        }


        if (event instanceof Event3D e) {
            LivingEntity target = TargetsUtility.getTarget();
            boolean hurt = target != null && target.hurtTime < 7;
            boolean isCoolDowncompelte = mc.player.getAttackCooldownProgress(1.5f) > 0.6f;


            if (target != null) {
                boolean isDone = aimWhenTargetAttack.get() ?
                        (isLeave(target) || ElytraAuraResolve.isStoyak(target)) && antiAimTimer.reached(antiAimDelayDelaySetting.getLong())
                                || ((((ResolvedPositionEntity) target).hachclientport$getResolvedPos().distanceTo(mc.player.getEyePos()) < target.getLerpedPos(mc.getRenderTickCounter().getTickProgress(true)).distanceTo(mc.player.getEyePos()) && target.isGliding()))
                        : smartDefensiveTimeSetting.get() ? hurt & isCoolDowncompelte : antiAimTimer.reached(antiAimDelayDelaySetting.getLong());
                if (antiAimIsActive && isDone) {
                    antiAimIsActive = false;
                    antiAimTimer.reset();
                }
            } else {
                antiAimIsActive = false;
            }

            if (target != null) {
                e.stack.push();
                Client.RENDERER.toCamera(e.stack);
                VertexConsumer consumer = e.buffer.getBuffer(ClientPipelines.OUTLINE);
                Vec3d pos = ElytraAuraResolve.getFinalTargetVector(target, false);

                Box box = new Box(pos.x - 0.1, pos.y - 0.1, pos.z - 0.1, pos.x + 0.1, pos.y + 0.1, pos.z + 0.1);
                VertexRendering.drawOutline(
                        e.stack,
                        consumer,
                        VoxelShapes.cuboid(box),
                        0.0, 0.0, 0.0,
                        0xFFFFFFFF,
                        1.0F
                );

                e.stack.pop();
            }
        }
        if (event instanceof EventBoost e) {
            if (TargetsUtility.getTarget() == null) return;
            if (rangeByter.get()) {
                LivingEntity entity = TargetsUtility.getTarget();
                double wrap = Math.atan2(mc.player.getZ() - entity.getZ(), mc.player.getX() - entity.getX());
                wrap += 4 / mc.player.getEntityPos().distanceTo(entity.getEntityPos());
                double x = entity.getX() + 5 * Math.cos(wrap);
                double z = entity.getZ() + 5 * Math.sin(wrap);

                double diffX = x - mc.player.getX();
                double diffZ = z - mc.player.getZ();
                double d1 = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90;

                e.yaw = (float) d1;
            }
        }

        // дефенсив
        if (!rangeByter.get())
            ElytraAuraDefensive.handleEvent(event);
    };

    public Vec2f[] antiAimVectorYaw() {
        if (TargetsUtility.getTarget() == null) {
            return new Vec2f[]{new Vec2f(0, 0)};
        }

        return getYawVectorByMode(defensiveType.get());
    }

    private Vec2f[] getYawVectorByMode(DefensiveType mode) {
        return switch (mode) {
            case DefensiveType.Random -> new Vec2f[]{
                    new Vec2f(180, 0), new Vec2f(135, 0),
                    new Vec2f(90, 0), new Vec2f(45, 0),
                    new Vec2f(-45, 0), new Vec2f(-90, 0),
                    new Vec2f(-135, 0), new Vec2f(0, 0)
            };
            case DefensiveType.Horizontal, DefensiveType.Roll -> new Vec2f[]{new Vec2f(360, 0)};
            case DefensiveType.Unilateral -> new Vec2f[]{new Vec2f(180, 0)};
        };
    }

    public boolean activeAntiAim(LivingEntity target) {
        if (target == null) return false;
        if (ifWorkAntiAimSetting.is(DefensiveCondition.TargetNotGliding)) {
            return !target.isGliding() || target.isOnGround() || ElytraAuraResolve.isStoyak(target) || PlayerUtility.isOnSolidGround(target, 1);
        } else {
            return true;
        }
    }

    public boolean isLeave(LivingEntity entity) {
        return targetIsLeave(entity);
    }

    public boolean targetIsLeave(LivingEntity entity) {
        if (entity == null) return false;
        return leaveTime.reached(1500) && entity.isGliding();
    }

    public void updateTarget() {
        if (TargetsUtility.getTarget() != null) {
        }
        TargetsUtility.find(attackRangeSetting.get() + preAttackRangeSetting.get(), TargetsUtility.Sort.Distance);
    }

    public void resetAll() {
        missAmount = 0;
        leaveTime.reset();
        hitTargetTime.reset();
        antiAimTimer.reset();
        antiAimIsActive = false;
        Client.TIMER = 1F;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        TargetsUtility.reset();
        resetAll();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        TargetsUtility.reset();
        resetAll();

        ElytraAuraDefensive.onDisable();

        Client.TIMER = 1F;
    }

    @AllArgsConstructor
    @Getter
    public enum Feature implements EnumChoice {
        IF_TARGET_ON_STOYAK("Target on Stoyak", true),
        IF_TARGET_IS_RANGE("Target is near", false);
        final String renderName;
        final boolean defaultEnabled;
    }
}