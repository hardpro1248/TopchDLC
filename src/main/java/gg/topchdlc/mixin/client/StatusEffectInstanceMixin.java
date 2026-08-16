package gg.topchdlc.mixin.client;

import gg.topchdlc.vse.utils.client.mixin.IStatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(StatusEffectInstance.class)
public class StatusEffectInstanceMixin implements IStatusEffectInstance {
    @Unique
    float lerped = 0;

    @Override
    public float getLerp() {
        return lerped;
    }

    @Override
    public void setLerp(float value) {
        lerped = value;
    }
}
