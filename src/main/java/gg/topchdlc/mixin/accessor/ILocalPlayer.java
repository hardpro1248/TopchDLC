package gg.topchdlc.mixin.accessor;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface ILocalPlayer {

    @Accessor("lastSprinting")
    boolean serverSprintState();
}
