package gg.topchdlc.vse.rotation;

import gg.topchdlc.vse.shutki.module.settings.EnumChoice;

public enum RotationTiming implements EnumChoice {
    Normal,
    Snap,
    Tick;

    @Override
    public String getLangClassName() {
        return "RotationTiming";
    }
}
