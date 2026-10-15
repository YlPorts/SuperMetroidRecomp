package com.ylports.supermetroid;

import android.app.Instrumentation;
import android.os.Bundle;
import android.view.MotionEvent;
import java.lang.reflect.Field;
import java.util.List;

/** Runs actual Android MotionEvents through the production View, no mock SDK. */
public final class ControlsSmokeTest extends Instrumentation {
    private Throwable failure;
    private int held, menus, edits;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        runOnMainSync(()->{
            try { exercise(); } catch(Throwable t) { failure=t; }
        });
        if(failure!=null) {
            result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(failure));
            finish(0,result);
        } else {
            result.putString("stream","PASS: real Android View, 4 fingers, IDs/index reorder, shared buttons, slide, cancel, editor persistence and resize\n");
            result.putString("controls_result","passed"); finish(-1,result);
        }
    }
    private void check(boolean b,String message) { if(!b) throw new AssertionError(message); }
    private void exercise() throws Exception {
        // The test APK's settings, isolated from the player's preferences.
        AppSettings settings=new AppSettings(getContext()); settings.prefs.edit().clear().commit();
        TouchControlsView.Host host=new TouchControlsView.Host() {
            public void keys(int down,int up) { held=(held|down)&~up; }
            public void menu() { menus++; }
            public void editDone() { edits++; }
            public void editControl(String id) { edits++; }
            public void safeArea(int left,int top,int right,int bottom) {}
        };
        TouchControlsView view=new TouchControlsView(getTargetContext(),settings,host);
        view.layout(0,0,1900,900);
        float[] pad=center(view,"pad"),run=center(view,"a"),fire=center(view,"y"),jump=center(view,"b");
        pad[0]+=radius(view,"pad")*.65f;
        int[] ids={17,5,29,2}; float[][] points={pad,run,fire,jump};
        for(int i=0;i<4;i++) send(view,i==0?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_POINTER_DOWN|(i<<8),ids,points,i+1);
        check(held==(TouchInput.RIGHT|TouchInput.A|TouchInput.Y|TouchInput.B),"run, jump, fire and move simultaneously");
        send(view,MotionEvent.ACTION_POINTER_UP|(1<<8),ids,points,4);
        check(held==(TouchInput.RIGHT|TouchInput.Y|TouchInput.B),"only ID 5 releases");
        int[] reordered={2,17,29}; float[][] p2={jump,pad,fire};
        send(view,MotionEvent.ACTION_MOVE,reordered,p2,3);
        check(held==(TouchInput.RIGHT|TouchInput.Y|TouchInput.B),"pointer index reorder preserves IDs");
        p2[2]=jump; send(view,MotionEvent.ACTION_MOVE,reordered,p2,3);
        check(held==(TouchInput.RIGHT|TouchInput.B),"slide between face buttons");
        send(view,MotionEvent.ACTION_POINTER_UP,reordered,p2,3);
        check(held==(TouchInput.RIGHT|TouchInput.B),"second finger still holds jump");
        send(view,MotionEvent.ACTION_CANCEL,reordered,p2,3); check(held==0,"cancel releases every input");
        send(view,MotionEvent.ACTION_DOWN,new int[]{33},new float[][]{jump},1);
        view.releaseAll(); check(held==0,"focus/lifecycle release clears keys");
        view.edit(true); float[] old=center(view,"y"); float[] moved={old[0]-90,old[1]-60};
        send(view,MotionEvent.ACTION_DOWN,new int[]{8},new float[][]{old},1);
        send(view,MotionEvent.ACTION_MOVE,new int[]{8},new float[][]{moved},1);
        send(view,MotionEvent.ACTION_UP,new int[]{8},new float[][]{moved},1);
        check(settings.prefs.contains("y.x"),"drag automatically persisted");
        view.reload(); float[] after=center(view,"y"); check(Math.abs(after[0]-moved[0])<1,"position survives reload");
        float before=radius(view,"y");
        settings.prefs.edit().putFloat("y.scale",.7f).putFloat("y.hit",1.4f).commit(); view.reload();
        check(radius(view,"y")<before*.71f,"individual size applied");
        view.edit(false); view.layout(0,0,1280,600); check(held==0,"resize releases all");
        check(menus==0,"face-button slides never open menu");
        settings.prefs.edit().clear().commit();
    }
    private Object control(TouchControlsView v,String id) throws Exception {
        Field f=TouchControlsView.class.getDeclaredField("controls"); f.setAccessible(true);
        for(Object c:(List<?>)f.get(v)) {
            Field name=c.getClass().getDeclaredField("id"); name.setAccessible(true);
            if(id.equals(name.get(c))) return c;
        }
        throw new AssertionError("missing control "+id);
    }
    private float field(Object c,String name) throws Exception { Field f=c.getClass().getDeclaredField(name); f.setAccessible(true); return f.getFloat(c); }
    private float[] center(TouchControlsView v,String id) throws Exception { Object c=control(v,id); return new float[]{field(c,"x"),field(c,"y")}; }
    private float radius(TouchControlsView v,String id) throws Exception { return field(control(v,id),"radius"); }
    private void send(TouchControlsView v,int action,int[] ids,float[][] points,int count) {
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[count];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
        for(int i=0;i<count;i++) {
            properties[i]=new MotionEvent.PointerProperties(); properties[i].id=ids[i]; properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords(); coords[i].x=points[i][0]; coords[i].y=points[i][1]; coords[i].pressure=1; coords[i].size=.1f;
        }
        long t=android.os.SystemClock.uptimeMillis();
        MotionEvent e=MotionEvent.obtain(t,t,action,count,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
        try { v.onTouchEvent(e); } finally { e.recycle(); }
    }
}
