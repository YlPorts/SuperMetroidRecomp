package com.ylports.supermetroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LauncherActivity extends Activity {
    private static final int PICK_ROM = 14;
    private AppSettings settings;
    private Button play, importButton;
    private TextView status;
    private boolean importing;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); settings = new AppSettings(this); PhoneUi.immersive(this);
        LinearLayout outer = new LinearLayout(this); outer.setGravity(Gravity.CENTER);
        outer.setBackgroundColor(PhoneUi.BACKGROUND);
        outer.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = PhoneUi.dp(this, 24), right = left;
            if (insets.getDisplayCutout() != null) {
                left += insets.getDisplayCutout().getSafeInsetLeft();
                right += insets.getDisplayCutout().getSafeInsetRight();
            }
            v.setPadding(left, PhoneUi.dp(this, 16), right, PhoneUi.dp(this, 16));
            return insets;
        });
        LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_VERTICAL);
        outer.addView(left, new LinearLayout.LayoutParams(0, -1, 1.1f));
        TextView eyebrow = PhoneUi.text(this, "YL PORTS   /   ANDROID", 12, PhoneUi.ACCENT);
        left.addView(eyebrow);
        TextView title = PhoneUi.text(this, "SUPER\nMETROID", 36, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); left.addView(title);
        TextView subtitle = PhoneUi.text(this, "Explora Zebes a pantalla completa.\nUltrawide y controles a tu medida.", 15, Color.LTGRAY);
        subtitle.setPadding(0, PhoneUi.dp(this, 12), PhoneUi.dp(this, 18), 0); left.addView(subtitle);
        ScrollView scroll = new ScrollView(this);
        outer.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.VERTICAL);
        actions.setPadding(PhoneUi.dp(this, 20), PhoneUi.dp(this, 16), 0, 0); scroll.addView(actions);
        play = PhoneUi.button(this, "Jugar", true); actions.addView(play);
        importButton = PhoneUi.button(this, "Elegir ROM", false); actions.addView(importButton);
        Button options = PhoneUi.button(this, "Pantalla y controles", false); actions.addView(options);
        status = PhoneUi.text(this, "", 13, Color.LTGRAY); actions.addView(status);
        play.setOnClickListener(v -> launchGame());
        importButton.setOnClickListener(v -> pickRom());
        options.setOnClickListener(v -> SettingsDialog.show(this, settings, this::refresh, this::refresh));
        setContentView(outer); outer.requestApplyInsets(); refresh();
    }

    @Override protected void onResume() {
        super.onResume(); settings = new AppSettings(this); PhoneUi.immersive(this);
        if (play != null) refresh();
    }
    private void refresh() {
        boolean ready = settings.rom().isFile() && settings.rom().length() == RomImporter.ROM_SIZE;
        play.setEnabled(ready && !importing && BuildConfig.ENGINE_COMPILED);
        play.setAlpha(play.isEnabled() ? 1 : 0.45f); importButton.setEnabled(!importing);
        status.setText(importing ? "Comprobando e importando…" : !BuildConfig.ENGINE_COMPILED
                ? "Compilación de verificación. El motor de juego todavía no está generado."
                : ready ? "ROM lista · " + (settings.wide() ? AppSettings.ASPECTS[settings.aspect()].replace("Fit", "Tu pantalla") : "4:3")
                : "Elige tu Super Metroid (Japan, USA)\nen .sfc o .smc. Se importa una sola vez.");
    }
    private void pickRom() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try { startActivityForResult(intent, PICK_ROM); }
        catch (ActivityNotFoundException e) { error("No se encontró un selector de archivos en este dispositivo."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_ROM || result != RESULT_OK || data == null || data.getData() == null) return;
        importing = true; refresh();
        android.net.Uri uri = data.getData();
        worker.execute(() -> {
            String failure = null;
            try { RomImporter.importRom(getContentResolver().openInputStream(uri), settings.rom()); }
            catch (IOException | SecurityException e) { failure = e.getMessage(); }
            String message = failure;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                importing = false; refresh();
                if (message != null) error(message);
            });
        });
    }
    private void launchGame() {
        try {
            settings.prepareGame();
            startActivity(new Intent(this, GameActivity.class));
        } catch (IOException e) { error(e.getMessage()); }
    }
    private void error(String text) { new AlertDialog.Builder(this).setTitle("Super Metroid").setMessage(text).setPositiveButton("Entendido", null).show(); }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
