package gg.topchdlc.mixin.accessor;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface IKeyBinding {
    @Accessor("boundKey")
    InputUtil.Key client$boundKey();
    @Invoker("reset")
    void client$reset();

    @Accessor("KEYS_BY_ID")
    static Map<String, KeyBinding> client$getKeysById() {
        throw new AssertionError("ugh");
    }
}
