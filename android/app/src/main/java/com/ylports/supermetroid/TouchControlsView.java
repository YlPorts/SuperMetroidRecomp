package com.ylports.supermetroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Draws only on input/layout changes; retains no animation loop over gameplay. */
final class TouchControlsView extends View {
    interface Host { void keys(int pressed, int released); void menu(); void editDone(); }
    private static final int PAD = -1, MENU = -2;
    private static final class Control {
        final String id, title, subtitle; final int mask; final float defaultX, defaultY, factor;
        float x, y, radius;
        Control(String id, String title, String subtitle, int mask, float x, float y, float factor) {
            this.id=id; this.title=title; this.subtitle=subtitle; this.mask=mask;
            defaultX=x; defaultY=y; this.factor=factor;
        }
    }
    private final AppSettings settings;
    private final Host host;
    private final TouchInput input;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF usable = new RectF();
    private final List<Control> controls = new ArrayList<>();
    private final Map<Integer, Control> owners = new HashMap<>();
    private int padPointer = -1;
    private boolean editing, hidden;
    private Control dragged;
    private int dragPointer = -1;
    private float dragDx, dragDy, safeLeft, safeTop, safeRight, safeBottom;

    TouchControlsView(Context context, AppSettings settings, Host host) {
        super(context); this.settings=settings; this.host=host;
        setContentDescription("Controles de Super Metroid; menú en el centro superior");
        setFocusable(false);
        input = new TouchInput((pressed, released) -> {
            host.keys(pressed, released);
            if (pressed != 0 && settings.vibration()) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            invalidate();
        });
        controls.add(new Control("pad", "", "", PAD, .115f, .72f, 2.05f));
        controls.add(new Control("b", "B", "SALTAR", TouchInput.B, .875f, .84f, 1.12f));
        controls.add(new Control("y", "Y", "DISPARAR", TouchInput.Y, .795f, .66f, 1.12f));
        controls.add(new Control("a", "A", "CORRER", TouchInput.A, .953f, .66f, .92f));
        controls.add(new Control("x", "X", "ARMA", TouchInput.X, .875f, .475f, .92f));
        controls.add(new Control("l", "L", "APUNTAR", TouchInput.L, .12f, .32f, .88f));
        controls.add(new Control("r", "R", "APUNTAR", TouchInput.R, .92f, .29f, .88f));
        controls.add(new Control("select", "SEL", "", TouchInput.SELECT, .43f, .88f, .76f));
        controls.add(new Control("start", "START", "", TouchInput.START, .55f, .88f, .76f));
        controls.add(new Control("menu", "≡", "", MENU, .50f, .115f, .72f));
        setOnApplyWindowInsetsListener((v, insets) -> {
            safeLeft=safeTop=safeRight=safeBottom=0;
            if (insets.getDisplayCutout()!=null) {
                safeLeft=insets.getDisplayCutout().getSafeInsetLeft();
                safeTop=insets.getDisplayCutout().getSafeInsetTop();
                safeRight=insets.getDisplayCutout().getSafeInsetRight();
                safeBottom=insets.getDisplayCutout().getSafeInsetBottom();
            }
            layoutControls(); return insets;
        });
    }
    void reload() { layoutControls(); }
    void hideForGamepad(boolean hide) { hidden=hide; releaseAll(); invalidate(); }
    void releaseAll() { owners.clear(); padPointer=-1; input.clear(); }
    void edit(boolean enabled) { releaseAll(); editing=enabled; dragged=null; dragPointer=-1; invalidate(); }
    boolean isEditing() { return editing; }
    void resetLayout() {
        android.content.SharedPreferences.Editor e = settings.prefs.edit();
        for (Control c:controls) { e.remove(c.id+".x"); e.remove(c.id+".y"); }
        e.commit(); layoutControls();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) { releaseAll(); layoutControls(); }
    private float density() { return getResources().getDisplayMetrics().density; }
    private static float clamp(float n,float min,float max) { return Math.max(min,Math.min(max,n)); }
    private void layoutControls() {
        float edge=8*density();
        usable.set(safeLeft+edge,safeTop+edge,getWidth()-safeRight-edge,getHeight()-safeBottom-edge);
        if (usable.width()<=0 || usable.height()<=0) return;
        float base=clamp(usable.height()*.088f,26*density(),36*density())*settings.size()/100f;
        for(Control c:controls) {
            c.radius=c.mask==MENU ? 22*density() : base*c.factor;
            c.radius=Math.min(c.radius, usable.height()*.24f);
            float fx=settings.prefs.getFloat(c.id+".x",c.defaultX), fy=settings.prefs.getFloat(c.id+".y",c.defaultY);
            c.x=clamp(usable.left+fx*usable.width(),usable.left+c.radius,usable.right-c.radius);
            c.y=clamp(usable.top+fy*usable.height(),usable.top+c.radius,usable.bottom-c.radius);
        }
        invalidate();
    }
    private Control hit(float x,float y) {
        Control best=null; float distance=Float.MAX_VALUE;
        for(Control c:controls) {
            if(hidden&&!editing&&c.mask!=MENU) continue;
            float dx=x-c.x,dy=y-c.y;
            float targetRadius=Math.max(c.radius,20*density());
            float ratio=(dx*dx+dy*dy)/(targetRadius*targetRadius);
            if(ratio<1.45f && ratio<distance) { best=c; distance=ratio; }
        }
        return best;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked(), index=event.getActionIndex(), id=event.getPointerId(index);
        if(action==MotionEvent.ACTION_CANCEL) { releaseAll(); dragged=null; dragPointer=-1; return true; }
        if(editing) return editTouch(event, action, index, id);
        if(action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN) {
            Control c=hit(event.getX(index),event.getY(index));
            if(c!=null) {
                if(c.mask==PAD) {
                    if(padPointer<0) { padPointer=id; owners.put(id,c); }
                } else owners.put(id,c);
            }
        }
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP) {
            Control old=owners.remove(id); input.release(id);
            if(padPointer==id) padPointer=-1;
            if(old!=null&&old.mask==MENU&&hit(event.getX(index),event.getY(index))==old) { performClick(); host.menu(); }
        }
        for(int i=0;i<event.getPointerCount();i++) {
            int pointer=event.getPointerId(i);
            if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP)&&i==index) continue;
            Control c=owners.get(pointer);
            if(c==null||c.mask==MENU) continue;
            if(c.mask==PAD) input.pointer(pointer,TouchInput.direction(event.getX(i)-c.x,event.getY(i)-c.y,c.radius));
            else {
                // A captured face-button finger may slide between buttons, but
                // never acquire the d-pad or open a menu halfway through a press.
                Control now=hit(event.getX(i),event.getY(i));
                input.pointer(pointer,now!=null&&now.mask>0 ? now.mask : 0);
            }
        }
        return true;
    }
    private boolean editTouch(MotionEvent event,int action,int index,int id) {
        if(action==MotionEvent.ACTION_DOWN) {
            Control c=hit(event.getX(index),event.getY(index));
            if(c!=null&&c.mask==MENU) { performClick(); host.editDone(); return true; }
            dragged=c; dragPointer=id;
            if(c!=null) { dragDx=c.x-event.getX(index); dragDy=c.y-event.getY(index); }
        } else if(action==MotionEvent.ACTION_MOVE&&dragged!=null) {
            int i=event.findPointerIndex(dragPointer);
            if(i>=0) {
                dragged.x=clamp(event.getX(i)+dragDx,usable.left+dragged.radius,usable.right-dragged.radius);
                dragged.y=clamp(event.getY(i)+dragDy,usable.top+dragged.radius,usable.bottom-dragged.radius);
                invalidate();
            }
        } else if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP)&&id==dragPointer) {
            if(dragged!=null) settings.prefs.edit()
                    .putFloat(dragged.id+".x",(dragged.x-usable.left)/usable.width())
                    .putFloat(dragged.id+".y",(dragged.y-usable.top)/usable.height()).commit();
            dragged=null; dragPointer=-1;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(editing) {
            canvas.drawColor(0x85070B10);
            paint.setColor(Color.WHITE); paint.setTextSize(14*density()); paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Arrastra los botones · toca LISTO para jugar",getWidth()*.5f,getHeight()*.25f,paint);
        }
        for(Control c:controls) {
            if(hidden&&!editing&&c.mask!=MENU) continue;
            if(c.mask==PAD) { drawPad(canvas,c); continue; }
            boolean pressed=c.mask>0&&(input.held()&c.mask)!=0;
            int alpha=editing?210:Math.round(settings.opacity()*2.55f);
            paint.setStyle(Paint.Style.FILL); paint.setColor(pressed?0xFFFFAC42:0xFF0C1825); paint.setAlpha(pressed?210:alpha);
            canvas.drawCircle(c.x,c.y,c.radius,paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.4f*density());
            paint.setColor(pressed?0xFFFFC976:0xFFD9E5EF); paint.setAlpha(Math.min(255,alpha+45));
            canvas.drawCircle(c.x,c.y,c.radius,paint);
            paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(pressed?0xFF0C1019:Color.WHITE); paint.setAlpha(Math.min(255,alpha+65));
            paint.setTextSize((c.mask==MENU&&editing?10:c.title.length()>1?11:19)*density());
            float baseline=c.subtitle.isEmpty()?c.y-paint.getFontMetrics().ascent*.34f:c.y;
            canvas.drawText(c.mask==MENU&&editing?"LISTO":c.title,c.x,baseline,paint);
            if(!c.subtitle.isEmpty()) {
                paint.setTextSize(7.5f*density()); canvas.drawText(c.subtitle,c.x,c.y+13*density(),paint);
            }
        }
    }
    private void drawPad(Canvas canvas,Control c) {
        float r=c.radius, arm=r*.35f;
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF0C1825);
        paint.setAlpha(editing?210:Math.round(settings.opacity()*2.55f));
        canvas.drawRoundRect(c.x-arm,c.y-r,c.x+arm,c.y+r,arm*.3f,arm*.3f,paint);
        canvas.drawRoundRect(c.x-r,c.y-arm,c.x+r,c.y+arm,arm*.3f,arm*.3f,paint);
        int[] bits={TouchInput.UP,TouchInput.RIGHT,TouchInput.DOWN,TouchInput.LEFT};
        for(int i=0;i<4;i++) {
            canvas.save(); canvas.rotate(i*90,c.x,c.y);
            paint.setColor((input.held()&bits[i])!=0?PhoneUi.ACCENT:Color.WHITE);
            paint.setAlpha(editing?230:Math.min(255,Math.round(settings.opacity()*2.55f)+55));
            path.reset(); path.moveTo(c.x,c.y-r*.82f); path.lineTo(c.x-r*.20f,c.y-r*.50f);
            path.lineTo(c.x+r*.20f,c.y-r*.50f); path.close(); canvas.drawPath(path,paint);
            canvas.restore();
        }
        paint.setColor(0xFF647B91); paint.setAlpha(180); canvas.drawCircle(c.x,c.y,r*.14f,paint);
    }
}
