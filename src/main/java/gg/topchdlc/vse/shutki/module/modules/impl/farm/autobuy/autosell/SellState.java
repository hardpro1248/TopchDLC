package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autosell;
/**
 * Create by daun kvass
 */
public enum SellState {
    IDLE,
    SWAP_TO_HOTBAR,
    WAIT_SWAP,
    CLOSING_SCREEN,
    WAIT_SCREEN_CLOSED,
    MARKET_SEND_SEARCH,
    MARKET_WAIT_AH_OPEN,
    MARKET_WAIT_SEARCH_RESULTS,
    MARKET_ANALYZE,
    MARKET_CLOSE_AH,
    MARKET_WAIT_CLOSED,
    SELL_PREPARE,
    SELL_SELECT_SLOT,
    SELL_WAIT_SLOT,
    SELL_SEND_COMMAND,
    SELL_WAIT_DONE,
    SELL_REOPEN_AH,
    SELL_WAIT_AH_REOPEN,
    DONE
}
