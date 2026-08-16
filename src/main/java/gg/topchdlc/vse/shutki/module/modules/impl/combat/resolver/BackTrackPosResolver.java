package gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver;

import gg.topchdlc.vse.shutki.module.modules.impl.combat.Resolver;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.mixin.IOtherClientPlayerEntity;
import lombok.Data;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

public class BackTrackPosResolver extends ResolverMode {
    public static final BackTrackPosResolver INSTANCE = new BackTrackPosResolver();
    private BackTrackPosResolver() {
        super("BackTrack Pos");
        toggleable(false);
    }

    public EnumSetting<Resolve> type = enumSetting("Type Resolve", Resolve.BackTrack);
    public SliderSetting backTicks = sliderSetting("Ticks", 15, 1, 50).increment(1f)
            .visible(() -> type.is(Resolve.BackTrack));

    public void resolvePlayers() {
        if (Resolver.INSTANCE.isEnabled() && isEnabled())
            for (PlayerEntity player : mc.world.getPlayers())
                if (player instanceof OtherClientPlayerEntity)
                    ((IOtherClientPlayerEntity) player).resolve(type.get());
    }

    public void restorePlayers() {
        if (Resolver.INSTANCE.isEnabled() && isEnabled())
            for (PlayerEntity player : mc.world.getPlayers())
                if (player instanceof OtherClientPlayerEntity)
                    ((IOtherClientPlayerEntity) player).releaseResolver();
    }

    public enum Resolve {
        BackTrack,
        Advantage,
        Predictive
    }

    @Data
    public static class Position {
        private double x, y, z;
        private int ticks;

        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public boolean shouldRemove() {
            return ticks++ > BackTrackPosResolver.INSTANCE.backTicks.get();
        }
    }
}
