package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.util.MoreFileUtils;
import java.io.File;
import java.util.Comparator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileCacheStorage implements CacheStorage {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheStorage.class);

    /**
     * Max number of cache entries per single "$groupId/artifactId" layout
     */
    private static final int MAX_CACHE_ENTRIES = 4;

    private final File baseDir;
    private final int maxCacheEntries;

    public FileCacheStorage(File baseDir) {
        this(baseDir, MAX_CACHE_ENTRIES);
    }

    public FileCacheStorage(File baseDir, int maxCacheEntries) {
        if (maxCacheEntries < 1) {
            throw new IllegalArgumentException("maxCacheEntries must be >= 1");
        }
        this.baseDir = baseDir;
        this.maxCacheEntries = maxCacheEntries;
    }

    private File getEntryFile(CacheEntryKey cacheEntryKey, String fileName) {
        return new File(baseDir, cacheEntryKey + "/" + fileName);
    }

    @Override
    public byte[] read(CacheEntryKey cacheEntryKey, String fileName) {
        var file = getEntryFile(cacheEntryKey, fileName);
        if (!file.exists()) {
            return null;
        }
        return MoreFileUtils.read(file);
    }

    @Override
    public int write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) {
        var file = getEntryFile(cacheEntryKey, fileName);
        var deleted = createParentAndCleanupOld(file.getParentFile());
        MoreFileUtils.write(file, value);
        return deleted;
    }

    private int createParentAndCleanupOld(File directory) {
        // "$baseId/$groupId/$artifactId/$hash"
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IllegalStateException("Not a directory: " + directory);
            }
            return 0;
        }

        int deleted = 0;
        // "$baseId/$groupId/$artifactId"
        var layoutDirectory = directory.getParentFile();
        if (layoutDirectory.exists()) {
            if (!layoutDirectory.getPath().startsWith(baseDir.getPath())) {
                // sanity check before deleting directories
                throw new IllegalStateException("Not a directory under baseDir " + layoutDirectory + " " + baseDir);
            }
            var dirs = layoutDirectory.listFiles();
            if (dirs == null) {
                throw new IllegalStateException("Not a directory: " + layoutDirectory);
            }
            var siblingDirs = Stream.of(dirs)
                .sorted(Comparator.comparing(File::lastModified))
                .toList();

            // we delete all old entries, keep only last MAX_CACHE_ENTRIES-1
            for (int idx = 0; idx <= siblingDirs.size() - maxCacheEntries; idx++) {
                var siblingDir = siblingDirs.get(idx);
                LOGGER.debug("Deleting old cache entry {}", siblingDir);
                MoreFileUtils.delete(siblingDir);
                deleted++;
            }
        }

        directory.mkdirs();
        return deleted;
    }
}
