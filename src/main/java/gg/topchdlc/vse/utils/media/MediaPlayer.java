package gg.topchdlc.vse.utils.media;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaPlayer {
    private volatile String title = "";
    private volatile String artist = "";
    private volatile String source = "";
    private volatile boolean isPlaying = false;
    private volatile long duration = 0;
    private volatile long position = 0;
    private ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long lastCheck = 0;
    private static final long CHECK_INTERVAL = 2000;
    private String lastTitle = "";
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final boolean USE_MAC_PROVIDER = IS_MAC && MacOSMediaProvider.isAvailable();
    private static boolean USE_WINDOWS_PROVIDER = false;
    private static boolean WINDOWS_PROVIDER_TESTED = false;
    
    static {
        if (USE_MAC_PROVIDER) {
            System.out.println("Using macOS media provider");
        } else if (IS_WINDOWS) {
            System.out.println("Will try Windows SMTC provider, fallback to media-player-info if needed");
        } else {
            System.out.println("Using fallback media provider");
        }
    }

    private static ExecutorService newDaemonExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MediaPlayer-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        running.set(true);
        executor = newDaemonExecutor();
        lastCheck = 0;
    }

    public void onTick() {
        if (!running.get()) {
            start();
        }
        if (executor == null || executor.isShutdown()) {
            executor = newDaemonExecutor();
        }
        long now = System.currentTimeMillis();
        if (now - lastCheck < CHECK_INTERVAL) return;
        lastCheck = now;
        try {
            executor.submit(this::checkMedia);
        } catch (Exception e) {
            executor = newDaemonExecutor();
        }
    }

    private void checkMedia() {
        try {
            if (USE_MAC_PROVIDER) {
                checkMediaMacOS();
                return;
            }
            if (IS_WINDOWS) {
                try {
                    List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
                    if (sessions != null && !sessions.isEmpty()) {
                        checkMediaPlayerInfo(sessions);
                        return;
                    }
                } catch (UnsatisfiedLinkError e) {
                    if (!WINDOWS_PROVIDER_TESTED) {
                        System.out.println("media-player-info not available, trying Windows SMTC provider");
                        WINDOWS_PROVIDER_TESTED = true;
                        USE_WINDOWS_PROVIDER = true;
                    }
                } catch (Exception e) {
                }
                if (USE_WINDOWS_PROVIDER) {
                    checkMediaWindows();
                    return;
                }
            }
            List<IMediaSession> sessions = null;
            try {
                sessions = MediaPlayerInfo.Instance.getMediaSessions();
            } catch (Exception e) {
                clearState();
                return;
            }
            
            if (sessions == null) {
                clearState();
                return;
            }
            checkMediaPlayerInfo(sessions);
        } catch (Exception e) {
        }
    }
    private void checkMediaPlayerInfo(List<IMediaSession> sessions) {
        try {
            if (sessions.isEmpty()) {
                clearState();
                return;
            }
            IMediaSession playingSession = null;
            IMediaSession pausedSession = null;
            for (IMediaSession s : sessions) {
                MediaInfo media = s.getMedia();
                if (media == null) continue;
                String t = media.getTitle();
                if (t == null || t.isEmpty()) continue;
                if (media.getPlaying()) {
                    if (playingSession == null) {
                        playingSession = s;
                    } else {
                        String owner = s.getOwner() != null ? s.getOwner().toLowerCase() : "";
                        String currentOwner = playingSession.getOwner() != null ? playingSession.getOwner().toLowerCase() : "";

                        int newPriority = getSourcePriority(owner);
                        int currentPriority = getSourcePriority(currentOwner);

                        if (newPriority > currentPriority) {
                            playingSession = s;
                        }
                    }
                } else {
                    if (pausedSession == null) {
                        pausedSession = s;
                    }
                }
            }
            IMediaSession session = playingSession != null ? playingSession : pausedSession;
            if (session == null) {
                clearState();
                return;
            }
            MediaInfo info = session.getMedia();
            String newTitle = sanitize(info.getTitle());
            String newArtist = sanitize(info.getArtist());
            boolean nowPlaying = info.getPlaying();
            if (newTitle.isEmpty()) {
                clearState();
                return;
            }
            title = newTitle;
            artist = newArtist;
            isPlaying = nowPlaying;
            long rawDuration = info.getDuration();
            long rawPosition = info.getPosition();
            if (rawDuration > 3600000) {
                duration = rawDuration / 1000;
                position = rawPosition / 1000;
            } else {
                duration = rawDuration;
                position = rawPosition;
            }
            source = session.getOwner() != null ? session.getOwner() : "";
            lastTitle = newTitle;
        } catch (Exception e) {
        }
    }
    private void checkMediaMacOS() {
        try {
            MacOSMediaProvider.MediaInfo info = MacOSMediaProvider.getNowPlaying();
            if (info.title == null || info.title.isEmpty()) {
                clearState();
                return;
            }
            title = sanitize(info.title);
            artist = sanitize(info.artist);
            isPlaying = info.isPlaying;
            duration = (long) (info.duration * 1000);
            position = (long) (info.position * 1000);
            source = info.appName != null ? info.appName : "";
            lastTitle = title;
        } catch (Exception e) {
            System.err.println("Error in checkMediaMacOS: " + e.getMessage());
        }
    }
    private void checkMediaWindows() {
        try {
            WindowsMediaProvider.MediaInfo info = WindowsMediaProvider.getNowPlaying();
            if (info.title == null || info.title.isEmpty()) {
                clearState();
                return;
            }
            title = sanitize(info.title);
            artist = sanitize(info.artist);
            isPlaying = info.isPlaying;
            duration = info.duration;
            position = info.position;
            source = "Windows Media";
            lastTitle = title;
        } catch (Exception e) {
            System.err.println("Error in checkMediaWindows: " + e.getMessage());
        }
    }
    ///  Р§РёСЃС‚Рѕ С‡Рµ Р·РЅР°СЋ С‚Рѕ РґРѕР±РЅСѓР» РјР± С‡РµРіРѕ С‚Рѕ РЅРµС‚ РЅРѕ РѕСЃРЅР°РІР° С‡РµСЂРµР· Р±СЂР°СѓР·РµСЂ
    private int getSourcePriority(String owner) {
        if (owner == null) return 0;
        owner = owner.toLowerCase();
        if (owner.contains("spotify")) return 100;
        if (owner.contains("aimp")) return 95;
        if (owner.contains("foobar")) return 90;
        if (owner.contains("vlc")) return 85;
        if (owner.contains("musicbee")) return 80;
        if (owner.contains("winamp")) return 75;
        if (owner.contains("itunes")) return 70;
        if (owner.contains("yandex") && owner.contains("music")) return 65;
        if (owner.contains("deezer")) return 60;
        if (owner.contains("tidal")) return 55;
        if (owner.contains("chrome")) return 20;
        if (owner.contains("firefox")) return 20;
        if (owner.contains("edge")) return 20;
        if (owner.contains("opera")) return 20;
        if (owner.contains("brave")) return 20;
        if (owner.contains("yandex")) return 20;
        return 10;
    }
    private void clearState() {
        title = "";
        artist = "";
        source = "";
        duration = 0;
        position = 0;
        isPlaying = false;
        lastTitle = "";
    }
    private String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if ((c >= 32 && c <= 126) || (c >= 0x410 && c <= 0x44F) || c == 0x401 || c == 0x451 || c == ' ') {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
    public void playPause() {
        if (USE_MAC_PROVIDER) {
            MacOSMediaProvider.sendMediaCommand("playpause");
        } else if (USE_WINDOWS_PROVIDER) {
            WindowsMediaProvider.sendMediaCommand("playpause");
        } else {
            sendMediaKey(0xB3);
        }
    }
    public void next() {
        if (USE_MAC_PROVIDER) {
            MacOSMediaProvider.sendMediaCommand("next");
        } else if (USE_WINDOWS_PROVIDER) {
            WindowsMediaProvider.sendMediaCommand("next");
        } else {
            sendMediaKey(0xB0);
        }
    }
    public void previous() {
        if (USE_MAC_PROVIDER) {
            MacOSMediaProvider.sendMediaCommand("previous");
        } else if (USE_WINDOWS_PROVIDER) {
            WindowsMediaProvider.sendMediaCommand("previous");
        } else {
            sendMediaKey(0xB1);
        }
    }
    private void sendMediaKey(int vk) {
        new Thread(() -> {
            try {
                String cmd = "powershell -NoProfile -Command \"" +
                        "Add-Type -TypeDefinition 'using System;using System.Runtime.InteropServices;public class K{[DllImport(\\\"user32.dll\\\")]public static extern void keybd_event(byte k,byte s,uint f,int e);}' -Language CSharp;" +
                        "[K]::keybd_event(" + vk + ",0,0,0);Start-Sleep -Milliseconds 50;[K]::keybd_event(" + vk + ",0,2,0)\"";
                Runtime.getRuntime().exec(cmd);
            } catch (Exception e) {
            }
        }).start();
    }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getSource() { return source; }
    public boolean isPlaying() { return isPlaying; }
    public boolean hasMedia() { return !title.isEmpty(); }
    public long getDuration() { return duration; }
    public long getPosition() { return position; }
    public float getProgress() {
        if (duration <= 0) return 0f;
        return Math.min(1f, (float) position / duration);
    }
    public void shutdown() {
        running.set(false);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
    }
    public static String formatTime(long ms) {
        if (ms <= 0) return "0:00";
        long seconds = ms / 1000;
        long mins = seconds / 60;
        long secs = seconds % 60;
        return mins + ":" + (secs < 10 ? "0" : "") + secs;
    }
}
