package com.github.seregamorph.maven.test.storage;

import com.github.seregamorph.maven.test.common.CacheEntryKey;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory cache LRU storage with SoftReference.
 *
 * @author Sergey Chernov
 */
public class SoftReferenceMemoryStorage implements CacheStorage {

    private static final Logger logger = LoggerFactory.getLogger(SoftReferenceMemoryStorage.class);

    private final Map<CacheEntryKey, SoftReference<Map<String, byte[]>>> cache = new LinkedHashMap<>();

    private final int size;

    public SoftReferenceMemoryStorage(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        this.size = size;
    }

    @Override
    @Nullable
    public byte[] read(CacheEntryKey cacheEntryKey, String fileName) {
        synchronized (cache) {
            SoftReference<Map<String, byte[]>> softReferenceCacheEntry = cache.remove(cacheEntryKey);
            if (softReferenceCacheEntry == null) {
                return null;
            }
            Map<String, byte[]> cacheEntry = softReferenceCacheEntry.get();
            if (cacheEntry == null) {
                logger.info("Null reference for cache key {}, current size {}", cacheEntryKey, cache.size());
                return null;
            }
            // LRU
            cache.put(cacheEntryKey, softReferenceCacheEntry);
            return cacheEntry.get(fileName);
        }
    }

    @Override
    public int write(CacheEntryKey cacheEntryKey, String fileName, byte[] value) {
        int deleted = 0;
        synchronized (cache) {
            SoftReference<Map<String, byte[]>> softReferenceCacheEntry = cache.remove(cacheEntryKey);
            Map<String, byte[]> cacheEntry = softReferenceCacheEntry == null ? null : softReferenceCacheEntry.get();
            if (cacheEntry == null) {
                if (cache.size() >= size) {
                    // LRU
                    Iterator<Map.Entry<CacheEntryKey, SoftReference<Map<String, byte[]>>>> iterator =
                        cache.entrySet().iterator();
                    Map.Entry<CacheEntryKey, SoftReference<Map<String, byte[]>>> deletedEntry = iterator.next();
                    logger.info("Removing eldest cache entry {} from cache.", deletedEntry.getKey());
                    SoftReference<Map<String, byte[]>> deletedReference = deletedEntry.getValue();
                    Map<String, byte[]> deletedValue = deletedReference.get();
                    deleted = deletedValue == null ? 0 : deletedValue.size();
                    iterator.remove();
                }
                cacheEntry = new LinkedHashMap<>();
            }
            cache.put(cacheEntryKey, new SoftReference<>(cacheEntry));
            cacheEntry.put(fileName, value);
        }
        return deleted;
    }
}
