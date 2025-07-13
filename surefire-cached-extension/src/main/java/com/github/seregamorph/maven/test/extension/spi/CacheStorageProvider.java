package com.github.seregamorph.maven.test.extension.spi;

import com.github.seregamorph.maven.test.storage.CacheStorage;
import com.github.seregamorph.maven.test.util.PropertySource;

/**
 * @author Sergey Chernov
 */
public interface CacheStorageProvider {

    boolean supportsCacheStorageUrl(String cacheStorageUrl);

    CacheStorage createCacheStorage(String cacheStorageUrl, PropertySource propertySource);
}
