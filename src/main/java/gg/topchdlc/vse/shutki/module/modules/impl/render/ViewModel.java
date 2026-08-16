package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public class ViewModel extends Module {
    public static final ViewModel INSTANCE = new ViewModel();

    public final SliderSetting rightX = sliderSetting("Right X", 0.0F, -2.0F, 2.0F).increment(0.01F);
    public final SliderSetting rightY = sliderSetting("Right Y", 0.0F, -2.0F, 2.0F).increment(0.01F);
    public final SliderSetting rightZ = sliderSetting("Right Z", 0.0F, -2.0F, 2.0F).increment(0.01F);
    public final SliderSetting leftX = sliderSetting("Left X", 0.0F, -2.0F, 2.0F).increment(0.01F);
    public final SliderSetting leftY = sliderSetting("Left Y", 0.0F, -2.0F, 2.0F).increment(0.01F);
    public final SliderSetting leftZ = sliderSetting("Left Z", 0.0F, -2.0F, 2.0F).increment(0.01F);
    public final SliderSetting rightYaw = sliderSetting("Right Yaw", 0.0F, -180.0F, 180.0F).increment(0.1F);
    public final SliderSetting rightPitch = sliderSetting("Right Pitch", 0.0F, -180.0F, 180.0F).increment(0.1F);
    public final SliderSetting rightRoll = sliderSetting("Right Roll", 0.0F, -180.0F, 180.0F).increment(0.1F);
    public final SliderSetting leftYaw = sliderSetting("Left Yaw", 0.0F, -180.0F, 180.0F).increment(0.1F);
    public final SliderSetting leftPitch = sliderSetting("Left Pitch", 0.0F, -180.0F, 180.0F).increment(0.1F);
    public final SliderSetting leftRoll = sliderSetting("Left Roll", 0.0F, -180.0F, 180.0F).increment(0.1F);
    public final CheckBox removeEatAnimation = checkbox("Убрать анимацию еды", true);

    private ViewModel() {
        super("ViewModel", Category.RENDER, "Настройка позиции рук в первом лице");
    }

    public void apply(Hand hand, Arm mainArm, net.minecraft.client.util.math.MatrixStack matrices) {
        if (!isEnabled()) return;

        boolean isRightHand = (hand == Hand.MAIN_HAND) == (mainArm == Arm.RIGHT);
        if (isRightHand) {
            matrices.translate(rightX.get(), rightY.get(), rightZ.get());
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rightPitch.get()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rightYaw.get()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rightRoll.get()));
        } else {
            matrices.translate(leftX.get(), leftY.get(), leftZ.get());
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leftPitch.get()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(leftYaw.get()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(leftRoll.get()));
        }
    }
}
