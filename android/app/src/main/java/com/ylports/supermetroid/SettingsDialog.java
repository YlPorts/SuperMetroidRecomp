package com.ylports.supermetroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.*;

/** Every editable row has a real consumer in the view or native host. */
final class SettingsDialog {
    interface Value { void set(int value); }
    static void show(Activity a, AppSettings s, Runnable updated, Runnable closed) {
        LinearLayout root = column(a);
        Spinner category = choice(a, new String[]{"Pantalla", "Controles", "Audio", "Rendimiento"}, 0);
        root.addView(category);
        ScrollView scroll = new ScrollView(a);
        LinearLayout content = column(a); scroll.addView(content); root.addView(scroll);
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p, View selectedView, int n, long selectedId) {
                content.removeAllViews();
                if (n == 0) {
                    toggle(a, content, "Vista ultrawide", s.wide(), b -> {
                        s.prefs.edit().putBoolean("wide", b).commit(); updated.run(); });
                    select(a, content, "Relación de aspecto", new String[]{"Original 4:3", "16:9", "21:9", "32:9", "Mi pantalla"}, s.aspect(), x -> {
                        s.prefs.edit().putInt("aspect", x).commit(); updated.run(); });
                    toggle(a, content, "HUD y minimapa en los extremos", s.hud(), b -> {
                        s.prefs.edit().putBoolean("hud", b).commit(); updated.run(); });
                    toggle(a, content, "Transiciones y cinemáticas en 4:3", s.safeWide(), b -> {
                        s.prefs.edit().putBoolean("safeWide", b).commit(); updated.run(); });
                    toggle(a, content, "Escala vertical entera", s.integerScale(), b -> {
                        s.prefs.edit().putBoolean("integerScale", b).commit(); updated.run(); });
                    toggle(a, content, "Pantalla completa", s.fullscreen(), b -> {
                        s.prefs.edit().putBoolean("fullscreen", b).commit(); PhoneUi.immersive(a); updated.run(); });
                    note(a, content, "Píxeles nítidos sin filtro. El motor dibuja a 224 líneas nativas y amplía el escenario; la imagen no se estira. La escala entera puede dejar márgenes.");
                } else if (n == 1) {
                    slider(a, content, "Tamaño general", s.size(), 70, 145, "%", x -> {
                        s.prefs.edit().putInt("size", x).commit(); updated.run(); });
                    slider(a, content, "Opacidad general", s.opacity(), 20, 90, "%", x -> {
                        s.prefs.edit().putInt("opacity", x).commit(); updated.run(); });
                    toggle(a, content, "Letras A · B · X · Y", s.labels(), b -> {
                        s.prefs.edit().putBoolean("labels", b).commit(); updated.run(); });
                    toggle(a, content, "Respuesta táctil", s.vibration(), b -> {
                        s.prefs.edit().putBoolean("vibration", b).commit(); updated.run(); });
                    toggle(a, content, "Ocultar controles con mando", s.prefs.getBoolean("autoHide", true), b -> {
                        s.prefs.edit().putBoolean("autoHide", b).commit(); updated.run(); });
                    Button presets = PhoneUi.button(a, "Distribuciones predefinidas", false); content.addView(presets);
                    presets.setOnClickListener(v -> new AlertDialog.Builder(a).setTitle("Distribución")
                            .setItems(new String[]{"Compacta", "Clásica"}, (d, x) -> {
                                android.content.SharedPreferences.Editor e=s.prefs.edit().putInt("size", x==0?85:100);
                                for (String id : TouchControlsView.CONTROL_IDS)
                                    for (String key : new String[]{"x","y","scale","opacity","hit","deadZone"}) e.remove(id+"."+key);
                                e.commit(); updated.run();
                            }).show());
                    note(a, content, "Menú → Editar controles. Arrastra un botón para moverlo; tócalo para cambiar tamaño, opacidad y área táctil. La cruceta tiene zona muerta ajustable. Los mandos físicos funcionan mediante SDL.");
                } else if (n == 2) {
                    slider(a, content, "Volumen general", s.volume(), 0, 100, "%", x -> {
                        s.prefs.edit().putInt("volume", x).commit(); updated.run(); });
                    int[] values={512,1024,2048,4096}; int current=2;
                    for (int i=0;i<values.length;i++) if(values[i]==s.audioSamples()) current=i;
                    select(a, content, "Búfer del dispositivo · próximo inicio", new String[]{"512 · menor latencia", "1024 · intermedio", "2048 · equilibrado", "4096 · mayor tolerancia"}, current, x -> {
                        s.prefs.edit().putInt("audioSamples", values[x]).commit(); });
                    note(a, content, "Salida estéreo a 48 kHz mediante AAudio y alternativa SDL. La música y los efectos conservan la mezcla original del SPC700. El tamaño del búfer no cambia la velocidad del juego.");
                } else {
                    select(a, content, "Presentación", new String[]{"Cadencia original · hasta 60 FPS", "Ahorro · hasta 30 FPS"}, s.presentation()==30?1:0, x -> {
                        s.prefs.edit().putInt("presentation", x==1?30:60).commit(); updated.run(); });
                    Button balanced=PhoneUi.button(a, "Aplicar perfil equilibrado", false); content.addView(balanced);
                    balanced.setOnClickListener(v -> {
                        s.prefs.edit().putInt("presentation",60).putInt("audioSamples",2048)
                                .putBoolean("safeWide",true).putBoolean("integerScale",false).commit();
                        updated.run(); Toast.makeText(a,"Perfil aplicado; búfer en el próximo inicio",Toast.LENGTH_SHORT).show();
                    });
                    note(a, content, "OpenGL ES 2 · ARM64. La lógica conserva el reloj original de SNES. El modo de 30 FPS reduce las presentaciones, sin interpolación ni cambios de físicas. Un único reloj limita los cuadros; VSync adicional se mantiene desactivado para evitar esperas dobles.");
                    note(a, content, "El diagnóstico de la sesión está disponible desde el menú del juego.");
                }
            }
        });
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Ajustes")
                .setView(root).setPositiveButton("Listo",null).create();
        dialog.setOnDismissListener(d -> closed.run()); dialog.show();
    }
    static void control(Activity a, AppSettings s, String id, Runnable updated, Runnable closed) {
        LinearLayout content=column(a);
        slider(a,content,"Tamaño",Math.round(s.controlValue(id,"scale",1,.6f,1.6f)*100),60,160,"%",x->{
            s.prefs.edit().putFloat(id+".scale",x/100f).commit(); updated.run(); });
        slider(a,content,"Opacidad",Math.round(s.controlValue(id,"opacity",s.opacity()/100f,.2f,.9f)*100),20,90,"%",x->{
            s.prefs.edit().putFloat(id+".opacity",x/100f).commit(); updated.run(); });
        slider(a,content,"Área táctil",Math.round(s.controlValue(id,"hit",1.12f,.8f,1.4f)*100),80,140,"%",x->{
            s.prefs.edit().putFloat(id+".hit",x/100f).commit(); updated.run(); });
        if(id.equals("pad")) slider(a,content,"Zona muerta",Math.round(s.controlValue(id,"deadZone",.21f,.08f,.4f)*100),8,40,"%",x->{
            s.prefs.edit().putFloat(id+".deadZone",x/100f).commit(); updated.run(); });
        ScrollView scroll=new ScrollView(a); scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(id.equals("pad")?"Cruceta":id.toUpperCase(java.util.Locale.ROOT))
                .setView(scroll).setNeutralButton("Restablecer",(d,w)->{
                    android.content.SharedPreferences.Editor e=s.prefs.edit();
                    for(String key:new String[]{"x","y","scale","opacity","hit","deadZone"}) e.remove(id+"."+key);
                    e.commit(); updated.run();
                }).setPositiveButton("Listo",null).create();
        dialog.setOnDismissListener(d->closed.run()); dialog.show();
    }
    private static LinearLayout column(Activity a) {
        LinearLayout l=new LinearLayout(a); l.setOrientation(LinearLayout.VERTICAL);
        int p=PhoneUi.dp(a,16); l.setPadding(p,0,p,p); return l;
    }
    private static void note(Activity a, LinearLayout l,String text) {
        TextView t=PhoneUi.text(a,text,13,Color.LTGRAY); int p=PhoneUi.dp(a,10); t.setPadding(0,p,0,p); l.addView(t);
    }
    private interface Bool { void set(boolean b); }
    private static void toggle(Activity a,LinearLayout l,String title,boolean value,Bool changed) {
        Switch s=new Switch(a); s.setText(title); s.setTextSize(14); s.setChecked(value);
        int p=PhoneUi.dp(a,10); s.setPadding(0,p,0,p);
        s.setOnCheckedChangeListener((b,on)->changed.set(on)); l.addView(s);
    }
    private static Spinner choice(Activity a,String[] names,int value) {
        Spinner s=new Spinner(a); s.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));
        s.setSelection(value); return s;
    }
    private static void select(Activity a,LinearLayout l,String name,String[] names,int value,Value changed) {
        note(a,l,name); Spinner s=choice(a,names,value); l.addView(s);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int current=value;
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p,View v,int n,long id) {
                if(n!=current) { current=n; changed.set(n); }
            }
        });
    }
    private static void slider(Activity a,LinearLayout l,String title,int value,int min,int max,String unit,Value changed) {
        TextView label=PhoneUi.text(a,title+" · "+value+unit,14,Color.LTGRAY); l.addView(label);
        SeekBar bar=new SeekBar(a); bar.setMax(max-min); bar.setProgress(value-min); l.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b,int n,boolean user) {
                label.setText(title+" · "+(n+min)+unit); if(user) changed.set(n+min);
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
    }
}
