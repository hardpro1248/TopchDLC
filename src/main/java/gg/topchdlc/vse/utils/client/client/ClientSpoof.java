package gg.topchdlc.vse.utils.client.client;

import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.text.TextSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class ClientSpoof extends Group {
    public static ClientSpoof INSTANCE = new ClientSpoof();
    private ClientSpoof() {
        super("Client Spoof", false);
    }

    public CheckBox clientSpoof = enabled;
    public EnumSetting<Mode> type = enumSetting("Client Mode", Mode.LUNAR);
    public TextSetting customName = text("Client Name", "TopchDLC").visible(() -> type.is(Mode.CUSTOM));

    public String getClientName() {
        if (type.is(Mode.CUSTOM)) return customName.getText();
        return type.get().text;
    }

    @AllArgsConstructor
    @Getter
    public enum Mode implements EnumChoice {
        LUNAR("Lunar 1.21.4", "lunarclient:1.21.4"),
        LABYMOD("LabyMod4 1.21.4", "labymod4:1.21.4"),
        DEFAULT("Vanilla", "vanilla"),
        CUSTOM("Custom", "");
        final String renderName;
        final String text;
    }
}
