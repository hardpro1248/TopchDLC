package gg.topchdlc.vse.utils.other;

import gg.topchdlc.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtility {
    public static final Logger LOGGER = LoggerFactory.getLogger("topchdlc");

    public static void debug(Object object) {
        if (Client.IS_DEBUG)
            System.out.println("[debug] " + object);
    }

    public static void error(Exception ex, String message) {
        LOGGER.error(message, ex);
    }
}
