package com.ylports.supermetroid;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

final class AppSettings {
    static final String[] ASPECTS = {"4:3", "16:9", "21:9", "32:9", "Fit"};
    final SharedPreferences prefs;
    private final Context context;
    AppSettings(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }
    File directory() {
        File ext = context.getExternalFilesDir(null);
        return ext == null ? context.getFilesDir() : ext;
    }
    File rom() { return new File(directory(), "supermetroid.sfc"); }
    int aspect() { return Math.max(0, Math.min(4, prefs.getInt("aspect", 4))); }
    boolean wide() { return prefs.getBoolean("wide", true); }
    boolean hud() { return prefs.getBoolean("hud", true); }
    int size() { return Math.max(70, Math.min(145, prefs.getInt("size", 100))); }
    int opacity() { return Math.max(20, Math.min(90, prefs.getInt("opacity", 50))); }
    boolean vibration() { return prefs.getBoolean("vibration", false); }
    void writeVideo() throws IOException {
        write("sm-video.ini", "[SuperMetroidVideo]\nEnhancedRenderer=" + (wide() ? 1 : 0)
                + "\nAspect=" + ASPECTS[aspect()] + "\nPresentationEnabled=0\nPresentationFPS=60\nHudAnchored="
                + (hud() ? 1 : 0) + "\n");
    }
    void prepareGame() throws IOException {
        writeVideo();
        write("rom.cfg", rom().getAbsolutePath() + "\n");
        // Seed once. Subsequent launches keep the engine's saved settings.
        if (!new File(directory(), "config.ini").isFile()) write("config.ini",
                "[General]\nSkipLauncher=1\nRunAhead=0\nAutosave=0\nDisableFrameDelay=0\n"
                + "[Graphics]\nRenderer=opengles2\nVSync=0\nNewRenderer=1\nNoSpriteLimits=1\n"
                + "IgnoreAspectRatio=0\nFullscreen=1\nFrameBlend=0\n"
                + "[Sound]\nVolume=100\nEnableAudio=1\nAudioFreq=48000\nAudioChannels=2\nAudioSamples=2048\n"
                + "[Rewind]\nEnabled=0\n[KeyMap]\nPause=p\nLoad=F1\nSave=F2\nDisplayPerf=f\n");
        if (!new File(directory(), "keybinds.ini").isFile()) write("keybinds.ini",
                "[player1]\na=X\nb=Z\nx=S\ny=A\nl=C\nr=V\nstart=Return\nselect=Right Shift\n"
                + "up=Up\ndown=Down\nleft=Left\nright=Right\n");
        new File(directory(), "saves").mkdirs();
    }
    private void write(String name, String content) throws IOException {
        File parent = directory();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("No se pudo guardar la configuración.");
        File out = new File(parent, name), temp = new File(parent, name + ".tmp");
        try {
            try (FileOutputStream stream = new FileOutputStream(temp)) {
                stream.write(content.getBytes(StandardCharsets.UTF_8));
                stream.getFD().sync();
            }
            Files.move(temp.toPath(), out.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp.toPath()); }
    }
}
