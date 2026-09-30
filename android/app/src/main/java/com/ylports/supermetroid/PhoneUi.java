package com.ylports.supermetroid;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class PhoneUi {
    static final int BACKGROUND = Color.rgb(12, 16, 25);
    static final int ACCENT = Color.rgb(255, 172, 66);
    static int dp(Activity a, float value) { return Math.round(value * a.getResources().getDisplayMetrics().density); }
    static void immersive(Activity a) {
        a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams p = a.getWindow().getAttributes();
        p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        a.getWindow().setAttributes(p);
        if (Build.VERSION.SDK_INT >= 30) {
            a.getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = a.getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    static TextView text(Activity a, String value, int size, int color) {
        TextView t = new TextView(a);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        return t;
    }
    static Button button(Activity a, String title, boolean primary) {
        Button b = new Button(a);
        b.setText(title); b.setAllCaps(false); b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? BACKGROUND : Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary ? ACCENT : Color.rgb(28, 36, 51));
        bg.setCornerRadius(dp(a, 14));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(a, 52));
        lp.setMargins(0, dp(a, 7), 0, dp(a, 7)); b.setLayoutParams(lp);
        return b;
    }
}
