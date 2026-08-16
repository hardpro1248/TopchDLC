package gg.topchdlc.api.sound.filters;

import net.minecraft.util.math.MathHelper;
public class FilterReverb {
    public float density = 1.0F;
    public float diffusion = 1.0F;
    public float gain = 0.32F;
    public float gainHF = 0.89F;
    public float decayTime = 1.49F;
    public float decayHFRatio = 0.83F;
    public float reflectionsGain = 0.05F;
    public float reflectionsDelay = 0.007F;
    public float lateReverbGain = 1.26F;
    public float lateReverbDelay = 0.011F;
    public float airAbsorptionGainHF = 0.994F;
    public float roomRolloffFactor = 0.0F;

    private static final float MIN_DENSITY = 0.0F;
    private static final float MAX_DENSITY = 1.0F;
    private static final float MIN_DIFFUSION = 0.0F;
    private static final float MAX_DIFFUSION = 1.0F;
    private static final float MIN_GAIN = 0.0F;
    private static final float MAX_GAIN = 1.0F;
    private static final float MIN_GAIN_HF = 0.0F;
    private static final float MAX_GAIN_HF = 1.0F;
    private static final float MIN_DECAY_TIME = 0.1F;
    private static final float MAX_DECAY_TIME = 20.0F;
    private static final float MIN_DECAY_HF_RATIO = 0.1F;
    private static final float MAX_DECAY_HF_RATIO = 2.0F;
    private static final float MIN_REFLECTIONS_GAIN = 0.0F;
    private static final float MAX_REFLECTIONS_GAIN = 3.16F;
    private static final float MIN_REFLECTIONS_DELAY = 0.0F;
    private static final float MAX_REFLECTIONS_DELAY = 0.3F;
    private static final float MIN_LATE_REVERB_GAIN = 0.0F;
    private static final float MAX_LATE_REVERB_GAIN = 10.0F;
    private static final float MIN_LATE_REVERB_DELAY = 0.0F;
    private static final float MAX_LATE_REVERB_DELAY = 0.1F;
    private static final float MIN_AIR_ABSORPTION_GAIN_HF = 0.892F;
    private static final float MAX_AIR_ABSORPTION_GAIN_HF = 1.0F;
    private static final float MIN_ROOM_ROLLOFF_FACTOR = 0.0F;
    private static final float MAX_ROOM_ROLLOFF_FACTOR = 10.0F;

    public void checkParameters() {
        this.density = MathHelper.clamp(this.density, MIN_DENSITY, MAX_DENSITY);
        this.diffusion = MathHelper.clamp(this.diffusion, MIN_DIFFUSION, MAX_DIFFUSION);
        this.gain = MathHelper.clamp(this.gain, MIN_GAIN, MAX_GAIN);
        this.gainHF = MathHelper.clamp(this.gainHF, MIN_GAIN_HF, MAX_GAIN_HF);
        this.decayTime = MathHelper.clamp(this.decayTime, MIN_DECAY_TIME, MAX_DECAY_TIME);
        this.decayHFRatio = MathHelper.clamp(this.decayHFRatio, MIN_DECAY_HF_RATIO, MAX_DECAY_HF_RATIO);
        this.reflectionsGain = MathHelper.clamp(this.reflectionsGain, MIN_REFLECTIONS_GAIN, MAX_REFLECTIONS_GAIN);
        this.reflectionsDelay = MathHelper.clamp(this.reflectionsDelay, MIN_REFLECTIONS_DELAY, MAX_REFLECTIONS_DELAY);
        this.lateReverbGain = MathHelper.clamp(this.lateReverbGain, MIN_LATE_REVERB_GAIN, MAX_LATE_REVERB_GAIN);
        this.lateReverbDelay = MathHelper.clamp(this.lateReverbDelay, MIN_LATE_REVERB_DELAY, MAX_LATE_REVERB_DELAY);
        this.airAbsorptionGainHF = MathHelper.clamp(this.airAbsorptionGainHF, MIN_AIR_ABSORPTION_GAIN_HF, MAX_AIR_ABSORPTION_GAIN_HF);
        this.roomRolloffFactor = MathHelper.clamp(this.roomRolloffFactor, MIN_ROOM_ROLLOFF_FACTOR, MAX_ROOM_ROLLOFF_FACTOR);
    }

    public void reset() {
        this.density = 1.0F;
        this.diffusion = 1.0F;
        this.gain = 0.32F;
        this.gainHF = 0.89F;
        this.decayTime = 1.49F;
        this.decayHFRatio = 0.83F;
        this.reflectionsGain = 0.05F;
        this.reflectionsDelay = 0.007F;
        this.lateReverbGain = 1.26F;
        this.lateReverbDelay = 0.011F;
        this.airAbsorptionGainHF = 0.994F;
        this.roomRolloffFactor = 0.0F;
    }
}
