package com.github.seregamorph.maven.test.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.Contract;

/**
 * @author Sergey Chernov
 */
final class HashUtils {

    @Contract(pure = true)
    static String hashArray(byte[] array) {
        return formatDigest(getMessageDigest().digest(array));
    }

    @Contract(pure = true)
    static SortedMap<String, String> hashZipFile(File file) {
        var map = new TreeMap<String, String>();
        try (var zipStream = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    map.put(zipEntry.getName(), hashStream(zipStream));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return map;
    }

    @Contract(pure = true)
    private static String hashStream(InputStream in) throws IOException {
        var digest = getMessageDigest();
        int len;
        byte[] buffer = new byte[8192];
        while ((len = in.read(buffer)) != -1) {
            digest.update(buffer, 0, len);
        }
        return formatDigest(digest.digest());
    }

    private static String formatDigest(byte[] digest) {
        var fullHash = String.format("%032X", new BigInteger(1, digest)).toLowerCase();
        return fullHash.substring(0, 32);
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Contract(pure = true)
    static SortedMap<String, String> hashDirectory(File dir) {
        var map = new TreeMap<String, String>();
        hashDirectory(map, dir.toPath(), dir.toPath());
        return map;
    }

    private static void hashDirectory(Map<String, String> map, Path baseDir, Path dir) {
        try (var directoryStream = Files.newDirectoryStream(dir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    hashDirectory(map, baseDir, path);
                } else {
                    var relativePath = baseDir.relativize(path);
                    map.put(relativePath.toString(), hashFile(path.toFile()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Contract(pure = true)
    private static String hashFile(File file) {
        try (var in = new FileInputStream(file)) {
            return hashStream(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private HashUtils() {
    }
}
