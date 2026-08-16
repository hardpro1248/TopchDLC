package gg.topchdlc.mixin.fixes;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.server.GameProfileResolver;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Create by daun kvass
 */
@Mixin(targets = "net.minecraft.component.type.ProfileComponent$Dynamic")
public abstract class ProfileComponentDynamicMixin {

    @Shadow
    private Either<String, UUID> nameOrId;
    @Overwrite
    public CompletableFuture<GameProfile> resolve(GameProfileResolver resolver) {
        ProfileComponent self = (ProfileComponent) (Object) this;
        GameProfile currentProfile = self.getGameProfile();

        if (!currentProfile.properties().isEmpty()) {
            return CompletableFuture.completedFuture(currentProfile);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return resolver.getProfile(this.nameOrId)
                        .orElseGet(() -> new GameProfile(
                                Uuids.getOfflinePlayerUuid(currentProfile.name()),
                                currentProfile.name()
                        ));
            } catch (Exception e) {
                return currentProfile;
            }
        }, Util.getDownloadWorkerExecutor());
    }
}