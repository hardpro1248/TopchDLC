package gg.topchdlc.mixin.client;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventBoost;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.utils.client.mixin.IShooterEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin implements IShooterEntity {
    @Shadow private @Nullable LivingEntity shooter;

    @Unique
    public LivingEntity shooterEntity = null;

    @Override @Unique
    public LivingEntity client$getShooter() {
        return shooterEntity;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V", ordinal = 0))
    private void onSetVelocity(LivingEntity instance, Vec3d origin) {
        this.shooterEntity = shooter;
        if (Client.IS_PANIC) {
            instance.setVelocity(origin);
            return;
        }
        
        if (instance instanceof ClientPlayerEntity) {
            EventBoost boost = EventBoost.build(Client.ROTATION.getRotate().getYaw(), Client.ROTATION.getRotate().getPitch());
            Client.EVENTS.post(boost);

            Vec3d vec3d = new Angle(boost.yaw, boost.pitch).toVector();
            Vec3d vec3d2 = instance.getVelocity();
            instance
                    .setVelocity(
                            vec3d2.add(
                                    vec3d.x * boost.a + (vec3d.x * boost.speedx - vec3d2.x) * boost.b,
                                    vec3d.y * boost.a + (vec3d.y * boost.speedy - vec3d2.y) * boost.b,
                                    vec3d.z * boost.a + (vec3d.z * boost.speedz - vec3d2.z) * boost.b
                            )
                    );
            return;
        }
        instance.setVelocity(origin);
    }
}
