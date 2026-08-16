package gg.topchdlc.mixin.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import gg.topchdlc.vse.utils.other.LogUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValueType;

import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Mixin(TranslationStorage.class)
public class TranslationStorageMixin {
    @Inject(method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resource/language/TranslationStorage;load(Ljava/lang/String;Ljava/util/List;Ljava/util/Map;)V"))
    private static void load(ResourceManager manager, List<Identifier> definitions, boolean rtl, CallbackInfoReturnable<?> cir,
                                        @Local Map<String, String> translations, @Local(ordinal = 0) String currentLangCode, @Local(ordinal = 2) String mod) {
        for (var resource : manager.getAllResources(Identifier.of(mod, "lang/" + currentLangCode + ".conf"))) {
            try (var is = resource.getInputStream()) {
                var config = ConfigFactory.parseReader(new InputStreamReader(is));
                var root = config.root();
                unwrap("", root, translations);
            } catch (IOException e) {
                LogUtility.LOGGER.error("couldn't read strings from {}/lang/{}.conf :(", mod, currentLangCode);
            }
        }
    }

    @Unique
    private static void unwrap(String prefix, ConfigObject object, Map<String, String> translations) {
        for (var entry : object.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            var newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
            if (value.valueType() == ConfigValueType.OBJECT) {
               unwrap(newPrefix, (ConfigObject) value, translations);
            } else if (value.valueType() == ConfigValueType.STRING) {
                translations.put(newPrefix, (String)value.unwrapped());
            }
        }
    }
}
