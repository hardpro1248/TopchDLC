package gg.topchdlc.vse.utils.media;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
/// ТЕМНИ ПРИНУЦ
public class MacOSMediaProvider {
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    public interface CoreFoundation extends Library {
        CoreFoundation INSTANCE = IS_MAC ? Native.load("CoreFoundation", CoreFoundation.class) : null;
        Pointer CFStringCreateWithCString(Pointer alloc, String cStr, int encoding);
        void CFRelease(Pointer cf);
    }
    public interface MediaRemote extends Library {
        MediaRemote INSTANCE = IS_MAC ? loadMediaRemote() : null;
        Pointer MRMediaRemoteGetNowPlayingInfo();
        Pointer MRMediaRemoteGetNowPlayingApplicationDisplayName();
        boolean MRMediaRemoteGetNowPlayingApplicationIsPlaying();
    }
    private static MediaRemote loadMediaRemote() {
        try {
            return Native.load("/System/Library/PrivateFrameworks/MediaRemote.framework/MediaRemote", MediaRemote.class);
        } catch (Exception e) {
            System.err.println("Failed to load MediaRemote framework: " + e.getMessage());
            return null;
        }
    }
    public static class MediaInfo {
        public String title = "";
        public String artist = "";
        public String album = "";
        public String appName = "";
        public boolean isPlaying = false;
        public double duration = 0;
        public double position = 0;
    }
    public static MediaInfo getNowPlaying() {
        MediaInfo info = new MediaInfo();
        
        if (!IS_MAC || MediaRemote.INSTANCE == null) {
            return info;
        }
        try {
            info.isPlaying = MediaRemote.INSTANCE.MRMediaRemoteGetNowPlayingApplicationIsPlaying();
            Pointer appNamePtr = MediaRemote.INSTANCE.MRMediaRemoteGetNowPlayingApplicationDisplayName();
            if (appNamePtr != null) {
                info.appName = appNamePtr.getString(0);
            }
            String script = "tell application \"System Events\"\n" +
                    "    set frontApp to name of first application process whose frontmost is true\n" +
                    "end tell\n" +
                    "try\n" +
                    "    tell application \"Music\"\n" +
                    "        if player state is playing or player state is paused then\n" +
                    "            set trackName to name of current track\n" +
                    "            set artistName to artist of current track\n" +
                    "            set albumName to album of current track\n" +
                    "            set trackDuration to duration of current track\n" +
                    "            set trackPosition to player position\n" +
                    "            set isPlaying to (player state is playing)\n" +
                    "            return trackName & \"|\" & artistName & \"|\" & albumName & \"|\" & trackDuration & \"|\" & trackPosition & \"|\" & isPlaying\n" +
                    "        end if\n" +
                    "    end tell\n" +
                    "end try\n" +
                    "try\n" +
                    "    tell application \"Spotify\"\n" +
                    "        if player state is playing or player state is paused then\n" +
                    "            set trackName to name of current track\n" +
                    "            set artistName to artist of current track\n" +
                    "            set albumName to album of current track\n" +
                    "            set trackDuration to duration of current track / 1000\n" +
                    "            set trackPosition to player position\n" +
                    "            set isPlaying to (player state is playing)\n" +
                    "            return trackName & \"|\" & artistName & \"|\" & albumName & \"|\" & trackDuration & \"|\" & trackPosition & \"|\" & isPlaying\n" +
                    "        end if\n" +
                    "    end tell\n" +
                    "end try\n" +
                    "return \"\"";
            
            String result = executeAppleScript(script);
            if (result != null && !result.isEmpty()) {
                String[] parts = result.split("\\|");
                if (parts.length >= 6) {
                    info.title = parts[0];
                    info.artist = parts[1];
                    info.album = parts[2];
                    info.duration = Double.parseDouble(parts[3]);
                    info.position = Double.parseDouble(parts[4]);
                    info.isPlaying = Boolean.parseBoolean(parts[5]);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error getting macOS media info: " + e.getMessage());
        }
        return info;
    }
    private static String executeAppleScript(String script) {
        try {
            ProcessBuilder pb = new ProcessBuilder("osascript", "-e", script);
            Process process = pb.start();
            
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();
            return output.toString().trim();
            
        } catch (Exception e) {
            return "";
        }
    }
    public static void sendMediaCommand(String command) {
        if (!IS_MAC) return;
        String script = "";
        switch (command.toLowerCase()) {
            case "playpause":
                script = "tell application \"System Events\" to key code 16 using {command down}";
                break;
            case "next":
                script = "tell application \"System Events\" to key code 17 using {command down}";
                break;
            case "previous":
                script = "tell application \"System Events\" to key code 18 using {command down}";
                break;
        }
        if (!script.isEmpty()) {
            executeAppleScript(script);
        }
    }
    public static boolean isAvailable() {
        return IS_MAC && MediaRemote.INSTANCE != null;
    }
}
