package gg.topchdlc.vse.utils.network;

import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.text.TextSetting;
import gg.topchdlc.vse.utils.other.LogUtility;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ProxyUtility extends Group {
    public ProxyUtility() {
        super("Proxy", false);
    }

    private final TextSetting address = text("Address", "localhost:9150");
    private final EnumSetting<Type> type = enumSetting("Type", Type.SOCKS5);
    private final TextSetting username = text("Username", "").desc("Enter username");
    private final TextSetting password = text("Password", "").desc("Enter password").setHideMask("*").visible(() -> type.is(Type.SOCKS5));

    public void fire(ChannelPipeline pipeline) {
        if (isEnabled()) {
            String v = address.getText();
            int colon = v.indexOf(':');
            String host = v.substring(0, colon);
            int port = Integer.parseInt(v.substring(colon + 1));
            LogUtility.debug(String.format("proxy address resolved! %s:%d", host, port));
            SocketAddress resolvedAddress = new InetSocketAddress(host, port);
            switch (type.get()) {
                case SOCKS4 -> pipeline.addFirst("leet-socks4", new Socks4ProxyHandler(resolvedAddress, username.getText()));
                case SOCKS5 -> pipeline.addFirst("leet-socks5", new Socks5ProxyHandler(resolvedAddress, username.getText(), password.getText()));
            }
        }
    }

    public enum Type {
        SOCKS5,
        SOCKS4,
    }
}
