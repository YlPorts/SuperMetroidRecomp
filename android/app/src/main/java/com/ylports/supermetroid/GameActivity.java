package com.ylports.supermetroid;

import android.app.AlertDialog;
import android.content.*;
import android.hardware.input.InputManager;
import android.os.*;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.widget.RelativeLayout;
import android.widget.Toast;
import org.libsdl.app.SDLActivity;
import java.io.*;
import java.nio.file.Files;

/** Phone UI publishes commands; only the native SDL thread executes them. */
public final class GameActivity extends SDLActivity implements TouchControlsView.Host, InputManager.InputDeviceListener {
    private static final int EXPORT_SRAM=40;
    private AppSettings settings;
    private TouchControlsView controls;
    private InputManager inputs;
    private boolean menuVisible, settingsVisible, operation, activityResumed;
    private android.window.OnBackInvokedCallback backCallback;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private static final int[] MASKS={TouchInput.UP,TouchInput.DOWN,TouchInput.LEFT,TouchInput.RIGHT,
            TouchInput.A,TouchInput.B,TouchInput.X,TouchInput.Y,TouchInput.L,TouchInput.R,TouchInput.START,TouchInput.SELECT};
    private static final int[] KEYS={KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_X,KeyEvent.KEYCODE_Z,KeyEvent.KEYCODE_S,KeyEvent.KEYCODE_A,KeyEvent.KEYCODE_C,KeyEvent.KEYCODE_V,
            KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_SHIFT_RIGHT};
    public static native void nativeVideoSettings(boolean enhanced,int aspect,boolean hud);
    public static native void nativeOptions(int volume,int presentation,boolean integerScale,boolean safeWide);
    public static native void nativeSetPaused(boolean paused);
    public static native void nativeCommand(int command);
    public static native int nativeCommandResult();
    public static native String nativeDiagnostics();
    public static native void nativeInsets(int left,int top,int right,int bottom);
    @Override public void safeArea(int left,int top,int right,int bottom) { nativeInsets(left,top,right,bottom); }
    @Override public Intent registerReceiver(BroadcastReceiver r,IntentFilter f) {
        if(Build.VERSION.SDK_INT>=33&&f!=null&&f.hasAction("org.libsdl.app.USB_PERMISSION"))
            return super.registerReceiver(r,f,Context.RECEIVER_NOT_EXPORTED);
        return super.registerReceiver(r,f);
    }
    @Override protected String[] getLibraries() { return new String[]{"SDL2","main"}; }
    @Override protected String[] getArguments() { return new String[]{"--no-launcher",new AppSettings(this).rom().getAbsolutePath()}; }
    @Override protected void onCreate(Bundle state) {
        settings=new AppSettings(this); super.onCreate(state);
        if(mBrokenLibraries) return;
        PhoneUi.immersive(this);
        android.view.WindowManager.LayoutParams w=getWindow().getAttributes();
        w.preferredRefreshRate=60; getWindow().setAttributes(w);
        controls=new TouchControlsView(this,settings,this);
        mLayout.addView(controls,new RelativeLayout.LayoutParams(-1,-1)); controls.requestApplyInsets();
        applySettings();
        inputs=(InputManager)getSystemService(INPUT_SERVICE);
        inputs.registerInputDeviceListener(this,handler); updateGamepads();
        if(Build.VERSION.SDK_INT>=33) {
            backCallback=this::handleBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback);
        }
    }
    private void applySettings() {
        if(controls==null) return;
        controls.reload(); updateGamepads();
        controls.requestApplyInsets();
        nativeVideoSettings(settings.wide(),settings.aspect(),settings.hud());
        nativeOptions(settings.volume(),settings.presentation(),settings.integerScale(),settings.safeWide());
        try { settings.writeVideo(); }
        catch(IOException e) { message("No se pudieron guardar los ajustes de pantalla."); }
    }
    @Override public void keys(int down,int up) {
        for(int i=0;i<MASKS.length;i++) {
            if((up&MASKS[i])!=0) SDLActivity.onNativeKeyUp(KEYS[i]);
            if((down&MASKS[i])!=0) SDLActivity.onNativeKeyDown(KEYS[i]);
        }
    }
    private void syncPause() {
        if(controls!=null) nativeSetPaused(!activityResumed||!hasWindowFocus()||menuVisible||settingsVisible
                ||operation||controls.isEditing()||isFinishing());
    }
    @Override protected void onPause() {
        activityResumed=false; if(controls!=null) { controls.releaseAll(); syncPause(); } super.onPause();
    }
    @Override protected void onResume() { super.onResume(); activityResumed=true; PhoneUi.immersive(this); syncPause(); }
    @Override public void onWindowFocusChanged(boolean focus) {
        if(!focus&&controls!=null) controls.releaseAll(); super.onWindowFocusChanged(focus);
        if(focus) PhoneUi.immersive(this); syncPause();
    }
    @Override public void onBackPressed() { handleBack(); }
    private void handleBack() { if(controls!=null&&controls.isEditing()) editDone(); else menu(); }
    @Override public void menu() {
        if(menuVisible||settingsVisible||operation||controls==null) return;
        controls.releaseAll(); menuVisible=true; syncPause();
        String[] items={"Continuar","Guardar estado rápido","Cargar estado rápido","Ajustes","Editar controles",
                "Restablecer controles","Exportar partida SNES","Diagnóstico de esta sesión","Cerrar juego"};
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Super Metroid").setItems(items,(d,which)->{
            // Transfer ownership before dismiss: no resume between nested UI.
            if(which==3||which==7) settingsVisible=true;
            if(which==4) controls.edit(true);
            if(which==1||which==2||which==6||which==8) operation=true;
            d.dismiss();
            if(which==1) command(2,()->{operation=false; syncPause(); message("Estado guardado");});
            else if(which==2) command(4,()->{operation=false; syncPause(); message("Estado cargado");});
            else if(which==3) showSettings();
            else if(which==5) controls.resetLayout();
            else if(which==6) exportSram();
            else if(which==7) diagnostics();
            else if(which==8) command(1,this::finish);
        }).create();
        dialog.setOnDismissListener(d->{menuVisible=false; syncPause();}); dialog.show();
    }
    private void command(int cmd,Runnable success) {
        nativeCommand(cmd); pollCommand(SystemClock.uptimeMillis()+5000,success);
    }
    private void pollCommand(long deadline,Runnable success) {
        if(isDestroyed()) return;
        int result=nativeCommandResult();
        if(result>0) { success.run(); return; }
        if(result<0||SystemClock.uptimeMillis()>deadline) {
            operation=false; syncPause(); message("No se pudo completar la operación. Se conserva la partida anterior."); return;
        }
        handler.postDelayed(()->pollCommand(deadline,success),25);
    }
    private void showSettings() {
        settingsVisible=true; syncPause();
        SettingsDialog.show(this,settings,this::applySettings,()->{
            settingsVisible=false; syncPause();
        });
    }
    @Override public void editControl(String id) {
        settingsVisible=true; syncPause();
        SettingsDialog.control(this,settings,id,controls::reload,()->{settingsVisible=false; syncPause();});
    }
    @Override public void editDone() { controls.edit(false); syncPause(); }
    private void diagnostics() {
        settingsVisible=true; syncPause();
        new AlertDialog.Builder(this).setTitle("Diagnóstico de esta sesión").setMessage(nativeDiagnostics())
                .setPositiveButton("Listo",null).setOnDismissListener(d->{settingsVisible=false; syncPause();}).show();
    }
    private void exportSram() {
        command(1,()->{
            try { startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,"SuperMetroid-partida.srm"),EXPORT_SRAM); }
            catch(ActivityNotFoundException e) { operation=false; syncPause(); message("No hay selector de archivos disponible."); }
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=EXPORT_SRAM) return;
        if(result==RESULT_OK&&data!=null&&data.getData()!=null) {
            try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")) {
                if(out==null) throw new IOException("Sin acceso al archivo.");
                Files.copy(new File(settings.directory(),"saves/save.srm").toPath(),out);
                message("Partida SNES exportada");
            } catch(IOException|SecurityException e) { message("No se pudo exportar la partida."); }
        }
        operation=false; syncPause();
    }
    private void message(String text) { Toast.makeText(this,text,Toast.LENGTH_LONG).show(); }
    private void updateGamepads() {
        if(controls==null) return;
        boolean pad=false;
        for(int id:InputDevice.getDeviceIds()) {
            InputDevice d=InputDevice.getDevice(id);
            if(d!=null&&!d.isVirtual()&&((d.getSources()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD
                    ||(d.getSources()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK)) { pad=true; break; }
        }
        controls.hideForGamepad(pad&&settings.prefs.getBoolean("autoHide",true));
    }
    public void onInputDeviceAdded(int id) { updateGamepads(); }
    public void onInputDeviceRemoved(int id) { updateGamepads(); }
    public void onInputDeviceChanged(int id) { updateGamepads(); }
    @Override protected void onDestroy() {
        if(controls!=null) controls.releaseAll(); if(inputs!=null) inputs.unregisterInputDeviceListener(this);
        if(Build.VERSION.SDK_INT>=33&&backCallback!=null)
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        handler.removeCallbacksAndMessages(null); super.onDestroy();
    }
}
