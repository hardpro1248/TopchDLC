package gg.topchdlc.mixin.accessor;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(ItemCooldownManager.class)
public interface IItemCooldownManager {
    @Accessor("entries")
    Map<Object, ?> getEntries();

    @Accessor("tick")
    int getTick();
}