package gg.topchdlc.mixin.accessor;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.TexturedModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TexturedModelData.class)
public interface ITexturedModelData {
    @Accessor("data")
    ModelData getData();
}
