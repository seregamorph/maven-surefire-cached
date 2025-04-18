package com.github.seregamorph.maven.test.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.lingala.zip4j.ZipFile;

/**
 * @author Sergey Chernov
 */
public final class ZipUtils {

    public static void zipDirectory(File sourceDirectory, File targetZipFile) {
        try (var zip = new ZipFile(targetZipFile)) {
            zip.addFolder(sourceDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void unzipDirectory(File sourceZipFile, File targetDirectory) {
        try (var zip = new ZipFile(sourceZipFile)) {
            zip.extractAll(targetDirectory.getAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ZipUtils() {
    }
}
