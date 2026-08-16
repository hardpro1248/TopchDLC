package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import com.mojang.authlib.GameProfile;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import java.util.UUID;

/**
 * Create by daun kvass
 */
public class FakePlayer extends Module {
    public static final FakePlayer INSTANCE = new FakePlayer();
    private OtherClientPlayerEntity fakePlayer;

    private FakePlayer() {
        super("FakePlayer", Category.Misc, "ставит тебя фейкового");
    }
    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) onUpdate();
    };
    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            this.toggle();
            return;
        }

        GameProfile profile = new GameProfile(UUID.randomUUID(), mc.player.getName().getString());
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
        fakePlayer.copyFrom(mc.player);

        fakePlayer.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        mc.world.addEntity(fakePlayer);
    }


    public void onUpdate() {

        if (fakePlayer == null) {
            this.toggle();
        }
    }

    @Override
    public void onDisable() {
        if (mc.world != null && fakePlayer != null) {
            mc.world.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
    }
}