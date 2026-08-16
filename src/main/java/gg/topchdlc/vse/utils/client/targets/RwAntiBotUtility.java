package gg.topchdlc.vse.utils.client.targets;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

@UtilityClass
public class RwAntiBotUtility implements MinecraftHolder {
    public boolean isRwBot(PlayerEntity p) {
        if (p == null || mc == null) return false;
        if (p instanceof AbstractClientPlayerEntity pl) {
            return pl.getGameProfile().properties().isEmpty()|| !NetworkUtility.offlineUUID(pl.getGameProfile().name()).equals(pl.getUuid());
        }
        return false;
    }
}
