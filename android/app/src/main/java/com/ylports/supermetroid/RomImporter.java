package com.ylports.supermetroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Accept the exact image used by the recompiler, with optional copier header. */
public final class RomImporter {
    public static final int ROM_SIZE = 3145728;
    public static final String SHA256 = "12b77c4bc9c1832cee8881244659065ee1d84c70c3d29e6eaf92e6798cc2ca72";
    private RomImporter() {}

    public static void importRom(InputStream source, File destination) throws IOException {
        // Bound reads before allocating; cloud document providers can omit size.
        byte[] data = new byte[ROM_SIZE + 513];
        int size = 0;
        try (InputStream in = source) {
            if (in == null) throw new IOException("No se pudo abrir el archivo.");
            while (size < data.length) {
                int count = in.read(data, size, data.length - size);
                if (count < 0) break;
                if (count == 0) {
                    int next = in.read();
                    if (next < 0) break;
                    data[size++] = (byte) next;
                } else size += count;
            }
        }
        int offset = size == ROM_SIZE + 512 ? 512 : 0;
        if (size != ROM_SIZE && size != ROM_SIZE + 512)
            throw new IOException("Selecciona Super Metroid (Japan, USA) en .sfc o .smc (3 MB). No un ZIP.");
        if (!digest(data, offset, ROM_SIZE).equals(SHA256))
            throw new IOException("Esta ROM no coincide con Super Metroid (Japan, USA). Usa la versión original sin parches.");
        File parent = destination.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs()))
            throw new IOException("No se pudo preparar la carpeta del juego.");
        File temp = new File(parent, destination.getName() + ".importing");
        try {
            try (FileOutputStream out = new FileOutputStream(temp)) {
                out.write(data, offset, ROM_SIZE);
                out.getFD().sync();
            }
            // On the same filesystem: failed imports never erase the old ROM.
            Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp.toPath());
        }
    }

    static String digest(byte[] bytes, int offset, int length) throws IOException {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(bytes, offset, length);
            StringBuilder result = new StringBuilder(64);
            for (byte b : sha.digest()) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("No se pudo verificar la ROM.", e);
        }
    }
}
