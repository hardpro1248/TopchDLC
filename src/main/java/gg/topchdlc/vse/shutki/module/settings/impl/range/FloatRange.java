package gg.topchdlc.vse.shutki.module.settings.impl.range;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.minecraft.util.math.MathHelper;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
public final class FloatRange {
    public float min, max;

    public void set(float min, float max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public float clamp(float value) {
        return MathHelper.clamp(value, min, max);
    }
}
