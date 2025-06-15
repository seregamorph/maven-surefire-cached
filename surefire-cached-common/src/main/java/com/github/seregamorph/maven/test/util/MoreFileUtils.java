package com.github.seregamorph.maven.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;

/**
 * @author Sergey Chernov
 */
public final class MoreFileUtils {

    public static byte[] read(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(File file, byte[] bytes) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void delete(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            try {
                FileUtils.deleteDirectory(fileOrDirectory);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            fileOrDirectory.delete();
        }
    }

    private MoreFileUtils() {
    }
}
