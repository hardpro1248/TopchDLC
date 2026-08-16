package gg.topchdlc.mixin.accessor;

import net.minecraft.entity.player.ItemCooldownManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCooldownManager.Entry.class)
public interface IItemCooldownEntry {
    @Accessor("startTick")
    int getStartTick();

    @Accessor("endTick")
    int getEndTick();
}