package gg.topchdlc.mixin.accessor;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/**
 * Create by daun kvass
 */
@Mixin(Input.class)
public interface InputAccessor {
    @Accessor("movementVector")
    void setMovementVector(Vec2f movementVector);

    @Accessor("movementVector")
    Vec2f getMovementVector();
}