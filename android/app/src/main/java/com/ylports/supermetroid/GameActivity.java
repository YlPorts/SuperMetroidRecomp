package com.ylports.supermetroid;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.widget.RelativeLayout;
import android.widget.Toast;
import org.libsdl.app.SDLActivity;
import java.io.IOException;

/** SDL's activity remains unmodified; this subclass owns all phone UI. */
public final class GameActivity extends SDLActivity implements TouchControlsView.Host, InputManager.InputDeviceListener {
    private AppSettings settings;
    private TouchControlsView controls;
    private InputManager inputs;
    private boolean menuVisible;
    private android.window.OnBackInvokedCallback backCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int[] MASKS={TouchInput.UP,TouchInput.DOWN,TouchInput.LEFT,TouchInput.RIGHT,
            TouchInput.A,TouchInput.B,TouchInput.X,TouchInput.Y,TouchInput.L,TouchInput.R,TouchInput.START,TouchInput.SELECT};
    private static final int[] KEYS={KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_X,KeyEvent.KEYCODE_Z,KeyEvent.KEYCODE_S,KeyEvent.KEYCODE_A,KeyEvent.KEYCODE_C,KeyEvent.KEYCODE_V,
            KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_SHIFT_RIGHT};
    public static native void nativeVideoSettings(boolean enhanced,int aspect,boolean hud);
    // SDL 2.32.8 registers a filter containing both system USB broadcasts and
    // its private permission action without the Android 13+ export flag.
    // Keep the vendored SDL glue intact; qualify that one registration here.
    @Override public Intent registerReceiver(BroadcastReceiver receiver,IntentFilter filter) {
        if(Build.VERSION.SDK_INT>=33&&filter!=null&&filter.hasAction("org.libsdl.app.USB_PERMISSION"))
            return super.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);
        return super.registerReceiver(receiver,filter);
    }
    @Override protected String[] getLibraries() { return new String[]{"SDL2","main"}; }
    @Override protected String[] getArguments() { return new String[]{"--no-launcher",new AppSettings(this).rom().getAbsolutePath()}; }
    @Override protected void onCreate(Bundle state) {
        settings=new AppSettings(this);
        super.onCreate(state);
        if(mBrokenLibraries) return;
        PhoneUi.immersive(this);
        android.view.WindowManager.LayoutParams window=getWindow().getAttributes();
        window.preferredRefreshRate=60f; getWindow().setAttributes(window);
        controls=new TouchControlsView(this,settings,this);
        mLayout.addView(controls,new RelativeLayout.LayoutParams(-1,-1));
        controls.requestApplyInsets();
        nativeVideoSettings(settings.wide(),settings.aspect(),settings.hud());
        inputs=(InputManager)getSystemService(INPUT_SERVICE);
        inputs.registerInputDeviceListener(this,handler); updateGamepads();
        if(Build.VERSION.SDK_INT>=33) {
            backCallback=this::handleBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback);
        }
    }
    @Override public void keys(int pressed,int released) {
        for(int i=0;i<MASKS.length;i++) {
            if((released&MASKS[i])!=0) SDLActivity.onNativeKeyUp(KEYS[i]);
            if((pressed&MASKS[i])!=0) SDLActivity.onNativeKeyDown(KEYS[i]);
        }
    }
    @Override protected void onPause() { if(controls!=null) controls.releaseAll(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); PhoneUi.immersive(this); }
    @Override public void onWindowFocusChanged(boolean focus) {
        if(!focus&&controls!=null) controls.releaseAll(); super.onWindowFocusChanged(focus);
        if(focus) PhoneUi.immersive(this);
    }
    @Override public void onBackPressed() { handleBack(); }
    private void handleBack() {
        if(controls!=null&&controls.isEditing()) editDone(); else menu();
    }
    private void pauseForUi() { controls.releaseAll(); pauseNativeThread(); }
    private void resumeGame() { if(!isFinishing()) { resumeNativeThread(); PhoneUi.immersive(this); } }
    @Override public void menu() {
        if(menuVisible||controls==null) return;
        menuVisible=true; pauseForUi();
        String[] items={"Continuar","Guardar estado rápido","Cargar estado rápido","Pantalla y controles","Mover botones","Restablecer botones","Salir al inicio"};
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Super Metroid").setItems(items,(d,which)->{
            d.dismiss();
            handler.post(()->{
                if(which==1) pulse(KeyEvent.KEYCODE_F2);
                else if(which==2) pulse(KeyEvent.KEYCODE_F1);
                else if(which==3) showSettings();
                else if(which==4) { pauseForUi(); controls.edit(true); }
                else if(which==5) controls.resetLayout();
                else if(which==6) finish();
            });
        }).create();
        dialog.setOnDismissListener(d->{menuVisible=false; resumeGame();}); dialog.show();
    }
    private void pulse(int key) {
        SDLActivity.onNativeKeyDown(key);
        handler.postDelayed(()->SDLActivity.onNativeKeyUp(key),80);
    }
    private void showSettings() {
        pauseForUi();
        SettingsDialog.show(this,settings,()->{
            controls.reload(); updateGamepads();
            nativeVideoSettings(settings.wide(),settings.aspect(),settings.hud());
            try { settings.writeVideo(); }
            catch(IOException e) { Toast.makeText(this,"No se pudo guardar la configuración.",Toast.LENGTH_LONG).show(); }
        },this::resumeGame);
    }
    @Override public void editDone() { controls.edit(false); resumeGame(); }
    private void updateGamepads() {
        if(controls==null) return;
        boolean pad=false;
        for(int id:InputDevice.getDeviceIds()) {
            InputDevice device=InputDevice.getDevice(id);
            if(device!=null&&!device.isVirtual()&&((device.getSources()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD
                    ||(device.getSources()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK)) { pad=true; break; }
        }
        controls.hideForGamepad(pad&&settings.prefs.getBoolean("autoHide",true));
    }
    public void onInputDeviceAdded(int id) { updateGamepads(); }
    public void onInputDeviceRemoved(int id) { updateGamepads(); }
    public void onInputDeviceChanged(int id) { updateGamepads(); }
    @Override protected void onDestroy() {
        if(controls!=null) controls.releaseAll();
        if(inputs!=null) inputs.unregisterInputDeviceListener(this);
        if(Build.VERSION.SDK_INT>=33&&backCallback!=null)
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        handler.removeCallbacksAndMessages(null); super.onDestroy();
    }
}
