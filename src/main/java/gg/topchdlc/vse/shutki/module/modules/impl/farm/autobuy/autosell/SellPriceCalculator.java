package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autosell;

import net.minecraft.text.Text;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.awt.*;

/**
 * Create by daun kvass
 */
public class SellPriceCalculator {

    public int calculate(SellTask task, AutoSellMode mode, int marketMinPerUnit, float markupPercent, int actualCount) {
        int basePricePerUnit = resolveBase(task, mode, marketMinPerUnit);
        int withMarkup = (int) (basePricePerUnit + basePricePerUnit * (markupPercent / 100f));
        return withMarkup * actualCount;
    }

    private int resolveBase(SellTask task, AutoSellMode mode, int marketMinPerUnit) {
        switch (mode) {
            case берет_цену_с_ах -> {
                if (marketMinPerUnit != Integer.MAX_VALUE) {
                    if (marketMinPerUnit > task.buyPricePerUnit) {
                        ChatUtility.send(Text.literal("AutoSell: Цена с аук (" + fmt(marketMinPerUnit) +
                                "$) > покупки (" + fmt(task.buyPricePerUnit) + "$) → берем аук")
                                .withColor(new Color(100, 255, 100).getRGB()));
                        return marketMinPerUnit;
                    } else {
                        ChatUtility.send(Text.literal("AutoSell: Цена с аук (" + fmt(marketMinPerUnit) +
                                "$) ≤ покупки (" + fmt(task.buyPricePerUnit) + "$) → берем покупку")
                                .withColor(new Color(255, 200, 100).getRGB()));
                        return task.buyPricePerUnit;
                    }
                } else {
                    ChatUtility.send(Text.literal("AutoSell: Цена с аук не найдена → берем покупку (" +
                            fmt(task.buyPricePerUnit) + "$)")
                            .withColor(new Color(255, 200, 100).getRGB()));
                    return task.buyPricePerUnit;
                }
            }
            case по_купившией_стоимосте -> {
                return task.buyPricePerUnit;
            }
            default -> {
                return task.buyPricePerUnit;
            }
        }
    }

    private String fmt(int price) {
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }
}
