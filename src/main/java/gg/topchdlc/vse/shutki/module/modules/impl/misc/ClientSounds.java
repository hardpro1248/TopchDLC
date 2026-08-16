package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.sounds.MySoundEvents;
import net.minecraft.sound.SoundEvent;


public class ClientSounds extends Module {
    public static final ClientSounds INSTANCE = new ClientSounds();
    private ClientSounds() {
        super("Client sounds", Category.Misc, "x");
        setEnabled(true, false);
    }

    public enum SoundPack {
        DEFAULT(MySoundEvents.moduleOn, MySoundEvents.moduleOff),
        PACK_1(MySoundEvents.moduleEnable, MySoundEvents.moduleDisable),
        PACK_2(MySoundEvents.moduleEnable1, MySoundEvents.moduleDisable1),
        PACK_3(MySoundEvents.moduleEnable2, MySoundEvents.moduleDisable2),
        PACK_TH(MySoundEvents.enable,MySoundEvents.disable),
        Pack_4(MySoundEvents.moduleon1,MySoundEvents.moduleoff1);

        public final SoundEvent enable, disable;
        SoundPack(SoundEvent enable, SoundEvent disable) {
            this.enable = enable;
            this.disable = disable;
        }
    }

    public CheckBox toggleFunction = checkbox("Module sound", true);
    public EnumSetting<SoundPack> soundPack = enumSetting("Sound pack", SoundPack.DEFAULT);
    public SliderSetting volume = sliderSetting("Volume", 0.5f, 0.1f, 2f).increment(.1f);
}
