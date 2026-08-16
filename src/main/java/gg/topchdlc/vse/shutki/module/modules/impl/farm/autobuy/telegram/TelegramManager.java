package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.telegram;

import net.minecraft.item.ItemStack;
import gg.topchdlc.api.telegram.TelegramNotifier;
import gg.topchdlc.api.telegram.TelegramPoller;

import java.util.function.Consumer;

/**
 * Create by daun kvass
 */
public class TelegramManager {
    private final TelegramPoller poller = new TelegramPoller();

    private boolean enabled = false;
    private String token = "";
    private String chatId = "";
    private boolean proxyEnabled = false;
    private String proxyHost = null;
    private int proxyPort = -1;
    private String proxyUser = null;
    private String proxyPass = null;

    public void configure(boolean enabled, String token, String chatId,
                          boolean proxyEnabled, String proxyHost, int proxyPort,
                          String proxyUser, String proxyPass) {
        this.enabled = enabled;
        this.token = token;
        this.chatId = chatId;
        this.proxyEnabled = proxyEnabled;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPass = proxyPass;
    }

    public void start(Consumer<String> commandHandler) {
        if (!enabled || token.isBlank() || chatId.isBlank()) return;
        poller.start(token, chatId,
                proxyEnabled ? proxyHost : null,
                proxyEnabled ? proxyPort : -1,
                proxyEnabled ? proxyUser : null,
                proxyEnabled ? proxyPass : null,
                commandHandler::accept);
    }

    public void stop() {
        poller.stop();
    }

    public void notifyBought(ItemStack stack, int price) {
        if (!enabled || token.isBlank() || chatId.isBlank()) return;
        int count = stack.getCount();
        int pricePerItem = count > 0 ? price / count : price;
        String msg = "AutoBuy купил!\n"
                + "Предмет: " + stack.getName().getString() + "\n"
                + "Кол-во: " + count + "\n"
                + "Цена: " + fmt(price) + "$\n"
                + "Цена/шт: " + fmt(pricePerItem) + "$";
        sendAsync(msg);
    }

    public void notifySold(String itemName, int count, int soldPrice, int boughtPrice) {
        if (!enabled || token.isBlank() || chatId.isBlank()) return;
        String msg = "AutoSell продал!\n"
                + "Предмет: " + itemName + "\n"
                + "Кол-во: " + count + "\n"
                + "Продано за: " + fmt(soldPrice) + "$\n"
                + "Куплено за: " + fmt(boughtPrice) + "$";
        sendAsync(msg);
    }

    public boolean isEnabled() { return enabled; }

    private void sendAsync(String msg) {
        TelegramNotifier.sendAsync(token, chatId, msg, null,
                proxyEnabled ? proxyHost : null,
                proxyEnabled ? proxyPort : -1,
                proxyEnabled ? proxyUser : null,
                proxyEnabled ? proxyPass : null);
    }

    private String fmt(int price) {
        if (price >= 1_000_000) return (price / 1_000_000) + "M";
        if (price >= 1_000) return (price / 1_000) + "K";
        return String.valueOf(price);
    }
}
