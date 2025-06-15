package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory cache LRU storage.
 *
 * @author Sergey Chernov
 */
public class MemoryStorage implements CacheStorage {

    private static final Logger logger = LoggerFactory.getLogger(MemoryStorage.class);

    private final Map<CacheEntryKey, Map<String, byte[]>> cache = new LinkedHashMap<>();

    private final int size;

    public MemoryStorage(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        this.size = size;
    }

    @Override
    @Nullable
    public byte[] read(CacheEntryKey cacheEntryKey, String fileName) {
        synchronized (cache) {
            Map<String, byte[]> cacheEntry = cache.remove(cacheEntryKey);
            if (cacheEntry == null) {
                return null;
            }
            // LRU
            cache.put(cacheEntryKey, cacheEntry);
            return cacheEntry.get(fileName);
        }
    }

    @Override
    public int write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) {
        int deleted = 0;
        synchronized (cache) {
            Map<String, byte[]> cacheEntry = cache.remove(cacheEntryKey);
            if (cacheEntry == null) {
                if (cache.size() >= size) {
                    // LRU
                    Iterator<Map.Entry<CacheEntryKey, Map<String, byte[]>>> iterator = cache.entrySet().iterator();
                    Map.Entry<CacheEntryKey, Map<String, byte[]>> deletedEntry = iterator.next();
                    logger.info("Removing eldest cache entry {} from cache.", deletedEntry.getKey());
                    Map<String, byte[]> deletedValue = deletedEntry.getValue();
                    synchronized (deletedValue) {
                        deleted = deletedValue.size();
                    }
                    iterator.remove();
                }
                cacheEntry = new LinkedHashMap<>();
            }
            cache.put(cacheEntryKey, cacheEntry);
            cacheEntry.put(fileName, value);
        }
        return deleted;
    }
}
