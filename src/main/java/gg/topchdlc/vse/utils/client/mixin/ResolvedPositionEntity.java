package gg.topchdlc.vse.utils.client.mixin;

import gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver.BackTrackPosResolver;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public interface ResolvedPositionEntity {
    Vec3d hachclientport$getResolvedPos();

    double getPrevServerX();

    double getPrevServerY();

    double getPrevServerZ();

    List<BackTrackPosResolver.Position> getPositionHistory();

}