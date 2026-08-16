package gg.topchdlc.mixin.accessor;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntityRenderState.class)
public interface PlayerEntityRenderStateAccessor {
    @Accessor("capeVisible")
    void setCapeVisible(boolean capeVisible);
}
