package gg.topchdlc.vse.utils.client.sounds;

import lombok.experimental.UtilityClass;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

@UtilityClass
public class MySoundEvents {
    public void init() {
    }

    private SoundEvent register(String name) {
        return Registry.register(Registries.SOUND_EVENT, Identifier.of("topchdlc", name), SoundEvent.of(Identifier.of("topchdlc", name)));
    }

    public final SoundEvent moduleOn = register("module_on");
    public final SoundEvent moduleOff = register("module_off");
    public final SoundEvent moduleEnable = register("module_enable");
    public final SoundEvent moduleDisable = register("module_disable");
    public final SoundEvent moduleEnable1 = register("module_enable1");
    public final SoundEvent moduleDisable1 = register("module_disable1");
    public final SoundEvent moduleEnable2 = register("module_enable2");
    public final SoundEvent moduleDisable2 = register("module_disable2");
    public final SoundEvent disable = register("disable");
    public final SoundEvent enable = register("enable");
    public final SoundEvent moduleoff1 = register("module_off1");
    public final SoundEvent moduleon1 = register("module_on1");
    public final SoundEvent thunder = register("thunder");
    public final SoundEvent join = register("join");
    public final SoundEvent hit = register("weave");
    public final SoundEvent hit2 = register("uwu");
    public final SoundEvent hit3 = register("moan1");
    public final SoundEvent hit3V2 = register("moan2");
    public final SoundEvent hit3V3 = register("moan3");
    public final SoundEvent hit3V4 = register("moan4");
    public final SoundEvent cutie = register("cutie");
    public final SoundEvent badtrip = register("badtrip");
}
