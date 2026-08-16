package gg.topchdlc.vse.utils.client.sounds;


import net.minecraft.client.sound.PositionedSoundInstance;
import gg.topchdlc.MinecraftHolder;
import net.minecraft.sound.SoundEvent;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundUtility implements MinecraftHolder {

    public static void playSound(SoundEvent sound, float volume) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        mc.world.playSoundFromEntity(
                mc.player,
                mc.player,
                sound,
                mc.player.getSoundCategory(),
                volume,
                1f
        );
    }
    public static void playSystemSound(SoundEvent sound) {
        if (sound == null) return;
        mc.getSoundManager().play(PositionedSoundInstance.ui(sound, 1.0F));
    }
    public static void playSoundRaw(String name, float volume) {
        new Thread(() -> {
            try {
                InputStream audioSrc = SoundUtility.class.getResourceAsStream("/assets/topchdlc/sounds/" + name + ".wav");
                if (audioSrc == null) {
                    audioSrc = SoundUtility.class.getResourceAsStream("/assets/topchdlc/sounds/" + name + ".ogg");
                }
                if (audioSrc == null) {
                    return;
                }

                BufferedInputStream bufferedIn = new BufferedInputStream(audioSrc);
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(bufferedIn);

                AudioFormat baseFormat = inputStream.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );

                AudioInputStream decodedStream = inputStream;
                if (!baseFormat.matches(decodedFormat)) {
                    if (AudioSystem.isConversionSupported(decodedFormat, baseFormat)) {
                        decodedStream = AudioSystem.getAudioInputStream(decodedFormat, inputStream);
                    }
                }

                Clip clip = AudioSystem.getClip();
                clip.open(decodedStream);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (20.0 * Math.log10(Math.max(0.0001, volume)));
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    gainControl.setValue(dB);
                }

                clip.start();

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (UnsupportedAudioFileException e) {
            } catch (Exception e) {
            }
        }).start();
    }
    public static Clip playSoundRawClip(String name, float volume) {
        try {
            InputStream audioSrc = SoundUtility.class.getResourceAsStream("/assets/topchdlc/sounds/" + name + ".wav");
            if (audioSrc == null)
                audioSrc = SoundUtility.class.getResourceAsStream("/assets/topchdlc/sounds/" + name + ".ogg");
            if (audioSrc == null) return null;

            BufferedInputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(bufferedIn);
            AudioFormat baseFormat = inputStream.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(), 16,
                    baseFormat.getChannels(), baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(), false
            );
            AudioInputStream decodedStream = inputStream;
            if (!baseFormat.matches(decodedFormat) && AudioSystem.isConversionSupported(decodedFormat, baseFormat))
                decodedStream = AudioSystem.getAudioInputStream(decodedFormat, inputStream);

            Clip clip = AudioSystem.getClip();
            clip.open(decodedStream);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (20.0 * Math.log10(Math.max(0.0001, volume)));
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
            }
            clip.start();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) clip.close();
            });
            return clip;
        } catch (Exception e) {
            return null;
        }
    }
}
