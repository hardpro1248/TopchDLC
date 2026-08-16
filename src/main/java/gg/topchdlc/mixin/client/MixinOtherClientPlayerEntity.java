package gg.topchdlc.mixin.client;

import com.mojang.authlib.GameProfile;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver.BackTrackPosResolver;
import gg.topchdlc.vse.utils.client.mixin.IOtherClientPlayerEntity;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPositionEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(OtherClientPlayerEntity.class)
public class MixinOtherClientPlayerEntity extends AbstractClientPlayerEntity implements IOtherClientPlayerEntity {
    @Unique private double backUpX, backUpY, backUpZ;

    public MixinOtherClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Unique
    Vec3d lastServer = Vec3d.ZERO;

    public void resolve(BackTrackPosResolver.Resolve mode) {
        backUpX = getX();
        backUpY = getY();
        backUpZ = getZ();

        if(mode == BackTrackPosResolver.Resolve.BackTrack) {
            double minDst = 999d;
            BackTrackPosResolver.Position bestPos = null;
            for (BackTrackPosResolver.Position p : ((ResolvedPositionEntity) this).getPositionHistory()) {
                double dst = mc.player.squaredDistanceTo(p.getX(), p.getY(), p.getZ());
                if (dst < minDst) {
                    minDst = dst;
                    bestPos = p;
                }
            }

            if(bestPos != null) {
                setPosition(bestPos.getX(), bestPos.getY(), bestPos.getZ());
            }
            return;
        }

        Vec3d from = lastServer;
        Vec3d to = ((ResolvedPositionEntity) this).hachclientport$getResolvedPos();

        if (mode == BackTrackPosResolver.Resolve.Advantage) {
            if (mc.player.squaredDistanceTo(from) > mc.player.squaredDistanceTo(to)) setPosition(to.x, to.y, to.z);
            else setPosition(from.x, from.y, from.z);
        } else {
            setPosition(to.x, to.y, to.z);
        }

        lastServer = to;
    }

    public void releaseResolver() {
        if (backUpY != -999) {
            setPosition(backUpX, backUpY, backUpZ);
            backUpY = -999;
        }
    }
}