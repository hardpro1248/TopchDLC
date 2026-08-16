package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.AuraModule;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.point.PointSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector2f;

public class SwingAnimations extends Module {
    public static final SwingAnimations INSTANCE = new SwingAnimations();

    public enum Mode {
        Standard("Стандарт"),
        Whack("Взмах"),
        Whack2("Взмах 2"),
        Shift("Сдвиг"),
        Break("Ломание"),
        Lunge("Выпад"),
        Spear("Копьё"),
        Down("Вниз"),
        Jelly("Желе"),
        Shake("Тряска");

        private final String renderName;

        Mode(String renderName) {
            this.renderName = renderName;
        }

        @Override
        public String toString() {
            return renderName;
        }
    }

    public final Group animation = this.group("Animation").toggleable(true);
    public final EnumSetting<Mode> mode;
    public final CheckBox translate;
    public final SliderSetting turn;
    public final SliderSetting speed;
    public final CheckBox strengthIncline;
    public final SliderSetting inclineStrength;
    public final CheckBox item360;

    public final Group check = this.group("Check").toggleable(true);
    public final CheckBox attackAuraOnly;
    public final CheckBox attackAuraOffsetsOnly;

    public final Group hands = this.group("Позиции рук").toggleable(true);
    public final PointSetting rightHandPos;
    public final SliderSetting rightHandZ;
    public final PointSetting leftHandPos;
    public final SliderSetting leftHandZ;

    private long lastAuraAttack = -1;

    public final EventBus<Event> bus = event -> {
        if (event instanceof EventAttack attack && AuraModule.INSTANCE.isEnabled()) {
            if (attack.target != null && attack.target == TargetsUtility.getTarget()) {
                lastAuraAttack = System.currentTimeMillis();
            }
        }
    };

    private SwingAnimations() {
        super("SwingAnimations", Category.RENDER, "анимации взмаха рукой");
        this.mode = this.animation.enumSetting("Режим", Mode.Standard);
        this.translate = this.animation.checkbox("Транслейт", true);
        this.turn = this.animation.sliderSetting("Поворот", 0.0F, -90.0F, 90.0F).increment(1.0F);
        this.speed = this.animation.sliderSetting("Скорость", 10.0F, 1.0F, 20.0F);
        this.strengthIncline = this.animation.checkbox("Уклон по силе", false);
        this.inclineStrength = this.animation.sliderSetting("Сила уклона", 10.0F, 0.0F, 20.0F).increment(0.5F).visible(this.strengthIncline::get);
        this.item360 = this.animation.checkbox("Item 360", false);

        this.attackAuraOnly = this.check.checkbox("Attack Aura Only", false);
        this.attackAuraOffsetsOnly = this.check.checkbox("Attack Aura Offsets Only", false);

        this.rightHandPos = this.hands.pointSetting("Позиция правой руки", new Vector2f(0.0F, 0.0F));
        this.rightHandZ = this.hands.sliderSetting("Z правой руки", 0.0F, -2.0F, 2.0F).increment(0.05F);
        this.leftHandPos = this.hands.pointSetting("Позиция левой руки", new Vector2f(0.0F, 0.0F));
        this.leftHandZ = this.hands.sliderSetting("Z левой руки", 0.0F, -2.0F, 2.0F).increment(0.05F);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Client.EVENTS.register(bus);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Client.EVENTS.unregister(bus);
    }

    public boolean isAuraAttacking() {
        if (!AuraModule.INSTANCE.isEnabled()) return false;
        return lastAuraAttack != -1 && System.currentTimeMillis() - lastAuraAttack <= 500;
    }

    public boolean shouldApply() {
        if (!isEnabled()) return false;
        if (attackAuraOnly.get()) return isAuraAttacking();
        return true;
    }

    public boolean shouldApplyOffsets() {
        return !attackAuraOffsetsOnly.get() || isAuraAttacking();
    }

    public boolean handleMatrix(float swingProgress, float equipProgress, MatrixStack matrices, float side, Arm arm) {
        if (!shouldApply()) return false;

        boolean isRight = arm == Arm.RIGHT;

        if (translate.get() && hands.isEnabled() && shouldApplyOffsets()) {
            Vector2f pos = isRight ? rightHandPos.get() : leftHandPos.get();
            float z = isRight ? rightHandZ.get() : -leftHandZ.get();
            matrices.translate(pos.x * side, pos.y, z);
        }

        matrices.translate(side * 0.56F, -0.52F, -0.72F);

        float turnAmount = turn.get();
        if (turnAmount != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(turnAmount * side));
        }

        float factor = strengthFactor();

        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);

        switch (mode.get()) {
            case Standard:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + f * -20.0F * factor)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * g * -20.0F * factor));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F * factor));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
                break;
            case Whack:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 45.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-160.0F + g * 170.0F * factor));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * g * -30.0F * factor));
                matrices.translate(0.0F, g * 0.12F * factor, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
                break;
            case Whack2:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(120.0F - g * 140.0F * factor));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * g * 30.0F * factor));
                matrices.translate(0.0F, -g * 0.12F * factor, 0.0F);
                break;
            case Shift:
                matrices.translate(side * (0.3F - f * 0.7F * factor), -0.1F + f * 0.2F * factor, -0.05F - f * 0.3F * factor);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -(45.0F + f * 20.0F * factor)));
                break;
            case Break:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 45.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * (20.0F - f * 40.0F * factor)));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F - g * 70.0F * factor));
                break;
            case Lunge:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 45.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -60.0F * factor));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -15.0F));
                matrices.translate(0.0F, -g * 0.2F * factor, g * 0.55F * factor);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * -45.0F));
                break;
            case Spear:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 45.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -75.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F - f * 45.0F * factor));
                matrices.translate(0.0F, f * 0.1F, -f * 0.3F);
                break;
            case Down:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 45.0F));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F - g * 15.0F * factor));
                matrices.translate(0.0F, -f * 0.6F * factor, 0.0F);
                break;
            case Jelly:
                float wob = MathHelper.sin(swingProgress * (float) Math.PI * 4.0F);
                matrices.scale(1.0F + g * 0.15F * factor, 1.0F - g * 0.15F * factor, 1.0F + g * 0.2F * factor);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(wob * 12.0F * factor));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wob * 8.0F * factor));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 45.0F));
                break;
            case Shake:
                float wob2 = MathHelper.sin(swingProgress * (float) Math.PI * 8.0F) * (1.0F - swingProgress);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * (45.0F + wob2 * 20.0F * factor)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(wob2 * 15.0F * side * factor));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F + wob2 * 25.0F * factor));
                matrices.translate(wob2 * 0.05F, 0.0F, 0.0F);
                break;
        }

        if (item360.get()) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingProgress * 360.0F));
        }

        return true;
    }

    private float strengthFactor() {
        if (!strengthIncline.get()) return 1.0F;
        float cooldown = 0.5F;
        if (mc.player != null) cooldown = Math.max(0.2F, mc.player.getAttackCooldownProgress(0));
        return cooldown * (inclineStrength.get() / 10.0F);
    }
}