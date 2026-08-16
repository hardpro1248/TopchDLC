package gg.topchdlc.api.scripts.api.bindings;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.scripts.Script;
import gg.topchdlc.api.scripts.api.bindings.support.AngleScripted;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RayTraceUtility;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.graalvm.polyglot.HostAccess;

import java.util.ArrayList;

public class ClientProvider implements MinecraftHolder {
    final ArrayList<ModuleProvider> modules = new ArrayList<>();

    Script script;
    public ClientProvider(Script script) {
        this.script = script;
    }

    @HostAccess.Export
    public ArrayList<ModuleProvider> getModules() {
        if (modules.size() != Client.MODULES.getModules().size()) {
            modules.clear();
            for (Module module : Client.MODULES.getModules()) {
                modules.add(new ModuleProvider(module));
            }
        }
        return modules;
    }
    @HostAccess.Export
    public void rotateTo(AngleScripted angle) {
        Client.ROTATION.rotate(DefaultRotation.INSTANCE, new Angle(angle.yaw(), angle.pitch()), 1, true, MovementCorrection.STRICT, Priorities.NORMAL);
    }
    @HostAccess.Export
    public AngleScripted localRotation() {
        if (mc.player == null) return new AngleScripted(0, 0);
        return new AngleScripted(mc.player.getYaw(), mc.player.getPitch());
    }
    @HostAccess.Export
    public AngleScripted rotation() {
        if (mc.player == null) return new AngleScripted(0, 0);
        Angle client = Client.ROTATION.getRotate();
        return new AngleScripted(client.getYaw(), client.getPitch());
    }
    @HostAccess.Export
    public void settings(Setting<?>... settings) {
        this.script.getScriptedModule().addAll(settings);
    }

    @HostAccess.Export
    public boolean traceEntity(AngleScripted vec, float distance, Entity entity, boolean walls) {
        if (walls && RayTraceUtility.raycast(mc.player.getEyePos(), distance, new Angle(vec.yaw(), vec.pitch()), false).getType() == HitResult.Type.BLOCK) return false;
        return RayTraceUtility.rayTrace(new Vec3d(vec.toVector()), distance, entity.getBoundingBox());
    }
}
