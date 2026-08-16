package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autoparser;

public enum ParserState {
    IDLE,
    OPENING_AH,
    WAIT_AH_OPEN,
    SEARCHING,
    WAIT_SEARCH_OPEN,
    WAIT_ANALYZE_DELAY,
    ANALYZING,
    CLOSING_AH,
    WAIT_CLOSE,
    NEXT_ITEM
}
