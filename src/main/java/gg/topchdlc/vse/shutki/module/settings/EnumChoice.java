package gg.topchdlc.vse.shutki.module.settings;

public interface EnumChoice {
    default String getLangClassName() { return this.getClass().getName(); }
    default String getRenderName() { return ((Enum<?>) this).name(); }
    default boolean isDefaultEnabled() { return false; }
}
