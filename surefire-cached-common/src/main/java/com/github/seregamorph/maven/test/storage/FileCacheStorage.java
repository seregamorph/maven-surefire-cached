package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import com.github.seregamorph.maven.test.util.MoreFileUtils;
import com.github.seregamorph.maven.test.util.ValidatorUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache storage in a local directory layout. Has basic protection for file traverse vulnerabilities.
 *
 * @author Sergey Chernov
 */
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
        ValidatorUtils.validateFileName(fileName);
        String child = cacheEntryKey + "/" + fileName;
        if (child.contains("..")) {
            // this may be a file traversal attack
            throw new IllegalArgumentException("Illegal file name '" + child + "'");
        }
        return new File(baseDir, child);
    }

    @Override
    public byte[] read(CacheEntryKey cacheEntryKey, String fileName) {
        File file = getEntryFile(cacheEntryKey, fileName);
        if (!file.exists()) {
            return null;
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new CacheStorageException("Error reading " + fileName, e);
        }
    }

    @Override
    public int write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) {
        File file = getEntryFile(cacheEntryKey, fileName);
        int deleted = createParentAndCleanupOld(file.getParentFile());
        try {
            Files.write(file.toPath(), value);
        } catch (IOException e) {
            throw new CacheStorageException("Error writing " + fileName, e);
        }
        return deleted;
    }

    /**
     *
     * @param directory
     * @return number of deleted files
     */
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
        File layoutDirectory = directory.getParentFile();
        if (layoutDirectory.exists()) {
            if (!layoutDirectory.getPath().startsWith(baseDir.getPath())) {
                // sanity check before deleting directories
                throw new IllegalStateException("Not a directory under baseDir " + layoutDirectory + " " + baseDir);
            }
            File[] dirs = layoutDirectory.listFiles();
            if (dirs == null) {
                throw new IllegalStateException("Not a directory: " + layoutDirectory);
            }
            List<File> siblingDirs = Stream.of(dirs)
                .sorted(Comparator.comparing(File::lastModified))
                .collect(Collectors.toList());

            // we delete all old entries, keep only last MAX_CACHE_ENTRIES-1
            for (int idx = 0; idx <= siblingDirs.size() - maxCacheEntries; idx++) {
                File siblingDir = siblingDirs.get(idx);
                LOGGER.debug("Deleting old cache entry {}", siblingDir);
                MoreFileUtils.delete(siblingDir);
                deleted++;
            }
        }

        directory.mkdirs();
        return deleted;
    }
}
