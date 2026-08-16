package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver.BackTrackPosResolver;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver.ResolverMode;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.MultiChoice;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPositionEntity;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

public class Resolver extends Module {
    private Resolver() {
        super("Resolver", Category.COMBAT, "резолвит позициую цели на указаную");
    }
    public static final Resolver INSTANCE = new Resolver();
    private final MultiChoice<ResolverMode> resolvers = multiChoice( BackTrackPosResolver.INSTANCE);

    private final CheckBox drawReal = checkbox("Show real position", false);

    public Vec3d resolveAura(Vec3d origin, float cooldown) {
        if (!isEnabled()) return origin;
        return resolvers.reduce(origin, (resolver, point) -> resolver.resolveAura(point, cooldown));
    }

    public Vec3d resolveElytra(Vec3d origin, float cooldown) {
        if (!isEnabled()) return origin;
        return resolvers.reduce(origin, (resolver, point) -> resolver.resolveElytra(point, cooldown));
    }

    public boolean isFakeLagging() {
        if (!isEnabled()) return false;
        for (ResolverMode resolver : resolvers.get()) {
            if (resolver.isEnabled() && resolver.isFakeLagging()) return true;
        }
        return false;
    }

    EventBus<Event> events = event -> {
        resolvers.onEvent(event);

        if (event instanceof Event3D e && drawReal.get() && TargetsUtility.getTarget() != null) {
            e.stack.push();
            Client.RENDERER.toCamera(e.stack);

            LivingEntity target = TargetsUtility.getTarget();
            Box box = target.getBoundingBox().offset(((ResolvedPositionEntity)target).hachclientport$getResolvedPos().subtract(target.getEntityPos()));
            VertexRendering.drawOutline(e.stack, e.buffer.getBuffer(RenderLayers.lines()), VoxelShapes.cuboid(box), 1, 1, 1, 1,1);

            e.stack.pop();
        }
    };
}
