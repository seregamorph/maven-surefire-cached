package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Chernov
 */
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    private final CacheStorage cacheStorage;
    private final CacheServiceMetrics metrics;
    private final int failureThreshold;

    public CacheService(CacheStorage cacheStorage, CacheServiceMetrics metrics, int failureThreshold) {
        this.cacheStorage = cacheStorage;
        this.metrics = metrics;
        this.failureThreshold = failureThreshold;
    }

    @Nullable
    public byte[] read(CacheEntryKey cacheEntryKey, String fileName) {
        if (metrics.getReadFailures() >= failureThreshold) {
            metrics.addReadSkipped();
            logger.info("Skipping reading {} {} because of too many failures", cacheEntryKey, fileName);
            return null;
        }

        long start = System.nanoTime();
        Integer length = null;
        try {
            ReadResult entity = cacheStorage.read(cacheEntryKey, fileName);
            if (entity != null) {
                length = entity.length();
            }
            return entity == null ? null : entity.bytes();
        } catch (CacheStorageException e) {
            logger.warn("Failed to read cache entry {}", e.toString());
            metrics.addReadFailure();
            return null;
        } finally {
            long nanos = System.nanoTime() - start;
            if (length == null) {
                metrics.addReadMissOperation(nanos);
            } else {
                metrics.addReadHitOperation(nanos, length);
            }
        }
    }

    public int write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) {
        if (metrics.getWriteFailures() >= failureThreshold) {
            metrics.addWriteSkipped();
            logger.info("Skipping writing {} {} because of too many failures", cacheEntryKey, fileName);
            return 0;
        }

        long start = System.nanoTime();
        try {
            WriteResult writeResult = cacheStorage.write(cacheEntryKey, fileName, value);
            return writeResult.deletedFilesCount();
        } catch (CacheStorageException e) {
            logger.warn("Failed to write cache entry {}", e.toString());
            metrics.addWriteFailure();
            return 0;
        } finally {
            metrics.addWriteOperation(System.nanoTime() - start, value.length);
        }
    }
}
