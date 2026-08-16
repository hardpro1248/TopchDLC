package gg.topchdlc.mixin.accessor;

import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisplayEntity.ItemDisplayEntity.class)
public interface IItemDisplayEntity {
    @Accessor("data")
    DisplayEntity.ItemDisplayEntity.Data client$data();
}
