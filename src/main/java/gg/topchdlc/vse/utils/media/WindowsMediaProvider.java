package gg.topchdlc.vse.utils.media;

public class WindowsMediaProvider {
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    public static class MediaInfo {
        public String title = "";
        public String artist = "";
        public String album = "";
        public boolean isPlaying = false;
        public long duration = 0;
        public long position = 0;
    }
    public static MediaInfo getNowPlaying() {
        MediaInfo info = new MediaInfo();
        
        if (!IS_WINDOWS) {
            return info;
        }
        try {
            String script = 
                "$sessions = (Get-Process).Where({$_.MainWindowTitle -ne ''}) | " +
                "ForEach-Object { " +
                "  try { " +
                "    $smtc = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media, ContentType = WindowsRuntime]::RequestAsync().GetAwaiter().GetResult(); " +
                "    $session = $smtc.GetCurrentSession(); " +
                "    if ($session) { " +
                "      $mediaProps = $session.TryGetMediaPropertiesAsync().GetAwaiter().GetResult(); " +
                "      $playback = $session.GetPlaybackInfo(); " +
                "      $timeline = $session.GetTimelineProperties(); " +
                "      Write-Output \"$($mediaProps.Title)|$($mediaProps.Artist)|$($mediaProps.AlbumTitle)|$($playback.PlaybackStatus)|$($timeline.EndTime.TotalSeconds)|$($timeline.Position.TotalSeconds)\"; " +
                "    } " +
                "  } catch {} " +
                "}; ";
            
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive", 
                "-Command",
                "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media, ContentType = WindowsRuntime] | Out-Null; " +
                "$smtc = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync().GetAwaiter().GetResult(); " +
                "$session = $smtc.GetCurrentSession(); " +
                "if ($session) { " +
                "  $mediaProps = $session.TryGetMediaPropertiesAsync().GetAwaiter().GetResult(); " +
                "  $playback = $session.GetPlaybackInfo(); " +
                "  $timeline = $session.GetTimelineProperties(); " +
                "  Write-Output \"$($mediaProps.Title)|$($mediaProps.Artist)|$($mediaProps.AlbumTitle)|$($playback.PlaybackStatus)|$($timeline.EndTime.TotalSeconds)|$($timeline.Position.TotalSeconds)\"; " +
                "}"
            );
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8")
            );
            String line = reader.readLine();
            process.waitFor();
            if (line != null && !line.isEmpty() && line.contains("|")) {
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 6) {
                    info.title = parts[0];
                    info.artist = parts[1];
                    info.album = parts[2];
                    String status = parts[3];
                    info.isPlaying = status.equals("4") || status.toLowerCase().contains("playing");
                    try {
                        info.duration = (long) (Double.parseDouble(parts[4]) * 1000);
                        info.position = (long) (Double.parseDouble(parts[5]) * 1000);
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting Windows media info: " + e.getMessage());
        }
        return info;
    }
    public static void sendMediaCommand(String command) {
        if (!IS_WINDOWS) return;
        
        int vkCode = 0;
        switch (command.toLowerCase()) {
            case "playpause":
                vkCode = 0xB3;
                break;
            case "next":
                vkCode = 0xB0;
                break;
            case "previous":
                vkCode = 0xB1;
                break;
        }
        if (vkCode != 0) {
            sendMediaKeyPowerShell(vkCode);
        }
    }
    private static void sendMediaKeyPowerShell(int vk) {
        new Thread(() -> {
            try {
                String cmd = String.format(
                    "powershell.exe -NoProfile -Command \"" +
                    "Add-Type -TypeDefinition 'using System;using System.Runtime.InteropServices;" +
                    "public class K{[DllImport(\\\"user32.dll\\\")]" +
                    "public static extern void keybd_event(byte k,byte s,uint f,int e);}' -Language CSharp;" +
                    "[K]::keybd_event(%d,0,0,0);Start-Sleep -Milliseconds 50;[K]::keybd_event(%d,0,2,0)\"",
                    vk, vk
                );
                Runtime.getRuntime().exec(cmd);
            } catch (Exception e) {
                System.err.println("Error sending media key: " + e.getMessage());
            }
        }).start();
    }
    public static boolean isAvailable() {
        return IS_WINDOWS;
    }
}
