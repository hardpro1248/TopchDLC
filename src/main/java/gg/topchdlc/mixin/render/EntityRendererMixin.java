package gg.topchdlc.mixin.render;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.modules.impl.render.ESP;
import gg.topchdlc.vse.shutki.module.modules.impl.render.ItemEsp;
import gg.topchdlc.vse.shutki.module.modules.impl.render.ProjectilesNameTag;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void hookNametag(T entity, CallbackInfoReturnable<Text> cir) {
        if (ESP.INSTANCE.isEnabled() && ESP.INSTANCE.dontRenderNametag(entity)) cir.setReturnValue(null);
        if(ItemEsp.INSTANCE.isEnabled() && ItemEsp.INSTANCE.dontRenderNametag(entity)) cir.setReturnValue(null);
        if(ProjectilesNameTag.INSTANCE.isEnabled() && ProjectilesNameTag.INSTANCE.dontRenderNametag(entity)) cir.setReturnValue(null);

        // Логотип "Т" возле ника игроков, которые играют с клиентом
        if (entity instanceof PlayerEntity player && Client.CLIENT_USERS != null && Client.CLIENT_USERS.isUser(player)) {
            Text original = cir.getReturnValue();
            if (original != null) {
                cir.setReturnValue(original.copy().append(Text.literal(" §7[§bТ§7]")));
            }
        }
    }


    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void entityRemoval(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.doesNotRenderEntity(entity)) {
            cir.setReturnValue(false);
        }
    }
}
