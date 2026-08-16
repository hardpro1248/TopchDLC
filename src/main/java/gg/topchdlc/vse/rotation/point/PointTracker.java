package gg.topchdlc.vse.rotation.point;

import gg.topchdlc.Client;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.impl.Builder;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.function.Function;
import static gg.topchdlc.vse.utils.math.MathUtility.*;

/**
 * Create by daun kvass
 */
public class PointTracker extends Group {

    public PointTracker() {
        super("Point Tracker");
    }

    final EnumSetting<Preset> preset = enumSetting("Preset", Preset.Default);
    final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Multi).visible(() -> preset.is(Preset.Custom));
    final EnumSetting<Point> high = enumSetting("High", Point.Head).visible(() -> preset.is(Preset.Custom));
    final EnumSetting<Point> low = enumSetting("Low", Point.Feet).visible(() -> preset.is(Preset.Custom));
    final SliderSetting shrinkBox = sliderSetting("Shrink box", 0.05f, 0, 0.3f).increment(0.01f).visible(() -> preset.is(Preset.Custom));

    private static final Mode DEFAULT_MODE = Mode.Multi;
    private static final Point DEFAULT_HIGH = Point.Head;
    private static final Point DEFAULT_LOW = Point.Feet;
    private static final float DEFAULT_SHRINK = 0.05f;

    public Vec3d getPoint(Entity target) {
        if (target == null || mc.player == null) return Vec3d.ZERO;

        if (Client.ROTATION != null && Client.ROTATION.getExecutor() instanceof Builder) {
            return Builder.getPointForTarget(target);
        }

        Mode activeMode = preset.is(Preset.Custom) ? mode.get() : DEFAULT_MODE;

        if (activeMode == Mode.Builder) {
            return Builder.getPointForTarget(target);
        }

        Point activeHigh = preset.is(Preset.Custom) ? high.get() : DEFAULT_HIGH;
        Point activeLow = preset.is(Preset.Custom) ? low.get() : DEFAULT_LOW;
        float activeShrink = preset.is(Preset.Custom) ? shrinkBox.get() : DEFAULT_SHRINK;

        if (activeLow.isHigherThan(activeHigh)) {
            activeHigh = activeLow;
        }

        Box box = target.getBoundingBox().expand(-activeShrink);

        double minY = box.minY + activeLow.getOffset.apply(target);
        double maxY = box.maxY - (target.getHeight() - activeHigh.getOffset.apply(target));

        if (minY > maxY) {
            double temp = minY;
            minY = maxY;
            maxY = temp;
        }

        Box offsetBox = new Box(box.minX, minY, box.minZ, box.maxX, maxY, box.maxZ);
        return activeMode.getPoint(offsetBox, mc.player.getEyePos());
    }

    @AllArgsConstructor
    enum Preset {
        Default, Custom
    }

    @AllArgsConstructor
    @Getter
    enum Point {
        Head(entity -> entity.getEyeHeight(entity.getPose())),
        Body(entity -> entity.getHeight() * 0.5f),
        Feet(entity -> 0f);

        final Function<Entity, Float> getOffset;

        public boolean isHigherThan(Point other) {
            return ordinal() < other.ordinal();
        }
    }

    @AllArgsConstructor
    @Getter
    public enum Mode {
        Random((box, eyes) -> new Vec3d(
                random((float) box.minX, (float) box.maxX),
                random((float) box.minY, (float) box.maxY),
                random((float) box.minZ, (float) box.maxZ)
        )),
        Closest((box, eyes) -> MathUtility.clampToBox(eyes, box)),
        Assist((box, eyes) -> MathUtility.clampToBox(eyes.add(Angle.fromPlayer().toVector()), box)),
        Multi(UBoxPoints::getBestVector3dOnEntityBox),
        Builder((box, eyes) -> gg.topchdlc.vse.rotation.all.impl.Builder.getPointForTarget(null));

        final GetPoint getPoint;

        public Vec3d getPoint(Box box, Vec3d eyes) {
            return getPoint.getPoint(box, eyes);
        }
    }

    @FunctionalInterface
    interface GetPoint {
        Vec3d getPoint(Box box, Vec3d eyes);
    }
}