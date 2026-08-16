package gg.topchdlc.mixin.screen;

import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.MipmapStrategy;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;

/**
 * Create by daun kvass
 */
@Mixin(targets = "net.minecraft.client.gui.screen.SplashOverlay$LogoTexture")
public class LogoTextureMixin {

    @Inject(
            method = "loadContents(Lnet/minecraft/resource/ResourceManager;)Lnet/minecraft/client/texture/TextureContents;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void loadCustomLogo(ResourceManager resourceManager, CallbackInfoReturnable<TextureContents> cir) {
        String path = "/assets/topchdlc/images/ui/pic/cute/cutie2.png";
        System.out.println("LTM Попытка загрузить логотип " + path);

        try (InputStream inputStream = LogoTextureMixin.class.getResourceAsStream(path)) {
            if (inputStream != null) {
                NativeImage nativeImage = NativeImage.read(inputStream);

                TextureResourceMetadata metadata = new TextureResourceMetadata(true, true, MipmapStrategy.AUTO, 0.0F);

                System.out.println("LTM Логотип win загружен  " + nativeImage.getWidth() + "x" + nativeImage.getHeight());
                cir.setReturnValue(new TextureContents(nativeImage, metadata));
            } else {
                System.err.println("LTM  fail картинки не найден в classpath");
                System.err.println("LTM  лежит ли файл по пути");
            }
        } catch (IOException e) {
            System.err.println("LTM Ошибка чтении PNG ");
            e.printStackTrace();
        }
    }
}