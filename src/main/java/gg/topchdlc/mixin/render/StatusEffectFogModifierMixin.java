package gg.topchdlc.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StatusEffectFogModifier.class)
public abstract class StatusEffectFogModifierMixin {
    @Shadow
    public abstract RegistryEntry<StatusEffect> getStatusEffect();

    @ModifyReturnValue(method = "shouldApply", at = @At("RETURN"))
    private boolean hookApply(boolean original) {
        Removals removals = Removals.INSTANCE;
        if (!removals.isEnabled()) return original;
        if (getStatusEffect() == StatusEffects.BLINDNESS) return original && !removals.removals.get(Removals.Removal.Blindness);
        if (getStatusEffect() == StatusEffects.DARKNESS) return original && !removals.removals.get(Removals.Removal.Darkness);
        return original;
    }
}
