package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.CustomWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.WeightedAttributeList;
import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldEnvironmentAttributeAccess.class)
public class WorldEnvironmentAttributeAccessMixin {

    @SuppressWarnings("unchecked")
    @Inject(method = "getAttributeValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/attribute/WeightedAttributeList;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
    private <Value> void onGetAttributeValue(EnvironmentAttribute<Value> attribute, Vec3d pos, @Nullable WeightedAttributeList pool, CallbackInfoReturnable<Value> cir) {
        if (attribute == EnvironmentAttributes.SKY_COLOR_VISUAL) {
            Integer customColor = vitalya$getModifiedSkyColor();
            if (customColor != null) {
                cir.setReturnValue((Value) customColor);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "getAttributeValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
    private <Value> void onGetAttributeValueNoPos(EnvironmentAttribute<Value> attribute, CallbackInfoReturnable<Value> cir) {
        if (attribute == EnvironmentAttributes.SKY_COLOR_VISUAL) {
            Integer customColor = vitalya$getModifiedSkyColor();
            if (customColor != null) {
                cir.setReturnValue((Value) customColor);
            }
        }
    }

    @Unique
    private Integer vitalya$getModifiedSkyColor() {
        if (CustomWorld.INSTANCE.isEnabled() && CustomWorld.INSTANCE.skyx.get()) {
            return CustomWorld.INSTANCE.getCustomSkyColor();
        }

        if (CustomWorld.INSTANCE.isEnabled() && CustomWorld.INSTANCE.useFog.get()) {
            return CustomWorld.INSTANCE.fogColor.get().getRGB();
        }
        return null;
    }
}