package gg.topchdlc.vse.utils.client.mixin;

import gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver.BackTrackPosResolver;

public interface IOtherClientPlayerEntity {
    void resolve(BackTrackPosResolver.Resolve mode);

    void releaseResolver();
}
