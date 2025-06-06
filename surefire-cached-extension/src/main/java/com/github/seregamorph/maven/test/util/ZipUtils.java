package com.github.seregamorph.maven.test.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * @author Sergey Chernov
 */
public final class ZipUtils {

    public record PackedFile(String fileName, long unpackedSize) {
    }

    /**
     * Pack directory filtered included files to TAR.GZ
     *
     * @param directory
     * @param includes
     * @param packFile  target tar.gz file
     * @return packed files info
     */
    public static List<PackedFile> packDirectory(File directory, List<String> includes, File packFile) {
        var packedFiles = new ArrayList<PackedFile>();
        try (OutputStream fos = new FileOutputStream(packFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(bos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)
        ) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            var matchingFileNames = findMatchingFileNames(directory, includes);

            for (var fileName : matchingFileNames) {
                var file = new File(directory, fileName);
                if (file.isFile()) {
                    TarArchiveEntry entry = new TarArchiveEntry(file, fileName);
                    taos.putArchiveEntry(entry);

                    try (InputStream fis = new FileInputStream(file)) {
                        IOUtils.copy(fis, taos);
                    }
                    taos.closeArchiveEntry();
                    packedFiles.add(new PackedFile(fileName, file.length()));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return packedFiles;
    }

    public static void unpackDirectory(File packFile, File targetDirectory) {
        try (InputStream fis = new FileInputStream(packFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)
        ) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                // Prevent Zip Slip vulnerability
                var outputDir = targetDirectory.toPath();
                Path outputPath = outputDir.resolve(entry.getName()).normalize();
                if (!outputPath.startsWith(outputDir)) {
                    throw new IOException("Entry is outside of the target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(outputPath.getParent());
                    try (OutputStream fos = Files.newOutputStream(outputPath);
                         BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                        IOUtils.copy(tais, bos);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Finds all files in the directory that match any of the given patterns.
     *
     * @param directory the directory to search in
     * @param includes  the Ant-style patterns to match against
     * @return a list of matching file names
     */
    private static List<String> findMatchingFileNames(File directory, List<String> includes) throws IOException {
        AntPathMatcher matcher = new AntPathMatcher();
        List<String> matchingFileNames = new ArrayList<>();

        // Get the base path for creating relative paths
        Path basePath = directory.toPath();

        // Walk the directory tree and collect matching files
        Files.walk(basePath)
            .filter(Files::isRegularFile)
            .forEach(path -> {
                // Create a relative path for matching
                String relativePath = basePath.relativize(path).toString();

                // Check if the file matches any of the patterns
                for (String pattern : includes) {
                    if (matcher.match(pattern, relativePath)) {
                        matchingFileNames.add(relativePath);
                        break;
                    }
                }
            });

        return matchingFileNames;
    }

    private ZipUtils() {
    }
}
