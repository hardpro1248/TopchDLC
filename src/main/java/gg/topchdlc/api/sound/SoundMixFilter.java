package gg.topchdlc.api.sound;

import gg.topchdlc.api.sound.filters.FilterLowPass;
import gg.topchdlc.api.sound.filters.FilterReverb;
import lombok.Getter;
import lombok.Setter;


public class SoundMixFilter {
    @Getter
    private final SoundMixerBase mixer;
    @Getter
    private final SoundSurroundTool surround;
    @Getter @Setter
    private boolean state = false;

    private SoundMixFilter() {
        this.mixer = SoundMixerBase.loadMixer();
        this.surround = SoundSurroundTool.build();
    }

    public static SoundMixFilter makeDistorterMixer() {
        return new SoundMixFilter();
    }


    public void updateMixer() {
        float[] args = new float[]{0.0F, 0.0F, 1.0F, 1.0F};


        float performanceMod = this.surround.isTooPerfomance() ? 0.75F : 1.0F;
        this.mixer.setEchoEffect(
            args[0] * performanceMod, 
            args[1] * (this.surround.isTooPerfomance() ? 0.4F : 0.8F)
        );
        this.mixer.setLowPass(args[2], args[3]);

        this.updateFiltersData();
    }

    private void updateFiltersData() {
        final SoundMixerBase mixer = this.getMixer();
        final FilterReverb reverbFilter = mixer.getReverbFilter();
        final FilterLowPass lowPassFilter = mixer.getLowPassFilter();
        
        final float echoDelay = mixer.getEchoPercent();
        final float echoRev = echoDelay + mixer.getReflectPercent() * 8.000011459172877F;
        
        reverbFilter.decayTime = echoDelay;
        reverbFilter.reflectionsGain = echoRev * (0.05F + 0.05F * echoDelay);
        reverbFilter.reflectionsDelay = 0.125F * echoDelay;
        reverbFilter.lateReverbGain = echoRev * (1.26F + 0.2F * echoDelay);
        reverbFilter.lateReverbDelay = 0.01F * echoDelay;
        reverbFilter.checkParameters();
        
        final float lLevelGain = mixer.getLowPassGain();
        final float lLevelGainHF = mixer.getLowPassGainHF();
        
        lowPassFilter.gain = lerp(
            lowPassFilter.gain, 
            lLevelGain, 
            lowPassFilter.gainHF > lLevelGainHF ? 0.05F : 0.1F
        );
        lowPassFilter.gainHF = lerp(
            lowPassFilter.gainHF, 
            lLevelGainHF, 
            lowPassFilter.gainHF > lLevelGainHF ? 0.3F : 0.12F
        );
        lowPassFilter.checkParameters();
    }


    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public boolean getHasMixerLoaded() {
        return mixer != null;
    }


    public void init() {
    }

    public void unload() {
        if (mixer != null) {
            mixer.cleanupEffects();
        }
    }
}
