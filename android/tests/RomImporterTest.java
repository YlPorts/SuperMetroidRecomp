package com.ylports.supermetroid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class RomImporterTest {
    private static void check(boolean condition,String reason) { if(!condition) throw new AssertionError(reason); }
    private static void rejected(InputStream source,Path destination) throws Exception {
        byte[] old=Files.readAllBytes(destination);
        try { RomImporter.importRom(source,destination.toFile()); throw new AssertionError("bad ROM accepted"); }
        catch(IOException expected) {}
        check(Arrays.equals(Files.readAllBytes(destination),old),"failed import preserves existing data");
        check(!Files.exists(Path.of(destination+".importing")),"no abandoned partial import");
    }
    public static void main(String[] args) throws Exception {
        Path folder=Files.createTempDirectory("sm-import-test"), destination=folder.resolve("supermetroid.sfc");
        Files.write(destination,new byte[]{7,2,3});
        rejected(new ByteArrayInputStream(new byte[23]),destination);
        rejected(new ByteArrayInputStream(new byte[RomImporter.ROM_SIZE]),destination);
        rejected(new ByteArrayInputStream(new byte[RomImporter.ROM_SIZE+512]),destination);
        final int[] read={0};
        rejected(new InputStream() { public int read() { read[0]++; return 1; }
            public int read(byte[] b,int o,int l) { Arrays.fill(b,o,o+l,(byte)1); read[0]+=l; return l; } },destination);
        check(read[0]==RomImporter.ROM_SIZE+513,"untrusted provider is bounded");
        check(RomImporter.digest("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8),0,3)
                .equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),"SHA-256 known answer");
        Files.delete(destination); Files.delete(folder);
        System.out.println("ROM import: size/hash rejection, bounded reads and preservation passed; valid-game import requires the user's ROM.");
    }
}
