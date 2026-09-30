package com.ylports.supermetroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

final class SettingsDialog {
    static void show(Activity activity, AppSettings settings, Runnable updated, Runnable closed) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = PhoneUi.dp(activity, 22); content.setPadding(padding, 0, padding, padding);
        Switch wide = toggle(activity, "Vista ultrawide", settings.wide());
        content.addView(wide);
        TextView label = PhoneUi.text(activity, "Proporción de pantalla", 14, Color.LTGRAY);
        content.addView(label);
        Spinner aspect = new Spinner(activity);
        String[] names = {"Original 4:3", "16:9", "Ultrawide 21:9", "Superwide 32:9", "Ajustar a mi pantalla"};
        aspect.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, names));
        aspect.setSelection(settings.aspect()); content.addView(aspect);
        Switch hud = toggle(activity, "HUD en los extremos", settings.hud()); content.addView(hud);
        slider(activity, content, "Tamaño de botones", settings.size(), 70, 145,
                value -> { settings.prefs.edit().putInt("size", value).apply(); updated.run(); });
        slider(activity, content, "Opacidad", settings.opacity(), 20, 90,
                value -> { settings.prefs.edit().putInt("opacity", value).apply(); updated.run(); });
        Switch vibration = toggle(activity, "Respuesta táctil", settings.vibration()); content.addView(vibration);
        Switch hide = toggle(activity, "Ocultar botones al conectar un mando", settings.prefs.getBoolean("autoHide", true));
        content.addView(hide);
        content.addView(PhoneUi.text(activity, "Puedes mover cada botón desde el menú del juego.\nB: saltar · Y: disparar · A: correr · X: cambiar arma", 13, Color.LTGRAY));
        ScrollView scroll = new ScrollView(activity); scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Pantalla y controles")
                .setView(scroll).setPositiveButton("Listo", (d, w) -> {
                    settings.prefs.edit().putBoolean("wide", wide.isChecked()).putInt("aspect", aspect.getSelectedItemPosition())
                            .putBoolean("hud", hud.isChecked()).putBoolean("vibration", vibration.isChecked())
                            .putBoolean("autoHide", hide.isChecked()).commit();
                    updated.run();
                }).create();
        dialog.setOnDismissListener(d -> closed.run()); dialog.show();
    }
    private static Switch toggle(Activity a, String text, boolean checked) {
        Switch s = new Switch(a); s.setText(text); s.setChecked(checked);
        s.setTextSize(15); s.setPadding(0, PhoneUi.dp(a, 12), 0, PhoneUi.dp(a, 12));
        return s;
    }
    private interface Value { void set(int value); }
    private static void slider(Activity a, LinearLayout group, String name, int current, int min, int max, Value action) {
        TextView label = PhoneUi.text(a, name + " · " + current + "%", 14, Color.LTGRAY); group.addView(label);
        SeekBar bar = new SeekBar(a); bar.setMax(max - min); bar.setProgress(current - min);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int value, boolean user) {
                label.setText(name + " · " + (value + min) + "%");
                if (user) action.set(value + min);
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        }); group.addView(bar);
    }
}
