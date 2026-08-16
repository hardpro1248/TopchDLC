package gg.topchdlc.vse.rotation;

import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum MovementCorrection implements EnumChoice {
    NONE("None"),
    STRICT("Strict"),
    SILENT("Silent"),
    LOOK("Client look"),
    TARGET("Target");
    @Getter
    final String renderName;
}
