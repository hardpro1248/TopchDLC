package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.HandShaderModule;
import gg.topchdlc.vse.utils.render.TextureExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(targets = "net.minecraft.client.render.command.OrderedRenderCommandQueueImpl")
public class OrderedRenderCommandQueueImplMixin {

    @ModifyVariable(
            method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private RenderLayer modifySubmitModelLayer(RenderLayer original) {
        if (HandShaderModule.isRenderingShader) {
            Identifier originalTex = TextureExtractor.getTexture(original);
            return HandShaderModule.getCustomShaderLayer(originalTex);
        }
        return original;
    }

    @ModifyVariable(
            method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2
    )
    private int modifySubmitModelTint(int original) {
        if (HandShaderModule.isRenderingShader) {
            return HandShaderModule.getCustomTint();
        }
        return original;
    }

    @ModifyVariable(
            method = "submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private RenderLayer modifySubmitModelPartLayer(RenderLayer original) {
        if (HandShaderModule.isRenderingShader) {
            Identifier originalTex = TextureExtractor.getTexture(original);
            return HandShaderModule.getCustomShaderLayer(originalTex);
        }
        return original;
    }

    @ModifyVariable(
            method = "submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2
    )
    private int modifySubmitModelPartTint(int original) {
        if (HandShaderModule.isRenderingShader) {
            return HandShaderModule.getCustomTint();
        }
        return original;
    }

    @ModifyVariable(
            method = "submitItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/item/ItemRenderState$Glint;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private RenderLayer modifySubmitItemLayer(RenderLayer original) {
        if (HandShaderModule.isRenderingShader) {
            Identifier originalTex = TextureExtractor.getTexture(original);
            return HandShaderModule.getCustomShaderLayer(originalTex);
        }
        return original;
    }
}