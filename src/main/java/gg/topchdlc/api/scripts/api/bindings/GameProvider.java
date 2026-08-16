package gg.topchdlc.api.scripts.api.bindings;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPositionEntity;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPotationEntity;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec2f;
import org.graalvm.polyglot.HostAccess;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class GameProvider implements MinecraftHolder {
    @HostAccess.Export
    public Vector3f getServerPos(Entity e) {
        return ((ResolvedPositionEntity)e).hachclientport$getResolvedPos().toVector3f();
    }
    @HostAccess.Export
    public Vector3f getPos(Entity e) { return e.getLerpedPos(mc.getRenderTickCounter().getTickProgress(true)).toVector3f(); }
    @HostAccess.Export
    public Vector2f getServerRot(Entity e) {
        Vec2f v = ((ResolvedPotationEntity)e).hachclientport$getResolvedRot();
        return new Vector2f(v.x, v.y);
    }
    @HostAccess.Export
    public ClientPlayerEntity getLocal() {
        return mc.player;
    }
    @HostAccess.Export
    public LivingEntity getTarget() {
        return TargetsUtility.getTarget();
    }
    @HostAccess.Export
    public LivingEntity getLastTarget() {
        return TargetsUtility.getLastTarget();
    }
    @HostAccess.Export
    public void send(String msg) {
        ChatUtility.send(msg);
    }
}
