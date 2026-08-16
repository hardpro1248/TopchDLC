package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autosell;
/**
 * Create by daun kvass
 */
public enum AutoSellMode {
    берет_цену_с_ах("По рынку (сравнивает с покупкой)"),
    по_купившией_стоимосте("По покупке"),
    берет_цену_с_автобая("По автобаю");

    private final String displayName;

    AutoSellMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
