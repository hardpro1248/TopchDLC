package gg.topchdlc.mixin.accessor;

import net.minecraft.client.model.ModelPartData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelPartData.class)
public interface IModelPartData {
    @Accessor("children")
    Map<String, ModelPartData>  getChildren();
}
