package gg.topchdlc.mixin.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;

/**
 * Create by daun kvass
 */
@Mixin(ParticleManager.class)
public class MixinParticleManager {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("RETURN"),
            cancellable = true)
    private void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        Particle particle = cir.getReturnValue();
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.Bubbles)) {
            if (//parameters.getType() == ParticleTypes.BUBBLE ||
                   // parameters.getType() == ParticleTypes.BUBBLE_POP ||
                    parameters.getType() == ParticleTypes.BUBBLE_COLUMN_UP ||
                    parameters.getType() == ParticleTypes.CURRENT_DOWN) {
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
       if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.Cam_F)) {
            if (parameters.getType() ==ParticleTypes.CAMPFIRE_COSY_SMOKE||
                    parameters.getType() == ParticleTypes.FLAME ||
                    parameters.getType() == ParticleTypes.SOUL_FIRE_FLAME ||
                    parameters.getType() == ParticleTypes.CAMPFIRE_SIGNAL_SMOKE){
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.Other)) {
            if (parameters.getType() ==ParticleTypes.LAVA ||
                    parameters.getType() == ParticleTypes.WHITE_ASH  ||
                    parameters.getType() == ParticleTypes.EXPLOSION ||
                    parameters.getType() == ParticleTypes.EXPLOSION_EMITTER ||
                    parameters.getType() == ParticleTypes.UNDERWATER ||
                    parameters.getType() == ParticleTypes.DRIPPING_DRIPSTONE_WATER ||
                    parameters.getType() == ParticleTypes.FALLING_DRIPSTONE_WATER ||
                    parameters.getType() == ParticleTypes.SMOKE ||
                    parameters.getType() == ParticleTypes.FIREFLY ||
                    parameters.getType() == ParticleTypes.FIREWORK||
                    parameters.getType() == ParticleTypes.ASH ||
                    parameters.getType() == ParticleTypes.POOF ||
                    parameters.getType() == ParticleTypes.REVERSE_PORTAL ||
                    parameters.getType() == ParticleTypes.MYCELIUM ||
                    parameters.getType() == ParticleTypes.PORTAL ||
                    parameters.getType() == ParticleTypes.CRIMSON_SPORE ||
                    parameters.getType() == ParticleTypes.WARPED_SPORE ||
                    parameters.getType() == ParticleTypes.SPORE_BLOSSOM_AIR){
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
         if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.TotemP)){
            if(parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING ){
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.Item)){
            if(parameters.getType() == ParticleTypes.ITEM ){
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.Glow)){
            if(parameters.getType() == ParticleTypes.GLOW||
            parameters.getType() == ParticleTypes.GLOW_SQUID_INK){
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
         if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.RainP)){
            if(parameters.getType() == ParticleTypes.RAIN ||
            parameters.getType() == ParticleTypes.SNOWFLAKE){
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.Effect)){
            if (parameters.getType() == ParticleTypes.ENTITY_EFFECT ||
                    parameters.getType() == ParticleTypes.EFFECT ||
                    parameters.getType() == ParticleTypes.INSTANT_EFFECT ||
                    parameters.getType() == ParticleTypes.WITCH) {
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
        if (Removals.INSTANCE.particle.get(Removals.Particle.Drips)) {
            if (parameters.getType() == ParticleTypes.DRIPPING_WATER ||
                    parameters.getType() == ParticleTypes.FALLING_WATER ||
                    parameters.getType() == ParticleTypes.DRIPPING_LAVA ||
                    parameters.getType() == ParticleTypes.FALLING_LAVA ||
                    parameters.getType() == ParticleTypes.DRIPPING_HONEY ||
                    parameters.getType() == ParticleTypes.DRIPPING_OBSIDIAN_TEAR) {
                particle.scale(0); 
                 particle.markDead(); 
            }
        }
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.particle.get(Removals.Particle.Block)){
            if(parameters.getType() == ParticleTypes.BLOCK||
            parameters.getType() == ParticleTypes.BLOCK_CRUMBLE ||
            parameters.getType()  == ParticleTypes.DUST_PILLAR ||
            parameters.getType() == ParticleTypes.FALLING_DUST){
                particle.scale(0); 
                 particle.markDead(); 
            }
        }

    }
}