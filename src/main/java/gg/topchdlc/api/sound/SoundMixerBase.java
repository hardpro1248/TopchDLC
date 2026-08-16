package gg.topchdlc.api.sound;

import gg.topchdlc.api.sound.filters.FilterLowPass;
import gg.topchdlc.api.sound.filters.FilterReverb;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.EXTEfx;


public class SoundMixerBase {
    @Getter
    private final FilterReverb reverbFilter = new FilterReverb();
    @Getter
    private final FilterLowPass lowPassFilter = new FilterLowPass();

    private float echoPercent = 0.0F;
    private float reflectPercent = 0.0F;
    private float lowPassGain = 1.0F;
    private float lowPassGainHF = 1.0F;

    private int reverbEffectSlot = -1;
    private int reverbEffect = -1;
    private int lowPassFilter_AL = -1;
    private boolean initialized = false;
    private boolean efxSupported = false;

    public static SoundMixerBase loadMixer() {
        return new SoundMixerBase();
    }

    public void setEchoEffect(float echo, float reflect) {
        this.echoPercent = echo;
        this.reflectPercent = reflect;
    }

    public void setLowPass(float gain, float gainHF) {
        this.lowPassGain = gain;
        this.lowPassGainHF = gainHF;
    }

    public float getEchoPercent() {
        return echoPercent;
    }

    public float getReflectPercent() {
        return reflectPercent;
    }

    public float getLowPassGain() {
        return lowPassGain;
    }

    public float getLowPassGainHF() {
        return lowPassGainHF;
    }


    public void initializeEffects() {
        try {
            org.lwjgl.openal.ALCapabilities capabilities = org.lwjgl.openal.AL.getCapabilities();
            if (capabilities == null || !capabilities.ALC_EXT_EFX) {
                System.out.println("[RTX Sounds] EFX extension not supported, audio effects disabled");
                efxSupported = false;
                return;
            }

            efxSupported = true;
            System.out.println("[RTX Sounds] EFX extension supported, initializing audio effects...");

            reverbEffect = EXTEfx.alGenEffects();
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                System.err.println("[RTX Sounds] Failed to create reverb effect");
                efxSupported = false;
                return;
            }
            EXTEfx.alEffecti(reverbEffect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);

            reverbEffectSlot = EXTEfx.alGenAuxiliaryEffectSlots();
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                System.err.println("[RTX Sounds] Failed to create effect slot");
                efxSupported = false;
                return;
            }

            lowPassFilter_AL = EXTEfx.alGenFilters();
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                System.err.println("[RTX Sounds] Failed to create lowpass filter");
                efxSupported = false;
                return;
            }
            EXTEfx.alFilteri(lowPassFilter_AL, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

            System.out.println("[RTX Sounds] Audio effects initialized successfully");

        } catch (Exception e) {
            System.err.println("[RTX Sounds] Failed to initialize OpenAL effects: " + e.getMessage());
            efxSupported = false;
        }
    }

    public void injectFiltersToChannel(SoundInstance sound, int sourceId) {
        if (sourceId <= 0) return;
        
        if (!initialized) {
            initializeEffects();
            initialized = true;
        }

        if (!efxSupported) {
            return;
        }

        try {
            if (reverbEffect != -1 && reverbEffectSlot != -1) {
                applyReverbEffect();
                EXTEfx.alAuxiliaryEffectSloti(reverbEffectSlot, EXTEfx.AL_EFFECTSLOT_EFFECT, reverbEffect);
                if (AL10.alGetError() == AL10.AL_NO_ERROR) {
                    org.lwjgl.openal.AL11.alSource3i(sourceId, EXTEfx.AL_AUXILIARY_SEND_FILTER, reverbEffectSlot, 0, EXTEfx.AL_FILTER_NULL);
                }
            }

            if (lowPassFilter_AL != -1) {
                applyLowPassFilter();
                AL10.alSourcei(sourceId, EXTEfx.AL_DIRECT_FILTER, lowPassFilter_AL);
            }


        } catch (Exception e) {
        }
    }

    private void applyReverbEffect() {
        if (reverbEffect == -1) return;

        try {
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DENSITY, reverbFilter.density);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DIFFUSION, reverbFilter.diffusion);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_GAIN, reverbFilter.gain);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_GAINHF, reverbFilter.gainHF);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DECAY_TIME, reverbFilter.decayTime);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_DECAY_HFRATIO, reverbFilter.decayHFRatio);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, reverbFilter.reflectionsGain);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, reverbFilter.reflectionsDelay);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, reverbFilter.lateReverbGain);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, reverbFilter.lateReverbDelay);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_AIR_ABSORPTION_GAINHF, reverbFilter.airAbsorptionGainHF);
            EXTEfx.alEffectf(reverbEffect, EXTEfx.AL_REVERB_ROOM_ROLLOFF_FACTOR, reverbFilter.roomRolloffFactor);
        } catch (Exception e) {
            System.err.println("[RTX Sounds] Failed to apply reverb parameters: " + e.getMessage());
        }
    }

    private void applyLowPassFilter() {
        if (lowPassFilter_AL == -1) return;

        try {
            EXTEfx.alFilterf(lowPassFilter_AL, EXTEfx.AL_LOWPASS_GAIN, lowPassFilter.gain);
            EXTEfx.alFilterf(lowPassFilter_AL, EXTEfx.AL_LOWPASS_GAINHF, lowPassFilter.gainHF);
        } catch (Exception e) {
            System.err.println("[RTX Sounds] Failed to apply lowpass parameters: " + e.getMessage());
        }
    }

    private void applyBetterStereo(SoundInstance sound, int sourceId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || sound.isRelative()) return;

        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d soundPos = new Vec3d(sound.getX(), sound.getY(), sound.getZ());
        
        Vec2f angles = SoundHelper.calculate(playerPos, soundPos);
        float playerYaw = mc.player.getYaw();
        float angleDiff = SoundHelper.getAngleDifference(angles.x, playerYaw);
        
        float stereoModifier = 1.0f - (angleDiff / 180.0f) * 0.3f;
        stereoModifier = Math.max(0.7f, Math.min(1.0f, stereoModifier));
        
        float currentVolume = sound.getVolume();
        AL10.alSourcef(sourceId, AL10.AL_GAIN, currentVolume * stereoModifier);
    }

    private void applyToneCompensation(SoundInstance sound, int sourceId) {
        float basePitch = sound.getPitch();
        float toneModifier = 1.0f + (this.echoPercent * 0.05f);
        toneModifier = Math.max(0.95f, Math.min(1.05f, toneModifier));
        
        AL10.alSourcef(sourceId, AL10.AL_PITCH, basePitch * toneModifier);
    }

    public void cleanupEffects() {
        try {
            if (reverbEffect != -1) {
                EXTEfx.alDeleteEffects(reverbEffect);
                reverbEffect = -1;
            }
            if (reverbEffectSlot != -1) {
                EXTEfx.alDeleteAuxiliaryEffectSlots(reverbEffectSlot);
                reverbEffectSlot = -1;
            }
            if (lowPassFilter_AL != -1) {
                EXTEfx.alDeleteFilters(lowPassFilter_AL);
                lowPassFilter_AL = -1;
            }
        } catch (Exception e) {
            System.err.println("[RTX Sounds] Failed to cleanup OpenAL effects: " + e.getMessage());
        }
    }
}
