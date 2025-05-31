package com.github.seregamorph.maven.test.util;

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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jetbrains.annotations.Contract;

/**
 * @author Sergey Chernov
 */
public final class HashUtils {

    /**
     * Hash of an empty directory or jar file.
     */
    public static final String HASH_EMPTY_FILE_COLLECTION = "00000000000000000000000000000000";

    @Contract(pure = true)
    public static String hashArray(byte[] array) {
        return formatDigest(getMessageDigest().digest(array));
    }

    /**
     * Calculate hash sums of entries from the ZIP archive
     *
     * @param file zip file
     * @return zip entry hash sums
     */
    @Contract(pure = true)
    public static SortedMap<String, String> hashZipFile(File file, List<String> excludePathPatterns) {
        var map = new TreeMap<String, String>();
        try (var zipStream = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    var entryName = zipEntry.getName();
                    if (include(excludePathPatterns, entryName)) {
                        map.put(entryName, hashStream(zipStream));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to unzip " + file, e);
        }
        return map;
    }

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

    /**
     * Calculate hash sums of files from the directory
     *
     * @param dir
     * @param excludePathPatterns
     * @return directory entry (relative path) hash sums
     */
    @Contract(pure = true)
    public static SortedMap<String, String> hashDirectory(File dir, List<String> excludePathPatterns) {
        var map = new TreeMap<String, String>();
        hashDirectory(map, dir.toPath(), dir.toPath(), excludePathPatterns);
        return map;
    }

    private static void hashDirectory(
        Map<String, String> map,
        Path baseDir,
        Path dir,
        List<String> excludePathPatterns
    ) {
        try (var directoryStream = Files.newDirectoryStream(dir)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    hashDirectory(map, baseDir, path, excludePathPatterns);
                } else {
                    var relativePath = baseDir.relativize(path).toString();
                    if (include(excludePathPatterns, relativePath)) {
                        map.put(relativePath, hashFile(path.toFile()));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean include(List<String> excludePathPatterns, String relativePath) {
        if (excludePathPatterns.isEmpty()) {
            return true;
        }
        var antPathMatcher = new AntPathMatcher();
        for (var excludePathPattern : excludePathPatterns) {
            if (antPathMatcher.match(excludePathPattern, relativePath)) {
                return false;
            }
        }
        return true;
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
