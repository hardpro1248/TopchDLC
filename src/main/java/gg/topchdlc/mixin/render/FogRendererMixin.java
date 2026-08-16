package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.CustomWorld;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogModifier;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Create by daun kvass
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Shadow @Final private MappableRingBuffer fogBuffer;
    @Shadow @Final private static List<FogModifier> FOG_MODIFIERS;


    // Цвет фога типо мелстрой ам ам
    @Inject(method = "getFogColor", at = @At("TAIL"), cancellable = true)
    private void topchdlc$getFogColor(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        if (CustomWorld.INSTANCE.isEnabled() && CustomWorld.INSTANCE.useFog.get()) {
            Color c = CustomWorld.INSTANCE.fogColor.get();
            cir.setReturnValue(new Vector4f(
                    c.getRed() / 255F, c.getGreen() / 255F,
                    c.getBlue() / 255F, c.getAlpha() / 255F));
        }
    }

    @Inject(
        method = "applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void topchdlc$applyFogRaw(ByteBuffer buffer, int bufPos, Vector4f fogColor,
                                   float environmentalStart, float environmentalEnd,
                                   float renderDistanceStart, float renderDistanceEnd,
                                   float skyEnd, float cloudEnd,
                                   CallbackInfo ci) {
        CustomWorld cw = CustomWorld.INSTANCE;
        if (!cw.isEnabled() || !cw.useFog.get()) return;

        ci.cancel();

        float start = cw.fogStart.get();
        float end   = cw.fogEnd.get();
        if (start > end) start = end;

        Color c = cw.fogColor.get();
        float r = c.getRed()   / 255F;
        float g = c.getGreen() / 255F;
        float b = c.getBlue()  / 255F;
        float a = c.getAlpha() / 255F;
        buffer.putFloat(bufPos,      r);
        buffer.putFloat(bufPos + 4,  g);
        buffer.putFloat(bufPos + 8,  b);
        buffer.putFloat(bufPos + 12, a);
        buffer.putFloat(bufPos + 16, start);
        buffer.putFloat(bufPos + 20, end);
        buffer.putFloat(bufPos + 24, start);
        buffer.putFloat(bufPos + 28, end);
        buffer.putFloat(bufPos + 32, end);
        buffer.putFloat(bufPos + 36, end);
    }
}
