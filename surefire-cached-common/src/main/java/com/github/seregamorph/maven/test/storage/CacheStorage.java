package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import javax.annotation.Nullable;

/**
 * Cache storage reads and writes cache entities locally or remotely. It can also be delegate filtering implementation
 * e.g. to pack/unpack.
 *
 * @author Sergey Chernov
 */
public interface CacheStorage {

    /**
     * Read cache entity
     *
     * @param cacheEntryKey
     * @param fileName
     * @return existing cache entity content or null if it does not exist
     * @throws CacheStorageException in case of I/O operations
     */
    @Nullable
    ReadResult read(CacheEntryKey cacheEntryKey, String fileName) throws CacheStorageException;

    /**
     * Write cache entry
     *
     * @param cacheEntryKey
     * @param fileName
     * @param value
     * @return number of deleted files
     * @throws CacheStorageException in case of I/O operations
     */
    WriteResult write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) throws CacheStorageException;
}
