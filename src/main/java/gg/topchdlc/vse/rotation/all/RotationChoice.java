package gg.topchdlc.vse.rotation.all;

import lombok.AllArgsConstructor;
import lombok.Getter;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;

public abstract class RotationChoice extends Choice implements Rotation {
    
    protected RotationChoice(String name) {
        super(name);
    }

    @AllArgsConstructor
    @Getter
    public enum SettingsMode implements EnumChoice {
        Default("Default"),
        Custom("Custom");
        
        final String renderName;
    }
}
