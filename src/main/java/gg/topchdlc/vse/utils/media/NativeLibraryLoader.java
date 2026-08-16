package gg.topchdlc.vse.utils.media;

import java.io.*;

public class NativeLibraryLoader {
    private static boolean loaded = false;
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "topchdlc-natives";
    public static synchronized void loadMediaPlayerInfoNatives() {
        if (loaded) return;
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();
            String libName = null;
            String libPath = null;
            if (os.contains("win")) {
                if (arch.contains("64")) {
                    libName = "MediaPlayerInfo.dll";
                    libPath = "/natives/win32-x86-64/" + libName;
                } else {
                    libName = "MediaPlayerInfo.dll";
                    libPath = "/natives/win32-x86/" + libName;
                }
            } else if (os.contains("mac")) {
                libName = "libMediaPlayerInfo.dylib";
                libPath = "/natives/darwin/" + libName;
            } else if (os.contains("nix") || os.contains("nux")) {
                libName = "libMediaPlayerInfo.so";
                libPath = "/natives/linux-x86-64/" + libName;
            }
            if (libName == null) {
                System.err.println("Unsupported OS for media player info: " + os);
                return;
            }
            InputStream in = NativeLibraryLoader.class.getResourceAsStream(libPath);
            if (in == null) {
                libPath = "/natives/" + libName;
                in = NativeLibraryLoader.class.getResourceAsStream(libPath);
            }
            if (in == null) {
                System.err.println("Native library not found in JAR: " + libPath);
                System.err.println("Media player info will not work in built JAR");
                return;
            }
            File tempDir = new File(TEMP_DIR);
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            File tempLib = new File(tempDir, libName);
            if (tempLib.exists() && (System.currentTimeMillis() - tempLib.lastModified()) < 86400000) {
                System.out.println("Using cached native library: " + tempLib.getAbsolutePath());
            } else {
                try (FileOutputStream out = new FileOutputStream(tempLib)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("Extracted native library to: " + tempLib.getAbsolutePath());
            }
            in.close();
            System.load(tempLib.getAbsolutePath());
            loaded = true;
            System.out.println("Successfully loaded media player info native library");
            
        } catch (Exception e) {
            System.err.println("Failed to load native library for media player info: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static boolean isLoaded() {
        return loaded;
    }
}
