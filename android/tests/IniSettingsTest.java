package com.ylports.supermetroid;
public final class IniSettingsTest {
    public static void main(String[] args) {
        String ini="# retained\n[Sound]\nVolume = 15\nAudioSamples=512\n[Other]\nVolume=77\nToken=kept\n";
        String out=IniSettings.set(ini,"Sound","Volume","80");
        if(!out.contains("Volume=80")||!out.contains("Volume=77")||!out.contains("Token=kept")||!out.contains("# retained")) throw new AssertionError(out);
        if(!IniSettings.set(out,"Graphics","LinearFiltering","0").contains("[Graphics]\nLinearFiltering=0")) throw new AssertionError("missing section");
        String duplicate="[Sound]\nVolume=4\nVolume=9\n";
        if(IniSettings.set(duplicate,"Sound","Volume","10").split("Volume=",-1).length!=2) throw new AssertionError("duplicate key");
        if(!IniSettings.set("[Sound]\nFoo=x\n[Next]\nY=1\n","Sound","Volume","22").contains("Volume=22\n[Next]")) throw new AssertionError("section boundary");
        System.out.println("INI migration: section boundaries, unknown options, duplicates and new sections passed.");
    }
}
