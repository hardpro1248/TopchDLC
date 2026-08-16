package gg.topchdlc.mixin.client;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.modules.impl.player.HitSound;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import gg.topchdlc.mixin.accessor.SourceAccessor;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;


@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Shadow
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Inject(method = "play", at = @At("RETURN"))
    private void onSoundPlay(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if (!Client.INITIALIZED || Client.RTX_ENGINE == null) return;
        
        try {
            Channel.SourceManager sourceManager = sources.get(sound);
            if (sourceManager != null) {
                sourceManager.run(source -> {
                    try {
                        int sourceId = ((SourceAccessor) source).rtx$getSourceId();
                        if (sourceId > 0) {
                            Client.RTX_ENGINE.getMixer().injectFiltersToChannel(sound, sourceId);
                        }
                    } catch (Exception e) {
                    }
                });
            }
        } catch (Exception e) {
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void onPlaySound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        Identifier id = sound.getId();
        if (!HitSound.INSTANCE.isEnabled()) return;
        //  var soundSettings = HitSound.INSTANCE.soundMultiEnumSettings;
        if (isHit(id) && HitSound.INSTANCE.soundMultiEnumSettings.get(HitSound.Sounds.muteoriginal)) {
            if (HitSound.INSTANCE.shouldApply()) {
                cir.cancel();
            }
        }
        if (!Removals.INSTANCE.isEnabled()) return;
        var soundSetting = Removals.INSTANCE.soundMultiEnumSetting;
        if (soundSetting.get(Removals.Sound.SoundWithering) && isWitherSound(id)) cir.cancel();
        if (soundSetting.get(Removals.Sound.SoundTRIDENT) && isTridentSound(id)) cir.cancel();
        if (soundSetting.get(Removals.Sound.SoundTotem) && isTotemSound(id)) cir.cancel();
        if (soundSetting.get(Removals.Sound.SoundExperience) && isExperienceOrbSound(id)) cir.cancel();

    }
    @Unique
    private boolean isHit(Identifier id) {
        String path = id.getPath();
        return path.contains("player") ||
              id.equals(Identifier.ofVanilla("entity.player.damage")) ||
                id.equals(Identifier.ofVanilla("entity.player.attack")) ||
                id.equals(Identifier.ofVanilla("entity.generic.hurt")) ||
              id.equals(Identifier.ofVanilla("entity.player.hurt")) ;
    }

    @Unique
    private boolean isWitherSound(Identifier id) {
        String path = id.getPath();
        return path.contains("wither") ||
                id.equals(Identifier.ofVanilla("entity.wither.ambient")) ||
                id.equals(Identifier.ofVanilla("entity.wither.shoot")) ||
                id.equals(Identifier.ofVanilla("entity.wither.death")) ||
                id.equals(Identifier.ofVanilla("entity.wither.hurt"));
    }

    @Unique
    private boolean isTotemSound(Identifier id) {
        return id.equals(Identifier.ofVanilla("item.totem.use"));
    }
    @Unique
    private boolean isTridentSound(Identifier id) {
        String path = id.getPath();
        return path.contains("trident") || id.equals(Identifier.ofVanilla("item.trident.hit"))
                || id.equals(Identifier.ofVanilla("item.trident.hit_ground"))
                || id.equals(Identifier.ofVanilla("item.trident.throw"))
                || id.equals(Identifier.ofVanilla("item.trident.return"));
    }

    @Unique
    private boolean isExperienceOrbSound(Identifier id) {
        String path = id.getPath();
        return path.contains("experience") || id.equals(Identifier.ofVanilla("entity.experience_orb.pickup")) || id.equals(Identifier.ofVanilla("entity.experience_bottle.throw"));
    }

}
