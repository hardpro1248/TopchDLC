package gg.topchdlc.vse.utils.jni;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.NativeType;

import java.util.List;

public interface DwmApi extends Library {
    DwmApi INSTANCE = isWindows() ? Native.load("dwmapi", DwmApi.class) : null;
    int INT_SIZE = 4;
    
    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    int BOOL_FALSE = 0;
    int BOOL_TRUE = 1;

    int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    int DWMWA_BORDER_COLOR = 34;
    int DWMWA_CAPTION_COLOR = 35;
    int DWMWA_TEXT_COLOR = 36;
    int DWMWA_SYSTEMBACKDROP_TYPE = 38;


    enum DWM_SYSTEMBACKDROP_TYPE {
        DWMSBT_AUTO("auto"),
        DWMSBT_NONE("none"),
        DWMSBT_MAINWINDOW("mica"),
        DWMSBT_TRANSIENTWINDOW("acrylic"),
        DWMSBT_TABBEDWINDOW("tabbed");

        public final String translate;

        DWM_SYSTEMBACKDROP_TYPE(final String translate) {
            this.translate = translate;
        }
    }

    enum DWM_WINDOW_CORNER_PREFERENCE {
        DWMWCP_DEFAULT("default"),
        DWMWCP_DONOTROUND("do_not_round"),
        DWMWCP_ROUND("round"),
        DWMWCP_ROUNDSMALL("round_small");
        public final String translate;
        DWM_WINDOW_CORNER_PREFERENCE(final String translate) {
            this.translate = translate;
        }
    }

    int DWMWA_COLOR_NONE = 0xFFFFFFFE;
    int DWMWA_COLOR_DEFAULT = 0x00000000;

    @NativeType("HRESULT")
    int DwmSetWindowAttribute(
            WinDef.HWND hwnd,
            int dwAttribute,
            PointerType pvAttribute,
            int cbAttribute
    );

    static void updateDwm(final boolean fullscreen, final long window) {
        if (INSTANCE == null) return;
        final WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(GLFWNativeWin32.glfwGetWin32Window(window)));
        if (fullscreen) {
            DwmApi.disableWindowEffect(hwnd);
            return;
        }
        INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, new IntByReference(0x353535), INT_SIZE);
        INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, new IntByReference(0x202020), INT_SIZE);
        INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, new IntByReference(0xFFFFFF), INT_SIZE);
    }

    static void disableWindowEffect(final WinDef.HWND hwnd) {
        if (INSTANCE == null) return;
        
        INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, new IntByReference(DWM_SYSTEMBACKDROP_TYPE.DWMSBT_AUTO.ordinal()), INT_SIZE);
        INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, new IntByReference(DWM_WINDOW_CORNER_PREFERENCE.DWMWCP_DEFAULT.ordinal()), INT_SIZE);
        INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, new IntByReference(DWMWA_COLOR_DEFAULT), INT_SIZE);
        INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, new IntByReference(DWMWA_COLOR_DEFAULT), INT_SIZE);
        INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, new IntByReference(DWMWA_COLOR_DEFAULT), INT_SIZE);
    }

    private static int convert(final int color) {
        final int b = (color >> 16) & 0xFF;
        final int g = (color >> 8) & 0xFF;
        final int r = color & 0xFF;

        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }


    @NativeType("HRESULT")
    int DwmExtendFrameIntoClientArea(
            WinDef.HWND hwnd,
            MARGINS pMarInset
    );

    class MARGINS extends Structure {

        public int cxLeftWidth;
        public int cxRightWidth;
        public int cyTopHeight;
        public int cyBottomHeight;

        public MARGINS(final int cxLeftWidth, final int cxRightWidth, final int cyTopHeight, final int cyBottomHeight) {
            this.cxLeftWidth = cxLeftWidth;
            this.cxRightWidth = cxRightWidth;
            this.cyTopHeight = cyTopHeight;
            this.cyBottomHeight = cyBottomHeight;
        }

        @Override
        protected List<String> getFieldOrder() {
            return List.of("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight");
        }
    }
}