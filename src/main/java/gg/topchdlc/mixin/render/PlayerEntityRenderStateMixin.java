package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.utils.client.mixin.IPlayerEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements IPlayerEntityRenderState {
    @Unique
    private PlayerEntity topchdlc$entity;

    @Override
    public PlayerEntity topchdlc$getEntity() {
        return topchdlc$entity;
    }

    @Override
    public void topchdlc$setEntity(PlayerEntity entity) {
        topchdlc$entity = entity;
    }
}
