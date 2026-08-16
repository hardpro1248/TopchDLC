package gg.topchdlc.mixin.client;

import gg.topchdlc.mixin.accessor.IModelPartData;
import gg.topchdlc.mixin.accessor.ITexturedModelData;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EntityModels.class)
public class EntityModelsMixin {
    @Inject(method = "getModels", at = @At("TAIL"))
    private static void getModels(CallbackInfoReturnable<Map<EntityModelLayer, TexturedModelData>> cir) {
        for (Map.Entry<EntityModelLayer, TexturedModelData> entry : cir.getReturnValue().entrySet()) {
            EntityModelLayer layer = entry.getKey();
            ModelData data = ((ITexturedModelData) entry.getValue()).getData();
            ModelPartData root = data.getRoot();
            Map<String, ModelPartData> s = ((IModelPartData) (Object) root).getChildren();

            for (Map.Entry<String, ModelPartData> entry2 : s.entrySet()) {
                ModelPartData partData = entry2.getValue();
//                partData. // бля заебало
            }
        }
    }
}
