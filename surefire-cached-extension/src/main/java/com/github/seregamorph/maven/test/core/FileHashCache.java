package com.github.seregamorph.maven.test.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.seregamorph.maven.test.util.HashUtils;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Chernov
 */
public class FileHashCache {

    private static final Logger log = LoggerFactory.getLogger(FileHashCache.class);

    /**
     * "$canonicalAbsoluteFileName:$sensitivity" -> file hash
     */
    private final Cache<CacheKey, FileHashValue> cacheFiles;
    /**
     * "$canonicalAbsoluteDirName" -> directory hash
     */
    private final Cache<CacheKey, DirHashValue> cacheDirectories;

    public FileHashCache() {
        cacheFiles = Caffeine.newBuilder().build();
        cacheDirectories = Caffeine.newBuilder().build();
    }

    /**
     * Get hash sum of classpath element which may be a JAR file or a directory with classes. The META-INF/MANIFEST.MF
     * is ignored if present.
     * <p>
     * Note: this method will calculate the same hash sum for class directory and jar archive of the same directory.
     *
     * @param file                class directory or JAR file
     * @param excludePathPatterns ant expressions of classpath resources that should be skipped in hash calculation
     * @return aggregated hash sum
     */
    public String getClasspathElementHash(File file, List<String> excludePathPatterns) {
        try {
            CacheKey cacheKey = new CacheKey(file.getCanonicalFile().getAbsolutePath(),
                new ArrayList<>(excludePathPatterns));
            if (file.isDirectory()) {
                Function<CacheKey, DirHashValue> loader = $ -> {
                    String hash = plainHash(HashUtils.hashDirectory(file, excludePathPatterns));
                    return new DirHashValue(hash, file.lastModified());
                };
                DirHashValue fileHashValue = cacheDirectories.get(cacheKey, loader);
                assert fileHashValue != null;
                if (fileHashValue.fileLastModified() != file.lastModified()) {
                    log.warn("Invalidating cache of: {}", cacheKey);
                    cacheDirectories.invalidate(cacheKey);
                    fileHashValue = cacheDirectories.get(cacheKey, loader);
                }
                return fileHashValue.hash();
            } else if (file.exists()) {
                Function<CacheKey, FileHashValue> loader = $ -> {
                    // calculate has hums of zip entries ignoring timestamps
                    String hash = plainHash(HashUtils.hashZipFile(file, excludePathPatterns));
                    return new FileHashValue(hash, file.length(), file.lastModified());
                };
                FileHashValue fileHashValue = cacheFiles.get(cacheKey, loader);
                if (file.length() != fileHashValue.fileLength()
                    || fileHashValue.fileLastModified() != file.lastModified()) {
                    // this should not happen in a regular mvnw
                    log.warn("Invalidating cache of file: {}", cacheKey);
                    cacheFiles.invalidate(cacheKey);
                    fileHashValue = cacheFiles.get(cacheKey, loader);
                }
                return fileHashValue.hash();
            } else {
                // this can be a non-existing classes directory for modules with empty sourceSet
                return HashUtils.HASH_EMPTY_FILE_COLLECTION;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String plainHash(SortedMap<String, String> mapHash) {
        if (mapHash.isEmpty()) {
            return HashUtils.HASH_EMPTY_FILE_COLLECTION;
        }

        StringBuilder sw = new StringBuilder();
        mapHash.forEach((key, value) -> sw.append(key).append(":").append(value).append("\n"));
        return HashUtils.hashArray(sw.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static final class CacheKey {

        private final String absoluteFileName;
        private final List<String> excludeClasspathResources;

        private CacheKey(String absoluteFileName, List<String> excludeClasspathResources) {
            this.absoluteFileName = absoluteFileName;
            this.excludeClasspathResources = excludeClasspathResources;
        }

        String absoluteFileName() {
            return absoluteFileName;
        }

        List<String> excludeClasspathResources() {
            return excludeClasspathResources;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            CacheKey that = (CacheKey) obj;
            return Objects.equals(this.absoluteFileName, that.absoluteFileName)
                && Objects.equals(this.excludeClasspathResources, that.excludeClasspathResources);
        }

        @Override
        public int hashCode() {
            return Objects.hash(absoluteFileName, excludeClasspathResources);
        }

        @Override
        public String toString() {
            return "CacheKey[" +
                "absoluteFileName=" + absoluteFileName + ", " +
                "excludeClasspathResources=" + excludeClasspathResources + ']';
        }
    }

    private static final class DirHashValue {

        private final String hash;
        private final long fileLastModified;

        private DirHashValue(String hash, long fileLastModified) {
            this.hash = hash;
            this.fileLastModified = fileLastModified;
        }

        String hash() {
            return hash;
        }

        long fileLastModified() {
            return fileLastModified;
        }
    }

    private static final class FileHashValue {

        private final String hash;
        private final long fileLength;
        private final long fileLastModified;

        private FileHashValue(String hash, long fileLength, long fileLastModified) {
            this.hash = hash;
            this.fileLength = fileLength;
            this.fileLastModified = fileLastModified;
        }

        String hash() {
            return hash;
        }

        long fileLength() {
            return fileLength;
        }

        long fileLastModified() {
            return fileLastModified;
        }
    }
}
