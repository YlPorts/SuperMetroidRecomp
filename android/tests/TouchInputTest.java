package com.ylports.supermetroid;
import java.util.ArrayList;
import java.util.List;

public final class TouchInputTest {
    private static void check(boolean condition,String reason) { if(!condition) throw new AssertionError(reason); }
    public static void main(String[] args) {
        List<Integer> downs=new ArrayList<>(),ups=new ArrayList<>();
        TouchInput pad=new TouchInput((down,up)->{downs.add(down);ups.add(up);});
        pad.pointer(7,TouchInput.RIGHT); pad.pointer(21,TouchInput.Y); pad.pointer(4,TouchInput.B);
        check(pad.held()==(TouchInput.RIGHT|TouchInput.Y|TouchInput.B),"move, shoot and jump together");
        pad.release(21); check(pad.held()==(TouchInput.RIGHT|TouchInput.B),"release only the shooting finger");
        pad.pointer(21,TouchInput.B); int before=ups.size(); pad.release(4);
        check(pad.held()==(TouchInput.RIGHT|TouchInput.B)&&ups.size()==before,"two fingers sharing a button");
        pad.pointer(7,TouchInput.LEFT|TouchInput.UP);
        check(downs.get(downs.size()-1)==(TouchInput.LEFT|TouchInput.UP),"slide into a diagonal");
        check(ups.get(ups.size()-1)==TouchInput.RIGHT,"old direction released when sliding");
        pad.clear(); check(pad.held()==0,"cancel releases every held key");
        int events=downs.size(); pad.release(999); pad.clear(); check(downs.size()==events,"duplicate cancel is inert");
        check(TouchInput.direction(0,0,100)==0,"center dead zone");
        check(TouchInput.direction(10,5,100)==0,"small drift dead zone");
        int[] expected={TouchInput.RIGHT,TouchInput.RIGHT|TouchInput.DOWN,TouchInput.DOWN,TouchInput.DOWN|TouchInput.LEFT,
                TouchInput.LEFT,TouchInput.LEFT|TouchInput.UP,TouchInput.UP,TouchInput.UP|TouchInput.RIGHT};
        for(int i=0;i<8;i++) {
            double angle=i*Math.PI/4;
            check(TouchInput.direction((float)(Math.cos(angle)*90),(float)(Math.sin(angle)*90),100)==expected[i],"direction "+i);
        }
        check(TouchInput.direction(2000,0,100)==TouchInput.RIGHT,"captured thumb can leave the d-pad");
        System.out.println("Touch input: simultaneous actions, shared keys, slide, cancel and eight directions passed.");
    }
}
