package gg.topchdlc.mixin.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.Client;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.client.SkinManager;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class SkinTexturesMixin {

    @Unique
    private SkinTextures topchdlc$cachedTextures;
    @Unique
    private SkinTextures topchdlc$lastOriginalTextures;
    @Unique
    private String topchdlc$lastCapeId = "";
    @Unique
    private String topchdlc$lastModelId = "";

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void applyCustomCape(CallbackInfoReturnable<SkinTextures> cir) {
        if (!ClientSettings.INSTANCE.customizeSetting.cape.get()) return;

        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        boolean isMe = player.getGameProfile().name().equals(mc.getSession().getUsername());
        boolean isFriend = Client.INSTANCE.FRIENDS.isFriend(player);

        if (isMe || isFriend) {
            SkinTextures original = cir.getReturnValue();
            Identifier selectedCapeId = ClientSettings.INSTANCE.customizeSetting.capeMode.get().getIdentifier();

            AssetInfo.TextureAssetInfo modelSkin = null;
            if (isMe && (ClientSettings.INSTANCE.customizeSetting.customModel.get() || SkinManager.INSTANCE.getLoadedSkin() != null)) {
                Identifier loadedSkin = SkinManager.INSTANCE.getLoadedSkin();
                if (loadedSkin != null) {
                    modelSkin = new AssetInfo.TextureAssetInfo(loadedSkin, loadedSkin);
                } else {
                    Identifier modelId = ClientSettings.INSTANCE.customizeSetting.modelMode.get().getIdentifier();
                    if (modelId != null) modelSkin = new AssetInfo.TextureAssetInfo(modelId, modelId);
                }
            }
            String modelCacheKey = modelSkin == null ? "" : modelSkin.toString();

            if (topchdlc$cachedTextures == null || original != topchdlc$lastOriginalTextures || !topchdlc$lastCapeId.equals(selectedCapeId.toString()) || !topchdlc$lastModelId.equals(modelCacheKey)) {
                AssetInfo.TextureAssetInfo customCapeAsset = new AssetInfo.TextureAssetInfo(selectedCapeId, selectedCapeId);

                topchdlc$cachedTextures = new SkinTextures(
                        modelSkin != null ? modelSkin : original.body(),
                        customCapeAsset,
                        customCapeAsset,
                        original.model(),
                        original.secure()
                );
                topchdlc$lastOriginalTextures = original;
                topchdlc$lastCapeId = selectedCapeId.toString();
                topchdlc$lastModelId = modelCacheKey;
            }

            cir.setReturnValue(topchdlc$cachedTextures);
        }
    }
}